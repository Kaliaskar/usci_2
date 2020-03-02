
package kz.bsbnb.usci.wsclient.jaxb.nsi;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the service package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _GETGUIDE_QNAME = new QName("http://service/", "GET_GUIDE");
    private final static QName _GETGUIDEResponse_QNAME = new QName("http://service/", "GET_GUIDEResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: service
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GETGUIDE }
     * 
     */
    public GETGUIDE createGETGUIDE() {
        return new GETGUIDE();
    }

    /**
     * Create an instance of {@link GETGUIDEResponse }
     * 
     */
    public GETGUIDEResponse createGETGUIDEResponse() {
        return new GETGUIDEResponse();
    }

    /**
     * Create an instance of {@link ServiceResponse }
     * 
     */
    public ServiceResponse createServiceResponse() {
        return new ServiceResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GETGUIDE }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service/", name = "GET_GUIDE")
    public JAXBElement<GETGUIDE> createGETGUIDE(GETGUIDE value) {
        return new JAXBElement<GETGUIDE>(_GETGUIDE_QNAME, GETGUIDE.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GETGUIDEResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service/", name = "GET_GUIDEResponse")
    public JAXBElement<GETGUIDEResponse> createGETGUIDEResponse(GETGUIDEResponse value) {
        return new JAXBElement<GETGUIDEResponse>(_GETGUIDEResponse_QNAME, GETGUIDEResponse.class, null, value);
    }

}
