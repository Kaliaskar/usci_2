package kz.bsbnb.usci.receiver.parser.impl;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseValue;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
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
public class ChangeRemainsInterestParser extends BatchParser {
    private String interestWay;

    public ChangeRemainsInterestParser() {
        super();
    }

    private BaseEntity fieldCurrent;
    private BaseEntity fieldPastDue;
    private BaseEntity fieldWriteOf;

    private MetaClass refRemainsInterestCurrectMeta, refRemainsInterestPastdueMeta, refRemainsInterestWriteOffMeta,
            refBalanceAccountMeta;

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("remains_interest"),
                respondentId, batch.getReportDate(), batch.getId());
        fieldCurrent = null;
        fieldPastDue = null;
        fieldWriteOf = null;

        refRemainsInterestCurrectMeta = metaClassRepository.getMetaClass("remains_interest_current");
        refRemainsInterestPastdueMeta = metaClassRepository.getMetaClass("remains_interest_pastdue");
        refRemainsInterestWriteOffMeta = metaClassRepository.getMetaClass("remains_interest_write_off");
        refBalanceAccountMeta = metaClassRepository.getMetaClass("ref_balance_account");
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "interest":
                break;
            case "current":
                fieldCurrent = new BaseEntity(refRemainsInterestCurrectMeta, respondentId, batch.getReportDate(), batch.getId());
                interestWay = localName;
                break;
            case "pastdue":
                fieldPastDue = new BaseEntity(refRemainsInterestPastdueMeta, respondentId, batch.getReportDate(), batch.getId());
                interestWay = localName;
                break;
            case "write_off":
                fieldWriteOf = new BaseEntity(refRemainsInterestWriteOffMeta, respondentId, batch.getReportDate(), batch.getId());
                interestWay = localName;
                break;
            case "value": {
                event = (XMLEvent) xmlReader.next();

                BaseValue baseValue = new BaseValue(new Double(trim(event.asCharacters().getData())));

                switch (interestWay) {
                    case "current":
                        fieldCurrent.put("value", baseValue);
                        break;
                    case "pastdue":
                        fieldPastDue.put("value", baseValue);
                        break;
                    case "write_off":
                        fieldWriteOf.put("value", baseValue);
                        break;
                }
                break;
            }
            case "value_currency": {
                event = (XMLEvent) xmlReader.next();

                BaseValue baseValue = new BaseValue( new Double(trim(event.asCharacters().getData())));

                switch (interestWay) {
                    case "current":
                        fieldCurrent.put("value_currency", baseValue);
                        break;
                    case "pastdue":
                        fieldPastDue.put("value_currency", baseValue);
                        break;
                    case "write_off":
                        fieldWriteOf.put("value_currency", baseValue);
                        break;
                }
                break;
            }
            case "balance_account": {
                event = (XMLEvent) xmlReader.next();
                BaseEntity baseEntity = new BaseEntity(refBalanceAccountMeta, respondentId, batch.getReportDate(), batch.getId());
                baseEntity.put("no_", new BaseValue(trim(event.asCharacters().getData())));
                BaseValue baseValue = new BaseValue(baseEntity);

                switch (interestWay) {
                    case "current":
                        fieldCurrent.put("balance_account", baseValue);
                        break;
                    case "pastdue":
                        fieldPastDue.put("balance_account", baseValue);
                        break;
                    case "write_off":
                        fieldWriteOf.put("balance_account", baseValue);
                        break;
                }
                break;
            }
            case "open_date": {
                event = (XMLEvent) xmlReader.next();
                String dateRaw = trim(event.asCharacters().getData());
                try {
                    fieldPastDue.put("open_date", new BaseValue(LocalDate.parse(dateRaw)));
                } catch (DateTimeParseException e) {
                    getCurrentBaseEntity().addValidationError("Неправильная дата: " + dateRaw);
                }
                break;
            }
            case "close_date": {
                event = (XMLEvent) xmlReader.next();
                String dateRaw = trim(event.asCharacters().getData());
                try {
                    fieldPastDue.put("close_date",  new BaseValue(LocalDate.parse(dateRaw)));
                } catch (DateTimeParseException e) {
                    getCurrentBaseEntity().addValidationError("Неправильная дата: " + dateRaw);
                }
                break;
            }
            case "date": {
                event = (XMLEvent) xmlReader.next();
                String dateRaw = trim(event.asCharacters().getData());
                try {
                    fieldWriteOf.put("date", new BaseValue(LocalDate.parse(dateRaw)));
                } catch (DateTimeParseException e) {
                    getCurrentBaseEntity().addValidationError("Неправильная дата: " + dateRaw);
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
            case "interest":
                return true;
            case "current":
                currentBaseEntity.put("current", new BaseValue(fieldCurrent));
                break;
            case "pastdue":
                currentBaseEntity.put("pastdue", new BaseValue(fieldPastDue));
                break;
            case "write_off":
                currentBaseEntity.put("write_off", new BaseValue(fieldWriteOf));
                break;
            case "value":
                switch (interestWay) {
                    case "current":
                        break;
                    case "pastdue":
                        break;
                    case "write_off":
                        break;
                    default:
                        break;
                }
                break;
            case "value_currency":
                switch (interestWay) {
                    case "current":
                        break;
                    case "pastdue":
                        break;
                    case "write_off":
                        break;
                    default:
                        break;
                }
                break;
            case "balance_account":
                switch (interestWay) {
                    case "current":
                        break;
                    case "pastdue":
                        break;
                    default:
                        break;
                }
                break;
            case "open_date":
                break;
            case "close_date":
                break;
            case "date":
                break;
            default:
                break;
        }

        return false;
    }
}
