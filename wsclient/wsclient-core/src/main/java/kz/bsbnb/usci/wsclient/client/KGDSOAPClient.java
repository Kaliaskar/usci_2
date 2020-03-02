package kz.bsbnb.usci.wsclient.client;

import kz.bsbnb.usci.wsclient.config.HttpLoggingUtils;
import kz.bsbnb.usci.wsclient.jaxb.ctr.Entities;
import kz.bsbnb.usci.wsclient.jaxb.ctr.response.ProcessingStatus;
import kz.bsbnb.usci.wsclient.jaxb.kgd.*;
import kz.bsbnb.usci.wsclient.jaxb.test.AncInfo;
import kz.bsbnb.usci.wsclient.model.ctrkgd.Request;
import kz.bsbnb.usci.wsclient.model.ctrkgd.RequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.ws.FaultAwareWebServiceMessage;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceMessageExtractor;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.support.MarshallingUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.GregorianCalendar;
import java.util.UUID;

@Component
public class KGDSOAPClient {
    private static final Logger logger = LoggerFactory.getLogger(KGDSOAPClient.class);
    private WebServiceTemplate kgdWebServiceTemplate;
    private Jaxb2Marshaller kgdJaxb2Marshaller;

    public KGDSOAPClient(WebServiceTemplate kgdWebServiceTemplate, Jaxb2Marshaller kgdJaxb2Marshaller) {
        this.kgdWebServiceTemplate = kgdWebServiceTemplate;
        this.kgdJaxb2Marshaller = kgdJaxb2Marshaller;
    }

    public ResponseMessage testResponseMessage() {
        ObjectFactory objectFactory = new ObjectFactory();

        OperationType operationType = new OperationType();
        operationType.setCode("ancInfo");
        operationType.setDescription("Информация по валютному договору по экспорту или импорту с учетным номером");

        RequestType requestType = new RequestType();
        requestType.setCode("0");
        requestType.setDescription("Запрос");

        PackageType packageType = new PackageType();
        packageType.setRequestId("132131321321321321323232321321");

        ////////////////
        AncInfo ancInfo = new AncInfo();
        AncInfo.Header header = new AncInfo.Header();
        header.setGuid("{7b3Ca5DA-Ab9A-eeeB-Ceb6-9AaFEb1a4f1f}");
        LocalDateTime localDateTime = LocalDateTime.now();
        GregorianCalendar calendar = GregorianCalendar.from(localDateTime.atZone(ZoneId.of("Asia/Almaty")));
        XMLGregorianCalendar xmlDate = null;
        try {
            xmlDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
        } catch (DatatypeConfigurationException e) {
            logger.error("Ошибка при обработке дат: ", e);
        }
        header.setCreationDateTime(xmlDate);

        AncInfo.Data ancInfoData = new AncInfo.Data();
        AncInfo.Data.ContractRegistrationInfo contractRegistrationInfo = new AncInfo.Data.ContractRegistrationInfo();
        AncInfo.Data.ContractPartiesInfo contractPartiesInfo = new AncInfo.Data.ContractPartiesInfo();
        AncInfo.Data.ContractDetailsInfo contractDetailsInfo = new AncInfo.Data.ContractDetailsInfo();
        AncInfo.Data.TerminationNoticeInfo terminationNoticeInfo = new AncInfo.Data.TerminationNoticeInfo();
        AncInfo.Data.ContractClosureInfo contractClosureInfo = new AncInfo.Data.ContractClosureInfo();

        contractRegistrationInfo.setAccountingNumber("string");
        LocalDate localDate = LocalDate.now();
        calendar = GregorianCalendar.from(localDate.atStartOfDay().atZone(ZoneId.of("Asia/Almaty")));
        try {
            xmlDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
        } catch (DatatypeConfigurationException e) {
            logger.error("Ошибка при обработке дат: ", e);
        }
        contractRegistrationInfo.setDateOfRegistration(xmlDate);

        AncInfo.Data.ContractPartiesInfo.ResidentInfo residentInfo = new AncInfo.Data.ContractPartiesInfo.ResidentInfo();
        AncInfo.Data.ContractPartiesInfo.NonResidentInfo nonResidentInfo = new AncInfo.Data.ContractPartiesInfo.NonResidentInfo();
        residentInfo.setIin("157587093945");
        residentInfo.setRegionCode("48");
        nonResidentInfo.setName("string");
        nonResidentInfo.setCountryCode("RT");
        contractPartiesInfo.setResidentInfo(residentInfo);
        contractPartiesInfo.setNonResidentInfo(nonResidentInfo);

        contractDetailsInfo.setNumber("string");
        contractDetailsInfo.setDateOfExecution(xmlDate);
        contractDetailsInfo.setSum(BigDecimal.valueOf(-1117169.77));
        contractDetailsInfo.setCurrency("CTB");
        contractDetailsInfo.setTermOfRepatriation("448.44");

        terminationNoticeInfo.setNoticeSentDate(xmlDate);

        contractClosureInfo.setClosureDate(xmlDate);
        contractClosureInfo.setClosureBasis(BigDecimal.valueOf(18));

        ancInfoData.setContractRegistrationInfo(contractRegistrationInfo);
        ancInfoData.setContractPartiesInfo(contractPartiesInfo);
        ancInfoData.setContractDetailsInfo(contractDetailsInfo);
        ancInfoData.setTerminationNoticeInfo(terminationNoticeInfo);
        ancInfoData.setContractClosureInfo(contractClosureInfo);
        ancInfoData.setNote("string");

        ancInfo.setHeader(header);
        ancInfo.setData(ancInfoData);
        ///////////////

        String xml = null;
        try {
            xml = marshal(AncInfo.class, ancInfo);
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Document parsedDoc = parseStringToXMLDocument(xml);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Document doc = builder.newDocument();
        Element root = doc.createElement("data");
        doc.appendChild(root);
        Node copy = doc.importNode(parsedDoc.getDocumentElement(), true);
        root.appendChild(copy);
        packageType.setData(doc.getDocumentElement());

        RequestMessage.Sender sender = new RequestMessage.Sender();
        sender.setFullName("Nikolay Protsenko");
        sender.setSystemName("eivk");

        RequestMessage requestMessage = objectFactory.createRequestMessage();
        requestMessage.setOperation(operationType);
        requestMessage.setRequestType(requestType);
        requestMessage.setSender(sender);
        requestMessage.setPackage(packageType);

        JAXBElement<RequestMessage> request = objectFactory.createSendMessageRequest(requestMessage);
        JAXBElement<ResponseMessage> response = null;

        response = (JAXBElement<ResponseMessage>) kgdWebServiceTemplate.marshalSendAndReceive(request);

        return response.getValue();

    }

    public Request ctrRequest(Entities entities) {
        ObjectFactory objectFactory = new ObjectFactory();

        OperationType operationType = new OperationType();
        operationType.setCode("usciCtr");
        operationType.setDescription("Информация о валютной операции");

        RequestType requestType = new RequestType();
        requestType.setCode("0");
        requestType.setDescription("Запрос");

        PackageType packageType = new PackageType();
        UUID uuid = UUID.randomUUID();
        String uuidStr = uuid.toString();
        packageType.setRequestId(uuidStr);

        String xml = null;
        try {
            xml = marshal(Entities.class, entities);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Document parsedDoc = parseStringToXMLDocument(xml);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Document doc = builder.newDocument();
        Element root = doc.createElement("data");
        doc.appendChild(root);
        Node copy = doc.importNode(parsedDoc.getDocumentElement(), true);
        root.appendChild(copy);
        packageType.setData(doc.getDocumentElement());

        RequestMessage.Sender sender = new RequestMessage.Sender();
        sender.setFullName("ESSP");
        sender.setSystemName("essp");

        RequestMessage requestMessage = objectFactory.createRequestMessage();
        requestMessage.setOperation(operationType);
        requestMessage.setRequestType(requestType);
        requestMessage.setSender(sender);
        requestMessage.setPackage(packageType);

        Request ctrRequest = new Request();
        ctrRequest.setRequestId(uuidStr);

        JAXBElement<RequestMessage> request = objectFactory.createSendMessageRequest(requestMessage);
        JAXBElement<ResponseMessage> response = null;
        try {
            response = (JAXBElement<ResponseMessage>) kgdWebServiceTemplate.sendAndReceive(message -> {
                MarshallingUtils.marshal(kgdJaxb2Marshaller, request, message);
                ctrRequest.setRequestBody(HttpLoggingUtils.getMessage(message));
                logger.info(ctrRequest.getRequestBody());
            }, message -> {
                ctrRequest.setResponseBody(HttpLoggingUtils.getMessage(message));
                logger.info(ctrRequest.getResponseBody());
                return MarshallingUtils.unmarshal(kgdJaxb2Marshaller, message);
            });

            ProcessingStatus processingStatus = new ProcessingStatus();
            try {
                processingStatus = unmarshal(ProcessingStatus.class, response.getValue().getPackage().getData());
            } catch (Exception e) {
                e.printStackTrace();
            }
            logger.info("Status is " + processingStatus.getDescription());
            if (response.getValue() != null) {
                //TODO: на время теста смотрим на этот статус, в реале надо поменять на package.data.processStatus
                if (response.getValue().getStatus().getCode().equals("000")) {
                    ctrRequest.setRequestStatus(RequestStatus.SUCCESS);
                } else {
                    ctrRequest.setRequestStatus(RequestStatus.ERROR);
                }
            }
        } catch (SoapFaultClientException e) {
            ctrRequest.setResponseBody(HttpLoggingUtils.getMessage(e.getWebServiceMessage()));
            logger.info(ctrRequest.getResponseBody());
            ctrRequest.setRequestStatus(RequestStatus.ERROR);
            return ctrRequest;
        }

        return ctrRequest;
    }

    private static <T> String marshal(Class<T> clazz, T instance) throws JAXBException, IOException {
        javax.xml.bind.JAXBContext jc = javax.xml.bind.JAXBContext.newInstance(clazz);
        javax.xml.bind.Marshaller m = jc.createMarshaller();
        try (java.io.StringWriter writer = new java.io.StringWriter()) {
            m.marshal(instance, writer);
            return writer.toString().replaceAll(">\\s+<", "><");
        }
    }

    private static <T> T unmarshal(Class<T> clazz, Object node) throws Exception {
        javax.xml.bind.JAXBContext jc = javax.xml.bind.JAXBContext.newInstance(clazz);
        javax.xml.bind.Unmarshaller m = jc.createUnmarshaller();
        org.w3c.dom.Node nodeE = (org.w3c.dom.Node) node;
        return m.unmarshal(nodeE.getFirstChild(), clazz).getValue();
    }

    public static Document parseStringToXMLDocument(String xmlString) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setCoalescing(false);
            dbf.setNamespaceAware(true);
            DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
            return documentBuilder.parse(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
