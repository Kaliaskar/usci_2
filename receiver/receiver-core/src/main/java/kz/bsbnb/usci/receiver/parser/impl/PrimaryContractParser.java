package kz.bsbnb.usci.receiver.parser.impl;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseValue;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.receiver.parser.BatchParser;
import kz.bsbnb.usci.receiver.parser.exceptions.UnknownTagException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * @author Artur Tkachenko
 * @author Kanat Tulbassiev
 */

@Component
@Scope("prototype")
public class PrimaryContractParser extends BatchParser {
    public PrimaryContractParser() {
        super();
    }

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("primary_contract"), respondentId, batch.getReportDate(), batch.getId());
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "primary_contract":
                break;
            case "no":
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("no", new BaseValue(trim(event.asCharacters().getData())));
                break;
            case "date":
                event = (XMLEvent) xmlReader.next();
                String dateRaw = trim(event.asCharacters().getData());
                try {
                    currentBaseEntity.put("date", new BaseValue(LocalDate.parse(dateRaw)));
                } catch (DateTimeParseException e) {
                    currentBaseEntity.addValidationError("Неправильная дата: " + dateRaw);
                }
                break;
            default:
               throw  new UsciException("Нет такого тега: " + localName);

        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "primary_contract":
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
