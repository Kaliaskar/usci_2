package kz.bsbnb.usci.wsclient.service;

import kz.bsbnb.usci.wsclient.client.NSISOAPClient;
import kz.bsbnb.usci.wsclient.dao.NSIDao;
import kz.bsbnb.usci.wsclient.jaxb.nsi.GETGUIDEResponse;
import kz.bsbnb.usci.wsclient.model.currency.HolidayEntityCustom;
import kz.bsbnb.usci.wsclient.model.currency.NSIEntity;
import kz.bsbnb.usci.wsclient.model.currency.CurrencyEntityCustom;
import kz.bsbnb.usci.wsclient.model.currency.NSIEntitySystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class NSIServiceImpl implements  NSIService{
    private static final Logger logger = LoggerFactory.getLogger(NSIServiceImpl.class);

    private final NSIDao nsiDao;
    private final NSISOAPClient nsiSoapClient;

    public NSIServiceImpl(NSIDao nsiDao, NSISOAPClient nsiSoapClient) {
        this.nsiDao = nsiDao;
        this.nsiSoapClient = nsiSoapClient;
    }

    @Override
    public void saveCurrencyRates(LocalDate beginDate, LocalDate endDate) {
        GETGUIDEResponse response = nsiSoapClient.getGuideResponse(beginDate.atTime(0,0,0), endDate.atTime(0,0,0), "NSI_NBRK_CRCY_COURSE");
        String result = response.getReturn().getResult();
        if (!result.equals("")) {
            try {
                DocumentBuilderFactory dbf =DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                InputSource is = new InputSource(new StringReader(result));
                Document doc = db.parse(is);
                doc.getDocumentElement().normalize();

                List<NSIEntity> NSIEntityList = new ArrayList<>();
                NodeList nList = doc.getElementsByTagName("Entity");

                for (int temp = 0; temp < nList.getLength(); temp++) {
                    NSIEntity NSIEntity = new NSIEntity();
                    NSIEntitySystem NSIEntitySystem = new NSIEntitySystem();
                    CurrencyEntityCustom currencyEntityCustom = new CurrencyEntityCustom();

                    Node nNode = nList.item(temp);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;

                        NSIEntitySystem.setOperType(eElement.getElementsByTagName("OperType").item(0).getTextContent());
                        NSIEntitySystem.setEntityID(Long.parseLong(eElement.getElementsByTagName("EntityID").item(0).getTextContent()));
                        NSIEntitySystem.setOperDate(LocalDate.parse(eElement.getElementsByTagName("OperDate").item(0).getTextContent()));
                        NSIEntitySystem.setBeginDate(LocalDate.parse(eElement.getElementsByTagName("BeginDate").item(0).getTextContent()));
                        NSIEntitySystem.setEndDate(LocalDate.parse(eElement.getElementsByTagName("EndDate").item(0).getTextContent()));
                        NSIEntity.setNSIEntitySystem(NSIEntitySystem);

                        currencyEntityCustom.setCurrId(Long.parseLong(eElement.getElementsByTagName("CurrId").item(0).getTextContent()));
                        currencyEntityCustom.setCurrCode(eElement.getElementsByTagName("CurrCode").item(0).getTextContent());
                        currencyEntityCustom.setCourseDate(LocalDate.parse(eElement.getElementsByTagName("CourseDate").item(0).getTextContent()));
                        currencyEntityCustom.setCourseKind(Long.parseLong(eElement.getElementsByTagName("CourseKind").item(0).getTextContent()));
                        currencyEntityCustom.setCourse(Double.valueOf(eElement.getElementsByTagName("Course").item(0).getTextContent().replaceAll(",", ".")));
                        currencyEntityCustom.setCorellation(Long.parseLong(eElement.getElementsByTagName("Corellation").item(0).getTextContent()));
                        currencyEntityCustom.setWdKind(Long.parseLong(eElement.getElementsByTagName("WdKind").item(0).getTextContent()));
                        NSIEntity.setNSIEntityCustom(currencyEntityCustom);

                        NSIEntityList.add(NSIEntity);
                    }
                }
                nsiDao.saveCurrencyRates(NSIEntityList);
            } catch (Exception e) {
                logger.error("Ошибка при парсинге ответа от веб-сервиса: ", e);
            }
        } else {
            logger.error("Другой ответ от веб-сервиса: "+result);
        }
    }

    @Override
    public LocalDate getMaxCourseDate() {
        return nsiDao.getMaxCourseDate();
    }

    @Override
    public void saveHolidayDates(LocalDate beginDate, LocalDate endDate) {
        GETGUIDEResponse response = nsiSoapClient.getGuideResponse(beginDate.atTime(0,0,0), endDate.atTime(0,0,0), "NSI_NBRK_HOLIDAY");
        String result = response.getReturn().getResult();
        if (!result.equals("")) {
            try {
                DocumentBuilderFactory dbf =DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                InputSource is = new InputSource(new StringReader(result));
                Document doc = db.parse(is);
                doc.getDocumentElement().normalize();

                List<NSIEntity> NSIEntityList = new ArrayList<>();
                NodeList nList = doc.getElementsByTagName("Entity");

                for (int temp = 0; temp < nList.getLength(); temp++) {
                    NSIEntity NSIEntity = new NSIEntity();
                    NSIEntitySystem NSIEntitySystem = new NSIEntitySystem();
                    HolidayEntityCustom holidayEntityCustom = new HolidayEntityCustom();

                    Node nNode = nList.item(temp);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;

                        NSIEntitySystem.setOperType(eElement.getElementsByTagName("OperType").item(0).getTextContent());
                        NSIEntitySystem.setEntityID(Long.parseLong(eElement.getElementsByTagName("EntityID").item(0).getTextContent()));
                        NSIEntitySystem.setOperDate(LocalDate.parse(eElement.getElementsByTagName("OperDate").item(0).getTextContent()));
                        NSIEntitySystem.setBeginDate(LocalDate.parse(eElement.getElementsByTagName("BeginDate").item(0).getTextContent()));
                        NSIEntitySystem.setEndDate(LocalDate.parse(eElement.getElementsByTagName("EndDate").item(0).getTextContent()));
                        NSIEntity.setNSIEntitySystem(NSIEntitySystem);

                        holidayEntityCustom.setHolidayDate(LocalDate.parse(eElement.getElementsByTagName("Holiday_Date").item(0).getTextContent()));
                        holidayEntityCustom.setHolidayType(Long.parseLong(eElement.getElementsByTagName("Holiday_Type").item(0).getTextContent()));
                        NSIEntity.setNSIEntityCustom(holidayEntityCustom);

                        NSIEntityList.add(NSIEntity);
                    }
                }
                nsiDao.saveHolidayDates(NSIEntityList);
            } catch (Exception e) {
                logger.error("Ошибка при парсинге ответа от веб-сервиса: ", e);
            }
        } else {
            logger.error("Другой ответ от веб-сервиса: "+result);
        }
    }
}
