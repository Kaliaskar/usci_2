package kz.bsbnb.usci.receiver.parser.impl;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseValue;
import kz.bsbnb.usci.receiver.parser.BatchParser;
import kz.bsbnb.usci.receiver.parser.exceptions.UnknownTagException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * @author Artur Tkachenko
 * @author Kanat Tulbassiev
 */

@Component
@Scope("prototype")
public class ChangeRemainsDiscountedValueParser extends BatchParser {

    public ChangeRemainsDiscountedValueParser() {
        super();
    }

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("remains_discounted_value"), respondentId,
                batch.getReportDate(), batch.getId());
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "discounted_value":
                break;
            case "value":
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("value", new BaseValue(new Double(trim(event.asCharacters().getData()))));
                break;
            default:
                throw new UnknownTagException(localName);
        }
        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "discounted_value":
                return true;
            case "value":
                break;
            default:
                throw new UnknownTagException(localName);
        }
        return false;
    }
}
