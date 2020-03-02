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
public class CreditorBranchParser extends BatchParser {
    private BaseEntity currentDoc = null;
    private BaseSet currentDocSet = null;
    private MetaClass documentMeta, refDocTypeMeta;

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("ref_creditor_branch"), respondentId, batch.getReportDate(), batch.getId());
        currentDoc = null;
        currentDocSet = null;
        documentMeta = metaClassRepository.getMetaClass("document");
        refDocTypeMeta = metaClassRepository.getMetaClass("ref_doc_type");
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "creditor_branch":
                break;
            case "code":
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("code",
                        new BaseValue(trim(event.asCharacters().getData())));
                break;
            case "docs":
                currentDocSet = new BaseSet(documentMeta);
                break;
            case "doc":
                currentDoc = new BaseEntity(documentMeta, respondentId, batch.getReportDate(), batch.getId());

                BaseEntity docType = new BaseEntity(refDocTypeMeta, respondentId, batch.getReportDate(), batch.getId());
                docType.put("code", new BaseValue(event.asStartElement().getAttributeByName(new QName("ref_doc_type")).getValue()));
                currentDoc.put("ref_doc_type", new BaseValue(docType));
                break;
            case "name":
                event = (XMLEvent) xmlReader.next();
                currentDoc.put("name", new BaseValue(trim(event.asCharacters().getData())));
                break;
            case "no":
                event = (XMLEvent) xmlReader.next();
                currentDoc.put("no",
                        new BaseValue(trim(event.asCharacters().getData())));
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "creditor_branch":
                return true;
            case "code":
                break;
            case "docs":
                currentBaseEntity.put("docs", new BaseValue(currentDocSet));
                break;
            case "doc":
                currentDocSet.put(new BaseValue(currentDoc));
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

