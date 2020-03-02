package kz.bsbnb.usci.receiver.parser;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.repository.MetaClassRepository;
import kz.bsbnb.usci.receiver.model.Batch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * @author Artur Tkachenko
 * @author Kanat Tulbassiev
 */

public abstract class BatchParser {
    protected XMLEventReader xmlReader;

    private Logger logger = LoggerFactory.getLogger(BatchParser.class);

    protected Batch batch;
    protected BaseEntity currentBaseEntity = null;
    protected boolean hasMore = false;
    protected long index;
    protected long respondentId;

    @Autowired
    protected MetaClassRepository metaClassRepository;

    public BatchParser() {
        super();
    }

    public void parse(XMLEventReader xmlReader, Batch batch, long index, long respondentId) throws SAXException {
        this.batch = batch;
        this.xmlReader = xmlReader;
        this.index = index;
        this.respondentId = respondentId;

        init();

        while (xmlReader.hasNext()) {
            XMLEvent event = (XMLEvent) xmlReader.next();

            if (event.isStartDocument()) {
                logger.info("Начинаем парсить xml файл batchId = {}", batch.getId());
            } else if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                String localName = startElement.getName().getLocalPart();

                if (startElement(event, startElement, localName)) break;
            } else if (event.isEndElement()) {
                EndElement endElement = event.asEndElement();
                String localName = endElement.getName().getLocalPart();

                if (endElement(localName)) break;
            } else if (event.isEndDocument()) {
                logger.info("Завершаем парсить xml файл");
            }
        }
    }

    public abstract boolean startElement(XMLEvent event, StartElement startElement, String localName)
            throws SAXException;

    public abstract boolean endElement(String localName) throws SAXException;
    public void init() {}

    public BaseEntity getCurrentBaseEntity() {
        return currentBaseEntity;
    }

    public boolean hasMore() {
        return hasMore;
    }

    public long getIndex() {
        return index;
    }

    public String trim(String data) {
        return data.trim();
    }

    public void setRespondentId(long respondentId) {
        this.respondentId = respondentId;
    }

}
