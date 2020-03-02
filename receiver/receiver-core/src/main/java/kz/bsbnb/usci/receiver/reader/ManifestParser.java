package kz.bsbnb.usci.receiver.reader;

import kz.bsbnb.usci.eav.model.meta.MetaDataType;
import kz.bsbnb.usci.receiver.model.ManifestData;
import kz.bsbnb.usci.receiver.model.exception.ReceiverException;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.time.format.DateTimeParseException;

/**
 * @author Jandos Iskakov
 */

public class ManifestParser extends MainReader {
    private final ManifestData manifestData = new ManifestData();

    @Override
    public void startElement(XMLEvent event, StartElement startElement, String localName) {
        switch (localName) {
            case "manifest":
                break;
            case "product":
                break;
            case "respondent":
                break;
            case "report_date":
                break;
        }
    }

    @Override
    public boolean endElement(String localName) {
        switch (localName) {
            case "manifest":
                return true;
            case "respondent":
                manifestData.setRespondent(data.toString());
                data.setLength(0);
                break;
            case "product":
                manifestData.setProduct(data.toString());
                data.setLength(0);
                break;
            case "report_date":
                try {
                    manifestData.setReportDate(MetaDataType.parseDate(data.toString()));
                } catch (DateTimeParseException e) {
                    throw new ReceiverException(e.getMessage());
                }
                data.setLength(0);
                break;
        }

        return false;
    }

    ManifestData getManifestData() {
        return manifestData;
    }

}
