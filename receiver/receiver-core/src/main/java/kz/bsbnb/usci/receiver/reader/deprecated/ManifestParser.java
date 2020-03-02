package kz.bsbnb.usci.receiver.reader.deprecated;

import kz.bsbnb.usci.eav.model.meta.MetaDataType;
import kz.bsbnb.usci.receiver.model.deprecated.ManifestData;
import kz.bsbnb.usci.receiver.model.exception.ReceiverException;
import kz.bsbnb.usci.receiver.reader.MainReader;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.time.format.DateTimeParseException;

/**
 * @author Dauletkhan Tulendiev
 */

@Deprecated
public class ManifestParser extends MainReader {
    private final ManifestData manifestData = new ManifestData();
    private String currentName;

    @Override
    public void startElement(XMLEvent event, StartElement startElement, String localName) {
        switch (localName) {
            case "manifest":
                break;
            case "type":
                break;
            case "userid":
                break;
            case "size":
                break;
            case "date":
                break;
            case "name":
                break;
            case "value":
                break;
        }
    }

    @Override
    public boolean endElement(String localName) {
        switch (localName) {
            case "manifest":
                return true;
            case "type":
                manifestData.setType(data.toString());
                data.setLength(0);
                break;
            case "product":
                manifestData.setProduct(data.toString());
                data.setLength(0);
                break;
            case "userid":
                manifestData.setUserId(Long.parseLong(data.toString()));
                data.setLength(0);
                break;
            case "size":
                manifestData.setSize(Integer.parseInt(data.toString()));
                data.setLength(0);
                break;
            case "date":
                try {
                    manifestData.setReportDate(MetaDataType.parseDate(data.toString()));
                } catch (DateTimeParseException e) {
                    throw new ReceiverException(e.getMessage());
                }
                data.setLength(0);
                break;
            case "name":
                currentName = data.toString();
                data.setLength(0);
                break;
            case "value":
                manifestData.getAdditionalParams().put(currentName, data.toString());
                data.setLength(0);
                break;
            case "maintenance":
                boolean isMaintenance = false;
                if(data != null) {
                    isMaintenance = "1".equals(data.toString()) || "true".equals(data.toString().toLowerCase());
                }
                manifestData.setMaintenance(isMaintenance);
                break;
        }

        return false;
    }

    public ManifestData getManifestData() {
        return manifestData;
    }

}
