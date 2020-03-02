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
import java.time.LocalDate;

/**
 * @author Artur Tkachenko
 * @author Kanat Tulbassiev
 */

@Component
@Scope("prototype")
public class CreditContractParser extends BatchParser {
    public CreditContractParser() {
        super();
    }

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("contract"), respondentId, batch.getReportDate(), batch.getId());
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "contract":
                break;
            case "no":
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("no", new BaseValue(trim(event.asCharacters().getData())));
                break;
            case "date":
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("date", new BaseValue(LocalDate.parse(trim(event.asCharacters().getData()))));
                break;
            default:
                throw new UnknownTagException(localName);
        }
        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "contract":
                return true;
            case "no":
                break;
            case "date":
                break;
            default:
                throw new UnknownTagException(localName);
        }
        return false;
    }
}

