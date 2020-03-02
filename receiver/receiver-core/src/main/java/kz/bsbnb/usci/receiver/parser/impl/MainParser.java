package kz.bsbnb.usci.receiver.parser.impl;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseValue;
import kz.bsbnb.usci.eav.model.base.OperType;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.receiver.model.Batch;
import kz.bsbnb.usci.receiver.parser.BatchParser;
import kz.bsbnb.usci.receiver.parser.exceptions.UnknownTagException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Artur Tkachenko
 * @author Kanat Tulbassiev
 */

@Component
@Scope("prototype")
public class MainParser extends BatchParser {
    private final InfoParser infoParser;
    private final PackageParser packageParser;
    private final PortfolioDataParser portfolioDataParser;

    private MetaClass creditMeta;

    public  MainParser(InfoParser infoParser, PackageParser packageParser, PortfolioDataParser portfolioDataParser) {
        this.infoParser = infoParser;
        this.packageParser = packageParser;
        this.portfolioDataParser = portfolioDataParser;
    }

    @Override
    public void init() {
        creditMeta = metaClassRepository.getMetaClass("credit");
    }

    public void parse(InputStream in, Batch batch) throws SAXException, IOException, XMLStreamException {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        parse(inputFactory.createXMLEventReader(in), batch, 1L, respondentId);
    }

    public void parseNextPackage() throws SAXException {
        long currentIndex = index++;

        packageParser.parse(xmlReader, batch, currentIndex, respondentId);

        if (packageParser.hasMore()) {
            currentBaseEntity = packageParser.getCurrentBaseEntity();
            BaseEntity creditor = infoParser.getCurrentBaseEntity();
            currentBaseEntity.put("creditor", new BaseValue(creditor));

            for (String s : creditor.getValidationErrors()) {
                currentBaseEntity.addValidationError(s);
            }
        } else {
            parse(xmlReader, batch, index = 1L, respondentId);
        }
    }

    public void skipNextPackage() throws SAXException {
        while (xmlReader.hasNext()) {
            XMLEvent event = (XMLEvent) xmlReader.next();
            currentBaseEntity = null;

            if (event.isEndElement()) {
                EndElement endElement = event.asEndElement();
                String localName = endElement.getName().getLocalPart();

                if (localName.equals("packages")) {
                    hasMore = false;
                    return;
                } else if (localName.equals("package")) {
                    hasMore = true;
                    return;
                }
            }
        }
    }

    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "batch":
                break;
            case "info":
                infoParser.parse(xmlReader, batch, index, respondentId);
                break;
            case "packages":
                break;
            case "package":
                BaseEntity pkg = new BaseEntity(creditMeta, respondentId, batch.getReportDate(), batch.getId());
                String strOperationType = event.asStartElement().getAttributeByName( new QName("operation_type")).getValue();

                switch (strOperationType) {
                    case "insert":
                        pkg.setOperation(OperType.INSERT);
                        break;
                    case "update":
                        pkg.setOperation(OperType.UPDATE);
                        break;
                    case "delete":
                        pkg.setOperation(OperType.DELETE);
                        break;
                    default:
                        throw new UsciException(String.format("Операция не поддерживается %s", strOperationType));
                }

                packageParser.setCurrentBaseEntity(pkg);
                hasMore = true;
                parseNextPackage();
                return true;
            case "portfolio_data":
                hasMore = true;
                portfolioDataParser.parse(xmlReader, batch, index, respondentId);
                currentBaseEntity = portfolioDataParser.getCurrentBaseEntity();
                currentBaseEntity.put("creditor", new BaseValue(infoParser.getCurrentBaseEntity()));
                return true;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "batch":
                hasMore = false;
                return true;
            case "info":
                break;
            case "packages":
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    public int getPackageCount() {
        return packageParser.getTotalCount();
    }
}
