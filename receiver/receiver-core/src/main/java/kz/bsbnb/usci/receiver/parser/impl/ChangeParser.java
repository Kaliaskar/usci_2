package kz.bsbnb.usci.receiver.parser.impl;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseValue;
import kz.bsbnb.usci.receiver.parser.BatchParser;
import kz.bsbnb.usci.receiver.parser.exceptions.UnknownTagException;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ChangeParser extends BatchParser {
    @Autowired
    private ChangeTurnoverParser changeTurnoverParser;
    @Autowired
    private ChangeRemainsParser changeRemainsParser;
    @Autowired
    private ChangeCreditFlowParser changeCreditFlowParser;
    private BaseValue maturityDate;
    private BaseValue prolongationDate;

    public ChangeParser() {
        super();
    }

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("change"), respondentId, batch.getReportDate(), batch.getId());
        maturityDate = null;
        prolongationDate = null;
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "change":
                break;
            case "turnover":
                changeTurnoverParser.parse(xmlReader, batch, index, respondentId);
                currentBaseEntity.put("turnover",  new BaseValue(changeTurnoverParser.getCurrentBaseEntity()));
                break;
            case "remains":
                changeRemainsParser.parse(xmlReader, batch, index, respondentId);
                currentBaseEntity.put("remains", new BaseValue(changeRemainsParser.getCurrentBaseEntity()));
                break;
            case "credit_flow":
                changeCreditFlowParser.parse(xmlReader, batch, index, respondentId);
                currentBaseEntity.put("credit_flow", new BaseValue(changeCreditFlowParser.getCurrentBaseEntity()));
                break;
            case "maturity_date": {
                event = (XMLEvent) xmlReader.next();
                String dateRaw = trim(event.asCharacters().getData());
                try {
                    maturityDate = new BaseValue(LocalDate.parse(dateRaw));
                } catch (DateTimeParseException e) {
                    currentBaseEntity.addValidationError("Неправильная дата: " + dateRaw);
                }
                break;
            }
            case "prolongation_date": {
                event = (XMLEvent) xmlReader.next();
                String dateRaw = trim(event.asCharacters().getData());
                try {
                    prolongationDate = new BaseValue(LocalDate.parse(dateRaw));
                } catch (DateTimeParseException e) {
                    currentBaseEntity.addValidationError("Неправильная дата: " + dateRaw);
                }
                break;
            }
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "change":
                return true;
            case "turnover":
                break;
            case "remains":
                for (String e : changeRemainsParser.getCurrentBaseEntity().getValidationErrors())
                    getCurrentBaseEntity().addValidationError(e);
                changeRemainsParser.getCurrentBaseEntity().clearValidationErrors();
                break;
            case "credit_flow":
                break;
            case "maturity_date":
                break;
            case "prolongation_date":
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    BaseValue getMaturityDate() {
        return maturityDate;
    }

    BaseValue getProlongationDate() {
        return prolongationDate;
    }
}
