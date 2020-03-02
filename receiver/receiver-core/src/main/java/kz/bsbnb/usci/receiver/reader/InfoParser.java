package kz.bsbnb.usci.receiver.reader;

import kz.bsbnb.usci.eav.model.meta.MetaDataType;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.receiver.model.deprecated.InfoData;

import javax.xml.namespace.QName;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.time.format.DateTimeParseException;

/**
 * @author Dauletkhan Tulendiev
 */

@Deprecated
public class InfoParser extends MainReader {
    private final InfoData infoData = new InfoData();

    @Override
    public void startElement(XMLEvent event, StartElement startElement, String localName) {
        switch (localName) {
            case "code":
                break;
            case "doc":
                infoData.setDocType(event.asStartElement().getAttributeByName(new QName("doc_type")).getValue());
                break;
            case "no":
                break;
            case "account_date":
                break;
            case "report_date":
                break;
            case "actual_credit_count":
                break;
            case "maintenance":
                String val = ((XMLEvent) this.xmlReader.next()).asCharacters().getData();
                infoData.setMaintenance("1".equals(val) || "true".equals(val));
                break;
        }
    }

    @Override
    public boolean endElement(String localName) {
        switch (localName) {
            case "info":
                return true;
            case "code":
                infoData.setCode(data.toString());
                data.setLength(0);
                break;
            case "doc":
                break;
            case "no":
                infoData.setDocValue(data.toString());
                data.setLength(0);
                break;
            case "account_date":
                try {
                    infoData.setAccountDate(MetaDataType.parseSplashDate(data.toString()));
                } catch (DateTimeParseException e) {
                    throw new UsciException(e.getMessage());
                }
                data.setLength(0);
                break;
            case "report_date":
                try {
                    infoData.setReportDate(MetaDataType.parseSplashDate(data.toString()));
                } catch (DateTimeParseException e) {
                    throw new UsciException(e.getMessage());
                }
                data.setLength(0);
                break;
            case "actual_credit_count":
                try {
                    infoData.setActualCreditCount(Long.parseLong(data.toString()));
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                }
                data.setLength(0);
                break;
            case "name":
                data.setLength(0);
                break;
        }

        return false;
    }

    public InfoData getInfoData() {
        return infoData;
    }

}
