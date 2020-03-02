
package kz.bsbnb.usci.wsclient.jaxb.kgd;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.9-b130926.1035
 * Generated source version: 2.2
 * 
 */
@WebServiceClient(name = "EiccToKgdUniversalService", targetNamespace = "http://nationalbank.kz/ws/EiccToKgdUniversal/", wsdlLocation = "http://10.8.255.50:1274/EiccToKgdUniversal?wsdl")
public class EiccToKgdUniversalService
    extends Service
{

    private final static URL EICCTOKGDUNIVERSALSERVICE_WSDL_LOCATION;
    private final static WebServiceException EICCTOKGDUNIVERSALSERVICE_EXCEPTION;
    private final static QName EICCTOKGDUNIVERSALSERVICE_QNAME = new QName("http://nationalbank.kz/ws/EiccToKgdUniversal/", "EiccToKgdUniversalService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://10.8.255.50:1274/EiccToKgdUniversal?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        EICCTOKGDUNIVERSALSERVICE_WSDL_LOCATION = url;
        EICCTOKGDUNIVERSALSERVICE_EXCEPTION = e;
    }

    public EiccToKgdUniversalService() {
        super(__getWsdlLocation(), EICCTOKGDUNIVERSALSERVICE_QNAME);
    }

    public EiccToKgdUniversalService(WebServiceFeature... features) {
        super(__getWsdlLocation(), EICCTOKGDUNIVERSALSERVICE_QNAME, features);
    }

    public EiccToKgdUniversalService(URL wsdlLocation) {
        super(wsdlLocation, EICCTOKGDUNIVERSALSERVICE_QNAME);
    }

    public EiccToKgdUniversalService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, EICCTOKGDUNIVERSALSERVICE_QNAME, features);
    }

    public EiccToKgdUniversalService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public EiccToKgdUniversalService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns EiccToKgdUniversalPortType
     */
    @WebEndpoint(name = "EiccToKgdUniversalPort")
    public EiccToKgdUniversalPortType getEiccToKgdUniversalPort() {
        return super.getPort(new QName("http://nationalbank.kz/ws/EiccToKgdUniversal/", "EiccToKgdUniversalPort"), EiccToKgdUniversalPortType.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns EiccToKgdUniversalPortType
     */
    @WebEndpoint(name = "EiccToKgdUniversalPort")
    public EiccToKgdUniversalPortType getEiccToKgdUniversalPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://nationalbank.kz/ws/EiccToKgdUniversal/", "EiccToKgdUniversalPort"), EiccToKgdUniversalPortType.class, features);
    }

    private static URL __getWsdlLocation() {
        if (EICCTOKGDUNIVERSALSERVICE_EXCEPTION!= null) {
            throw EICCTOKGDUNIVERSALSERVICE_EXCEPTION;
        }
        return EICCTOKGDUNIVERSALSERVICE_WSDL_LOCATION;
    }

}
