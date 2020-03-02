package kz.bsbnb.usci.receiver.parser.impl;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseValue;
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
public class ChangeRemainsParser extends BatchParser {
    @Autowired
    private ChangeRemainsDebtParser changeRemainsDebtParser;

    @Autowired
    private ChangeRemainsInterestParser changeRemainsInterestParser;

    @Autowired
    private ChangeRemainsDiscountParser changeRemainsDiscountParser;

    @Autowired
    private ChangeRemainsCorrectionParser changeRemainsCorrectionParser;

    @Autowired
    private ChangeRemainsLimitParser changeRemainsLimitParser;

    @Autowired
    private ChangeRemainsDiscountedValueParser changeRemainsDiscountedValueParser;

    public ChangeRemainsParser() {
        super();
    }

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("remains"), respondentId, batch.getReportDate(), batch.getId());
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "remains":
                break;
            case "debt":
                changeRemainsDebtParser.parse(xmlReader, batch, index, respondentId);
                currentBaseEntity.put("debt", new BaseValue(changeRemainsDebtParser.getCurrentBaseEntity()));
                break;
            case "interest":
                changeRemainsInterestParser.parse(xmlReader, batch, index, respondentId);
                currentBaseEntity.put("interest", new BaseValue(changeRemainsInterestParser.getCurrentBaseEntity()));
                break;
            case "discount":
                changeRemainsDiscountParser.parse(xmlReader, batch, index, respondentId);
                currentBaseEntity.put("discount", new BaseValue(changeRemainsDiscountParser.getCurrentBaseEntity()));
                break;
            case "correction":
                changeRemainsCorrectionParser.parse(xmlReader, batch, index, respondentId);
                currentBaseEntity.put("correction", new BaseValue(changeRemainsCorrectionParser.getCurrentBaseEntity()));
                break;
            case "discounted_value":
                changeRemainsDiscountedValueParser.parse(xmlReader, batch, index, respondentId);

                currentBaseEntity.put("discounted_value",
                        new BaseValue(changeRemainsDiscountedValueParser.getCurrentBaseEntity()));
                break;
            case "limit":
                changeRemainsLimitParser.parse(xmlReader, batch, index, respondentId);
                currentBaseEntity.put("limit",  new BaseValue(changeRemainsLimitParser.getCurrentBaseEntity()));
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "remains":
                return true;
            case "debt":
                for (String e : changeRemainsDebtParser.getCurrentBaseEntity().getValidationErrors())
                    getCurrentBaseEntity().addValidationError(e);
                changeRemainsDebtParser.getCurrentBaseEntity().clearValidationErrors();
                break;
            case "interest":
                for (String e : changeRemainsInterestParser.getCurrentBaseEntity().getValidationErrors())
                    getCurrentBaseEntity().addValidationError(e);
                changeRemainsInterestParser.getCurrentBaseEntity().clearValidationErrors();
                break;
            case "discount":
                break;
            case "correction":
                break;
            case "discounted_value":
                break;
            case "value":
                break;
            case "limit":
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }
}
