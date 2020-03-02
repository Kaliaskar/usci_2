package kz.bsbnb.usci.receiver.parser.impl;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseValue;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.receiver.parser.BatchParser;
import kz.bsbnb.usci.receiver.parser.exceptions.UnknownTagException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * @author Artur Tkachenko
 * @author Kanat Tulbassiev
 */

@Component
@Scope("prototype")
public class SubjectOrganizationNamesParser extends BatchParser {
    private MetaClass organizationNameMeta;

    public SubjectOrganizationNamesParser() {
        super();
    }

    @Override
    public void init() {
        organizationNameMeta = metaClassRepository.getMetaClass("organization_name");
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "names":
                break;
            case "name":
                currentBaseEntity = new BaseEntity(organizationNameMeta, respondentId, batch.getReportDate(), batch.getId());

                currentBaseEntity.put("lang", new BaseValue(event.asStartElement().getAttributeByName(new QName("lang")).getValue()));

                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("name", new BaseValue(trim(event.asCharacters().getData())));
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "names":
                hasMore = false;
                return true;
            case "name":
                hasMore = true;
                return true;
            default:
                throw new UnknownTagException(localName);
        }
    }
}
