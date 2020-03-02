package kz.bsbnb.usci.receiver.parser.impl;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseSet;
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
public class SubjectCreditorParser extends BatchParser {
    public SubjectCreditorParser() {
        super();
    }

    private BaseSet docs;
    private BaseEntity currentDoc;
    private BaseEntity creditorInfo;

    private MetaClass documentMeta, refDocTypeMeta;

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("subject"), respondentId, batch.getReportDate(), batch.getId());
        creditorInfo = new BaseEntity(metaClassRepository.getMetaClass("creditor_info"), respondentId, batch.getReportDate(), batch.getId());
        docs = null;
        currentDoc = null;

        documentMeta = metaClassRepository.getMetaClass("document");
        refDocTypeMeta = metaClassRepository.getMetaClass("ref_doc_type");
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "creditor":
                break;
            case "code":
                event = (XMLEvent) xmlReader.next();
                creditorInfo.put("code", new BaseValue(trim(event.asCharacters().getData())));
                break;
            case "docs":
                docs = new BaseSet(documentMeta);
                break;
            case "doc":
                currentDoc = new BaseEntity(documentMeta, respondentId, batch.getReportDate(), batch.getId());
                BaseEntity docType = new BaseEntity(refDocTypeMeta, respondentId, batch.getReportDate(), batch.getId());
                docType.put("code",new BaseValue(event.asStartElement().getAttributeByName(new QName("ref_doc_type")).getValue()));
                currentDoc.put("ref_doc_type", new BaseValue(docType));
                break;
            case "name":
                event = (XMLEvent) xmlReader.next();
                currentDoc.put("name", new BaseValue(trim(event.asCharacters().getData())));
                break;
            case "no":
                event = (XMLEvent) xmlReader.next();
                currentDoc.put("no", new BaseValue(trim(event.asCharacters().getData())));
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "creditor":
                currentBaseEntity.put("creditor_info", new BaseValue(creditorInfo));
                currentBaseEntity.put("is_person", new BaseValue(false));
                currentBaseEntity.put("is_organization", new BaseValue(false));
                currentBaseEntity.put("is_creditor", new BaseValue(true));
                return true;
            case "code":
                break;
            case "docs":
                currentBaseEntity.put("docs", new BaseValue(docs));
                break;
            case "doc":
                docs.put(new BaseValue(currentDoc));
                break;
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
