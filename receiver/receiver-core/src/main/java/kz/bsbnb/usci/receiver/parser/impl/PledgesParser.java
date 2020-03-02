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
public class PledgesParser extends BatchParser {
    public PledgesParser() {
        super();
    }

    private MetaClass pledgeMeta, refPledgeTypeMeta;

    @Override
    public void init() {
        pledgeMeta = metaClassRepository.getMetaClass("pledge");
        refPledgeTypeMeta = metaClassRepository.getMetaClass("ref_pledge_type");
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "pledges":
                break;
            case "pledge":
                currentBaseEntity = new BaseEntity(pledgeMeta, respondentId, batch.getReportDate(), batch.getId());
                break;
            case "pledge_type":
                event = (XMLEvent) xmlReader.next();
                BaseEntity pledgeType = new BaseEntity(refPledgeTypeMeta, respondentId, batch.getReportDate(), batch.getId());
                pledgeType.put("code", new BaseValue(trim(event.asCharacters().getData())));
                currentBaseEntity.put("pledge_type", new BaseValue(pledgeType));
                break;
            case "contract":
                break;
            case "no":
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("contract", new BaseValue(trim(event.asCharacters().getData())));
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
            case "pledges":
                hasMore = false;
                return true;
            case "pledge":
                hasMore = true;
                return true;
            case "pledge_type":
                break;
            case "contract":
                break;
            case "no":
                break;
            case "value":
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }
}
