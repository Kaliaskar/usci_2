package kz.bsbnb.usci.receiver.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Общий парсер для манифеста и тега инфо чтобы получить информацию по батчу
 * @author Dauletkhan Tulendiev
 */

public abstract class MainReader {
    private final Logger logger = LoggerFactory.getLogger(MainReader.class);

    protected XMLEventReader xmlReader;
    public final StringBuilder data = new StringBuilder();

    public void parse(XMLEventReader xmlReader) {
        this.xmlReader = xmlReader;

        while (xmlReader.hasNext()) {
            XMLEvent event = (XMLEvent) xmlReader.next();

            if (event.isStartDocument()) {
                logger.debug("start document");
            } else if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                String localName = startElement.getName().getLocalPart();

                startElement(event, startElement, localName);
            } else if (event.isEndElement()) {
                EndElement endElement = event.asEndElement();
                String localName = endElement.getName().getLocalPart();

                if (endElement(localName)) break;
            } else if (event.isCharacters()) {
                data.append(event.asCharacters().getData().trim());
            } else if (event.isEndDocument()) {
                logger.debug("end document");
                data.append(event.asCharacters().getData().trim());
            } else {
                logger.debug(event.toString());
            }
        }
    }

    public abstract void startElement(XMLEvent event, StartElement startElement, String localName);

    public abstract boolean endElement(String localName);

}
