package kz.bsbnb.usci.receiver.parser.impl;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseSet;
import kz.bsbnb.usci.eav.model.base.BaseValue;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.receiver.parser.BatchParser;
import kz.bsbnb.usci.receiver.parser.exceptions.UnknownTagException;
import org.springframework.beans.factory.annotation.Autowired;
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
public class SubjectOrganizationHeadParser extends BatchParser {
    @Autowired
    private SubjectOrganizationHeadNamesParser subjectOrganizationHeadNamesParser;
    @Autowired
    private SubjectOrganizationHeadDocsParser subjectOrganizationHeadDocsParser;

    public SubjectOrganizationHeadParser() {
        super();
    }

    private MetaClass personNameMeta, documentMeta;

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("head"), respondentId, batch.getReportDate(), batch.getId());

        personNameMeta = metaClassRepository.getMetaClass("person_name");
        documentMeta = metaClassRepository.getMetaClass("document");
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName)
            throws SAXException {
        switch (localName) {
            case "head":
                break;
            case "names":
                BaseSet headNames = new BaseSet(personNameMeta);
                while (true) {
                    subjectOrganizationHeadNamesParser.parse(xmlReader, batch, index, respondentId);
                    if (subjectOrganizationHeadNamesParser.hasMore()) {
                        headNames.put(new BaseValue(subjectOrganizationHeadNamesParser.getCurrentBaseEntity()));
                    } else break;
                }

                currentBaseEntity.put("names",
                        new BaseValue(headNames));
                break;
            case "docs":
                BaseSet docs = new BaseSet(documentMeta);
                while (true) {
                    subjectOrganizationHeadDocsParser.parse(xmlReader, batch, index, respondentId);

                    if (subjectOrganizationHeadDocsParser.hasMore()) {
                        docs.put(new BaseValue(subjectOrganizationHeadDocsParser.getCurrentBaseEntity()));
                    } else break;
                }

                currentBaseEntity.put("docs", new BaseValue(docs));
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "head":
                return true;
            case "names":
                break;
            case "docs":
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }
}
