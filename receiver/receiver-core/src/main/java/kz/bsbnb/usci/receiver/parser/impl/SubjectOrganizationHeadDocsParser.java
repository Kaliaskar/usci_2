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
public class SubjectOrganizationHeadDocsParser extends BatchParser {
    public SubjectOrganizationHeadDocsParser() {
        super();
    }

    private MetaClass refDocTypeMeta;

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("document"), respondentId, batch.getReportDate(), batch.getId());
        refDocTypeMeta = metaClassRepository.getMetaClass("ref_doc_type");
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "docs":
                break;
            case "doc":
                BaseEntity docType = new BaseEntity(refDocTypeMeta, respondentId, batch.getReportDate(), batch.getId());
                docType.put("code", new BaseValue(event.asStartElement().getAttributeByName(new QName("doc_type")).getValue()));
                currentBaseEntity.put("doc_type",  new BaseValue(docType));
                break;
            case "name":
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("name", new BaseValue(trim(event.asCharacters().getData())));
                break;
            case "no":
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("no", new BaseValue(trim(event.asCharacters().getData())));
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "docs":
                hasMore = false;
                return true;
            case "doc":
                hasMore = true;
                return true;
            case "name":
                break;
            case "no":
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }
}
