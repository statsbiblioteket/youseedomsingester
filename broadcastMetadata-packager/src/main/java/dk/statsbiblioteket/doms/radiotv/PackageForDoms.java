package dk.statsbiblioteket.doms.radiotv;

import dk.statsbiblioteket.doms.common.DomsOptionParser;
import dk.statsbiblioteket.doms.common.IngestContext;
import dk.statsbiblioteket.doms.common.OptionParseException;
import dk.statsbiblioteket.medieplatform.doms.autogenerated.BroadcastMetadata;
import dk.statsbiblioteket.medieplatform.doms.autogenerated.Channel;
import dk.statsbiblioteket.medieplatform.doms.autogenerated.Channels;
import dk.statsbiblioteket.medieplatform.doms.autogenerated.ObjectFactory;
import org.apache.commons.cli.*;
import org.apache.commons.cli.ParseException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.text.*;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created with IntelliJ IDEA.
 * User: abr
 * Date: 10/1/12
 * Time: 11:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class PackageForDoms {


    protected static final Option CHANNELID
            = new Option("channelID", true, "The sb channel id");
    protected static final Option FORMAT
            = new Option("format", true, "The format of the file");
    protected static final Option STARTIIME
            = new Option("startTime", true, "The start time stamp");
    protected static final Option ENDTIME
            = new Option("endTime", true, "The end time stamp");

    protected static final Option RECORDER
            = new Option("recorder", true, "The recorder");
    protected static final Option FILENAME
            = new Option("filename", true, "The filename");
    protected static final Option CHECKSUM
            = new Option("checksum", true, "The checksum");
    protected static final Option MUXCHANNELNR
            = new Option("muxChannelNR", true, "The mux channel nr");
    private static Options options;
    private static DateFormat ourDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");


    public static void main(String... args) throws OptionParseException, java.text.ParseException, DatatypeConfigurationException, JAXBException {
        BroadcastMetadata broadcast = new ObjectFactory().createBroadcastMetadata();
        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        options = new Options();
        options.addOption(CHANNELID);
        options.addOption(FORMAT);
        options.addOption(STARTIIME);
        options.addOption(ENDTIME);
        options.addOption(RECORDER);
        options.addOption(FILENAME);
        options.addOption(CHECKSUM);
        options.addOption(MUXCHANNELNR);

        for (Object option : options.getOptions()) {
            if (option instanceof Option) {
                ((Option) option).setRequired(true);
            }
        }

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            parseError(e.toString());
            throw new OptionParseException(e.getMessage(), e);
        }
        String temp = cmd.getOptionValue(CHANNELID.getOpt(),"");

        Channel channel = new Channel();
        channel.setChannelID(temp);
        temp = cmd.getOptionValue(MUXCHANNELNR.getOpt(),"");
        channel.setMuxProgramNr(Integer.parseInt(temp));
        broadcast.setChannels(new Channels());
        broadcast.getChannels().getChannel().add(channel);

        temp = cmd.getOptionValue(STARTIIME.getOpt(),"");
        GregorianCalendar calStart = new GregorianCalendar();
        calStart.setTime(ourDateFormat.parse(temp));
        broadcast.setStartTime(javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(calStart));

        temp = cmd.getOptionValue(ENDTIME.getOpt(),"");
        GregorianCalendar calEnd = new GregorianCalendar();
        calEnd.setTime(ourDateFormat.parse(temp));
        broadcast.setStopTime(javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(calEnd));

        temp = cmd.getOptionValue(RECORDER.getOpt(),"");
        broadcast.setRecorder(temp);
        temp = cmd.getOptionValue(FILENAME.getOpt(),"");
        broadcast.setFilename(temp);
        temp = cmd.getOptionValue(CHECKSUM.getOpt(),"");
        broadcast.setChecksum(temp);
        temp = cmd.getOptionValue(FORMAT.getOpt(),"");
        broadcast.setFormat(temp);


        JAXBContext.newInstance(BroadcastMetadata.class.getPackage().getName()).createMarshaller().marshal(new ObjectFactory().createBroadcastMetadata(broadcast),System.out);
    }



    protected static void parseError(String message) {
        System.err.println("Error parsing arguments");
        System.err.println(message);
        printUsage();
    }


    protected static void printUsage() {
        final HelpFormatter usageFormatter = new HelpFormatter();
        usageFormatter.printHelp(getHelpText(), options, true);
    }


    protected static String getHelpText() {
        return "packageForDoms";
    }
}
