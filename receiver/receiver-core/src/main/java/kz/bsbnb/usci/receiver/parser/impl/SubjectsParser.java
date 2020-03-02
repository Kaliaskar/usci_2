package kz.bsbnb.usci.receiver.parser.impl;

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
public class SubjectsParser extends BatchParser {
    @Autowired
    private SubjectPersonParser subjectPersonParser;

    @Autowired
    private SubjectOrganizationParser subjectOrganizationParser;

    @Autowired
    private SubjectCreditorParser subjectCreditorParser;

    public SubjectsParser() {
        super();
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "subjects":
                break;
            case "subject":
                break;
            case "person":
                subjectPersonParser.parse(xmlReader, batch, index, respondentId);
                currentBaseEntity = subjectPersonParser.getCurrentBaseEntity();
                break;
            case "organization":
                subjectOrganizationParser.parse(xmlReader, batch, index, respondentId);
                currentBaseEntity = subjectOrganizationParser.getCurrentBaseEntity();
                break;
            case "creditor":
                subjectCreditorParser.parse(xmlReader, batch, index, respondentId);
                currentBaseEntity = subjectCreditorParser.getCurrentBaseEntity();
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "subjects":
                hasMore = false;
                break;
            case "subject":
                hasMore = true;
                break;
            default:
                throw new UnknownTagException(localName);
        }
        return true;
    }    
}
