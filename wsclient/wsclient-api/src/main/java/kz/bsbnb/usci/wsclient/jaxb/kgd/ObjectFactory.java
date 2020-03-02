
package kz.bsbnb.usci.wsclient.jaxb.kgd;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the kz.nationalbank.ws.kgd package.
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

    private final static QName _SendMessageException_QNAME = new QName("http://nationalbank.kz/ws/EiccToKgdUniversal/", "sendMessageException");
    private final static QName _SendMessageResponse_QNAME = new QName("http://nationalbank.kz/ws/EiccToKgdUniversal/", "sendMessageResponse");
    private final static QName _SendMessageRequest_QNAME = new QName("http://nationalbank.kz/ws/EiccToKgdUniversal/", "sendMessageRequest");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: kz.nationalbank.ws.kgd
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link RequestMessage }
     * 
     */
    public RequestMessage createRequestMessage() {
        return new RequestMessage();
    }

    /**
     * Create an instance of {@link ExceptionMessage }
     * 
     */
    public ExceptionMessage createExceptionMessage() {
        return new ExceptionMessage();
    }

    /**
     * Create an instance of {@link ResponseMessage }
     * 
     */
    public ResponseMessage createResponseMessage() {
        return new ResponseMessage();
    }

    /**
     * Create an instance of {@link RequestType }
     * 
     */
    public RequestType createRequestType() {
        return new RequestType();
    }

    /**
     * Create an instance of {@link StatusType }
     * 
     */
    public StatusType createStatusType() {
        return new StatusType();
    }

    /**
     * Create an instance of {@link OperationType }
     * 
     */
    public OperationType createOperationType() {
        return new OperationType();
    }

    /**
     * Create an instance of {@link PackageType }
     * 
     */
    public PackageType createPackageType() {
        return new PackageType();
    }

    /**
     * Create an instance of {@link RequestMessage.Sender }
     * 
     */
    public RequestMessage.Sender createRequestMessageSender() {
        return new RequestMessage.Sender();
    }

    /**
     * Create an instance of {@link ExceptionMessage.Fault }
     * 
     */
    public ExceptionMessage.Fault createExceptionMessageFault() {
        return new ExceptionMessage.Fault();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExceptionMessage }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nationalbank.kz/ws/EiccToKgdUniversal/", name = "sendMessageException")
    public JAXBElement<ExceptionMessage> createSendMessageException(ExceptionMessage value) {
        return new JAXBElement<ExceptionMessage>(_SendMessageException_QNAME, ExceptionMessage.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ResponseMessage }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nationalbank.kz/ws/EiccToKgdUniversal/", name = "sendMessageResponse")
    public JAXBElement<ResponseMessage> createSendMessageResponse(ResponseMessage value) {
        return new JAXBElement<ResponseMessage>(_SendMessageResponse_QNAME, ResponseMessage.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RequestMessage }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nationalbank.kz/ws/EiccToKgdUniversal/", name = "sendMessageRequest")
    public JAXBElement<RequestMessage> createSendMessageRequest(RequestMessage value) {
        return new JAXBElement<RequestMessage>(_SendMessageRequest_QNAME, RequestMessage.class, null, value);
    }

}
