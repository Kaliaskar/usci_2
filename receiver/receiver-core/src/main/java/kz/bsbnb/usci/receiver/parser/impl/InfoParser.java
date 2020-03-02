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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * @author Artur Tkachenko
 * @author Kanat Tulbassiev
 */

@Component
@Scope("prototype")
public class InfoParser extends BatchParser {

    public InfoParser() {
        super();
    }

    private BaseSet docs;
    private BaseEntity currentDoc;
    private LocalDate reportDate;

    private MetaClass refCreditorMeta, documentMeta, refDocTypeMeta;

    @Override
    public void init() {
        refCreditorMeta = metaClassRepository.getMetaClass("ref_creditor");
        documentMeta = metaClassRepository.getMetaClass("document");
        refDocTypeMeta = metaClassRepository.getMetaClass("ref_doc_type");
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "info":
                break;
            case "creditor":
                currentBaseEntity = new BaseEntity(refCreditorMeta, respondentId, batch.getReportDate(), batch.getId());
                break;
            case "code":
                event = (XMLEvent) xmlReader.next();
                String crCode = trim(event.asCharacters().getData());
                currentBaseEntity.put("code", new BaseValue(crCode));
                break;
            case "docs":
                docs = new BaseSet(documentMeta);
                break;
            case "doc":
                currentDoc = new BaseEntity(documentMeta, respondentId, batch.getReportDate(), batch.getId());
                BaseEntity docType = new BaseEntity(refDocTypeMeta, respondentId, batch.getReportDate(), batch.getId());
                docType.put("code", new BaseValue(event.asStartElement().getAttributeByName(new QName("doc_type")).getValue()));
                currentDoc.put("doc_type", new BaseValue(docType));
                break;
            case "name":
                event = (XMLEvent) xmlReader.next();
                currentDoc.put("name", new BaseValue(trim(event.asCharacters().getData())));
                break;
            case "no":
                event = (XMLEvent) xmlReader.next();
                currentDoc.put("no", new BaseValue(trim(event.asCharacters().getData())));
                break;
            case "account_date":
                break;
            case "report_date":
                event = (XMLEvent) xmlReader.next();
                String dateRaw = trim(event.asCharacters().getData());
                try {
                    reportDate = LocalDate.parse(dateRaw);
                } catch (DateTimeParseException e) {
                   currentBaseEntity.addValidationError("Неправильная дата: " + dateRaw);
                }
                break;
            case "actual_credit_count":
                break;
            case  "maintenance":
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "info":
                return true;
            case "creditor":
                break;
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
            case "account_date":
                break;
            case "report_date":
                break;
            case "actual_credit_count":
                break;
            case "maintenance":
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }
}

