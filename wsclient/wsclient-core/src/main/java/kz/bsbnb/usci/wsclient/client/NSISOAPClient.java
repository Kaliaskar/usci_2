package kz.bsbnb.usci.wsclient.client;

import kz.bsbnb.usci.wsclient.jaxb.nsi.GETGUIDE;
import kz.bsbnb.usci.wsclient.jaxb.nsi.GETGUIDEResponse;
import kz.bsbnb.usci.wsclient.jaxb.nsi.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.GregorianCalendar;

@Component
public class NSISOAPClient {
    private static final Logger logger = LoggerFactory.getLogger(NSISOAPClient.class);
    private WebServiceTemplate nsiWebServiceTemplate;

    public NSISOAPClient(WebServiceTemplate nsiWebServiceTemplate) {
        this.nsiWebServiceTemplate = nsiWebServiceTemplate;
    }

    public GETGUIDEResponse getGuideResponse(LocalDateTime beginDate, LocalDateTime endDate, String guideCode) {
        ObjectFactory objectFactory = new ObjectFactory();

        GregorianCalendar begCalendar = GregorianCalendar.from(beginDate.atZone(ZoneId.of("Asia/Almaty")));
        GregorianCalendar endCalendar = GregorianCalendar.from(endDate.atZone(ZoneId.of("Asia/Almaty")));
        XMLGregorianCalendar begXDate = null;
        XMLGregorianCalendar endXDate = null;
        try {
            begXDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(begCalendar);
            endXDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(endCalendar);
        } catch (DatatypeConfigurationException e) {
            logger.error("Ошибка при обработке дат: ", e);
        }

        GETGUIDE getguide = objectFactory.createGETGUIDE();

        getguide.setBeginDate(begXDate);
        getguide.setEndDate(endXDate);
        getguide.setGuideCode(guideCode);
        getguide.setType("CHAD");

        JAXBElement<GETGUIDE> request = objectFactory.createGETGUIDE(getguide);
        JAXBElement<GETGUIDEResponse> response = null;

        response = (JAXBElement<GETGUIDEResponse>) nsiWebServiceTemplate.marshalSendAndReceive(request);
        return response.getValue();
    }
}
