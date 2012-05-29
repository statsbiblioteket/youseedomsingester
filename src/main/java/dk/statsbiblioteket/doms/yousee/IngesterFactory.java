package dk.statsbiblioteket.doms.yousee;

import dk.statsbiblioteket.doms.central.CentralWebservice;
import dk.statsbiblioteket.doms.central.CentralWebserviceService;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

/** Create the object instances for doms ingester. */
public class IngesterFactory {
    private Ingester ingester;
    private CentralWebservice centralWebservice;
    private static final String FEDORA_USERNAME_PROPERTY = "dk.statsbiblioteket.doms.yousee.fedorausername";
    private static final String FEDORA_PASSWORD_PROPERTY = "dk.statsbiblioteket.doms.yousee.fedorapassword";
    private static final String FEDORA_WEBSERVICE_URL_PROPERTY = "dk.statsbiblioteket.doms.yousee.fedorawebserviceurl";
    private final Properties config;

    /**
     * Initialise factory with given configuration.
     *
     * @param config The configuration.
     */
    public IngesterFactory(Properties config) {
        if (config == null) {
            this.config = new Properties(System.getProperties());
        } else {
            this.config = config;
        }
    }

    /**
     * Get ingester singleton.
     *
     * @return A doms ingester.
     */
    public synchronized Ingester getIngester() {
        if (ingester == null) {
            ingester = new DomsIngester(config, getWebservice());
        }
        return ingester;
    }

    /**
     * Get doms webservice singleton.
     *
     * @return A doms ingester.
     */
    private synchronized CentralWebservice getWebservice() {
        try {
            if (centralWebservice == null) {
                CentralWebservice webservice = new CentralWebserviceService(
                        new URL(config.getProperty(FEDORA_WEBSERVICE_URL_PROPERTY,
                                                   "http://localhost:7880/centralWebservice-service/central/?wsdl")),
                        new QName("http://central.doms.statsbiblioteket.dk/", "CentralWebserviceService"))
                        .getCentralWebservicePort();
                Map<String, Object> domsAPILogin = ((BindingProvider) webservice).getRequestContext();
                domsAPILogin.put(BindingProvider.USERNAME_PROPERTY,
                                 config.getProperty(FEDORA_USERNAME_PROPERTY, "fedoraAdmin"));
                domsAPILogin.put(BindingProvider.PASSWORD_PROPERTY,
                                 config.getProperty(FEDORA_PASSWORD_PROPERTY, "fedoraAdminPass"));
                centralWebservice = webservice;
            }
            return centralWebservice;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
