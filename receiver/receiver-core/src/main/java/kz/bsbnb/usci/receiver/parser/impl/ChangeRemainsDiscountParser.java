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

/**
 * @author Artur Tkachenko
 * @author Kanat Tulbassiev
 */

@Component
@Scope("prototype")
public class ChangeRemainsDiscountParser extends BatchParser {
    public ChangeRemainsDiscountParser() {
        super();
    }
    private MetaClass refBalanceAccountMeta;

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("remains_discount"), respondentId, batch.getReportDate(),  batch.getId());
        refBalanceAccountMeta = metaClassRepository.getMetaClass("ref_balance_account");
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "discount":
                break;
            case "value":
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("value",
                        new BaseValue(new Double(trim(event.asCharacters().getData()))));
                break;
            case "value_currency":
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("value_currency",
                        new BaseValue(new Double(trim(event.asCharacters().getData()))));
                break;
            case "balance_account":
                event = (XMLEvent) xmlReader.next();
                BaseEntity balanceAccount = new BaseEntity(refBalanceAccountMeta, respondentId, batch.getReportDate(), batch.getId());
                balanceAccount.put("no_", new BaseValue(trim(event.asCharacters().getData())));
                currentBaseEntity.put("balance_account", new BaseValue(balanceAccount));
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "discount":
                return true;
            case "value":
                break;
            case "value_currency":
                break;
            case "balance_account":
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }
}
