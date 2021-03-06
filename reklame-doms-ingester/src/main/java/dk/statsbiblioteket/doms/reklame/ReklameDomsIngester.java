package dk.statsbiblioteket.doms.reklame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import dk.statsbiblioteket.doms.central.CentralWebservice;
import dk.statsbiblioteket.doms.central.Relation;
import dk.statsbiblioteket.doms.common.DomsIngester;
import dk.statsbiblioteket.doms.common.FFProbeParser;
import dk.statsbiblioteket.doms.common.IngestContext;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XPathSelector;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Ingester for Doms. */
public class ReklameDomsIngester extends DomsIngester {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String REKLAMETEMPLATE_PROPERTY = "dk.statsbiblioteket.doms.common.reklametemplate";

    public ReklameDomsIngester(Properties config, CentralWebservice webservice) {
        super(config, webservice);
    }

    /**
     * Ingest data from context into DOMS via template given by config.
     *
     * @param context Command-line input parameters containing metadata to be
     *                ingested in DOMS.
     * @return The PID of the resulting DOMS file-object, now containing the
     *         metadata from context.
     */
    @Override
    public String ingest(IngestContext context) {
        ReklameIngestContext reklameContext;
        if (!(context instanceof ReklameIngestContext)) {
            throw new IllegalStateException("IngestContext context is not of type ReklameIngestContext");
        } else {
            reklameContext = (ReklameIngestContext) context;
        }
        // Template object to clone to get new file objects, get from properties file or command line
        String fileTemplate = reklameContext.getTemplatePid();
        if (fileTemplate == null) {
            fileTemplate = config.getProperty(TEMPLATE_PROPERTY, "doms:Template_ReklameFile"); // 2nd arg is default value
        }

        Map<String,String> allowedFormats = getAllowedFormatsProperty();
        //String validFormatUri = config.getProperty(FORMAT_URI_PROPERTY, "info:pronom/x-fmt/386");

        // Get FFProbe output from context
        String FFProbeOutput = reklameContext.getFfprobeContents();

        try {
            // Via DOMS Central, get PID of DOMS file-object which corresponds
            // to the file with the given URL (URL from context).
            String formatUri = new FFProbeParser(allowedFormats)
                    .getFormatURIFromFFProbeOutput(FFProbeOutput);
            String message = "Processed by '" + getClass().getName() + "'";
            String fileObjectPid;

            fileObjectPid = centralWebservice.getFileObjectWithURL(reklameContext.getRemoteURL());
            if (fileObjectPid == null) {
                // If not found, clone reklameFile template (config)
                fileObjectPid = centralWebservice.newObject(fileTemplate, null, message);
                centralWebservice.addFileFromPermanentURL(fileObjectPid, reklameContext.getFilename(), null,
                                                          reklameContext.getRemoteURL(), formatUri, message);
            }

            // Mark object as in progress
            centralWebservice.markInProgressObject(Arrays.asList(fileObjectPid), message);

            // Update elements of object from context
            setDatastreamContents(centralWebservice, fileObjectPid, "FFPROBE", reklameContext.getFfprobeContents(),
                                  message);
            setDatastreamContents(centralWebservice, fileObjectPid, "FFPROBE_ERROR_LOG", reklameContext.getFfprobeErrorContents(),
                                  message);
            // Checksum is assumed to be part of received metadata.
            setDatastreamContents(centralWebservice, fileObjectPid, "REKLAME_METADATA",
                                  reklameContext.getReklameMetadata(), message);

            // Mark object as published
            centralWebservice.markPublishedObject(Arrays.asList(fileObjectPid), message);

            // Get pbcore from context
            String pbCoreMetadata = reklameContext.getPbCoreContents();
            if (pbCoreMetadata != null) {

                Document pbcoreDocument = DOM.stringToDOM(pbCoreMetadata, true);

                // Template object to clone to get new reklame objects, get from properties file or command line
                String reklameTemplate = reklameContext.getReklameTemplatePid();
                if (reklameTemplate == null) {
                    reklameTemplate = config
                            .getProperty(REKLAMETEMPLATE_PROPERTY, "doms:Template_Reklamefilm"); // 2nd arg is default value
                }

                // Via DOMS Central, get PID of DOMS reklamefilm object which corresponds
                // to the file with the given ID (ID from pbcore).
                String pbcoreIdentifier = getIdFromPbCore(pbcoreDocument);
                List<String> reklameObjectPids;
                String reklameObjectPid;

                reklameObjectPids = centralWebservice.findObjectFromDCIdentifier(pbcoreIdentifier);
                if (reklameObjectPids == null || reklameObjectPids.size() == 0) {
                    // If not found, clone reklamefilm template (config)
                    reklameObjectPid = centralWebservice
                            .newObject(reklameTemplate, Arrays.asList(pbcoreIdentifier), message);
                } else {
                    // there should be only one, but if there are more, pick the first one.
                    reklameObjectPid = reklameObjectPids.get(0);
                }

                // Mark object as in progress
                centralWebservice.markInProgressObject(Arrays.asList(reklameObjectPid), message);

                // Add pbcore metadata
                setDatastreamContents(centralWebservice, reklameObjectPid, "PBCORE", reklameContext.getPbCoreContents(),
                                      message);
                // Set label
                centralWebservice.setObjectLabel(reklameObjectPid, getTitleFromPbCore(pbcoreDocument), message);
                Relation rel = new Relation();
                rel.setLiteral(false);
                rel.setSubject(reklameObjectPid);
                rel.setPredicate("http://doms.statsbiblioteket.dk/relations/default/0/1/#hasFile");
                rel.setObject(fileObjectPid);
                centralWebservice.addRelation(reklameObjectPid, rel, message);

                // Mark object as published
                centralWebservice.markPublishedObject(Arrays.asList(reklameObjectPid), message);
            }

            return fileObjectPid;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String getIdFromPbCore(Document pbcoreDocument) throws Exception {
        XPathSelector selector = DOM.createXPathSelector("p", "http://www.pbcore.org/PBCore/PBCoreNamespace.html");
        String id = selector.selectString(pbcoreDocument,
                                          "/p:PBCoreDescriptionDocument/p:pbcoreIdentifier[1]/p:identifier/text()");
        return id;
    }

    private String getTitleFromPbCore(Document pbcoreDocument) throws Exception {
        XPathSelector selector = DOM.createXPathSelector("p", "http://www.pbcore.org/PBCore/PBCoreNamespace.html");
        String id = selector.selectString(pbcoreDocument,
                                          "/p:PBCoreDescriptionDocument/p:pbcoreTitle[1]/p:title/text()");
        return id;
    }
}
