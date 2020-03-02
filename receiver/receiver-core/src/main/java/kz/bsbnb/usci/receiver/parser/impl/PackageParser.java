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

import javax.xml.namespace.QName;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * @author Artur Tkachenko
 * @author Kanat Tulbassiev
 */

@Component
@Scope("prototype")
public class PackageParser extends BatchParser {
    @Autowired
    private PrimaryContractParser primaryContractParser;
    @Autowired
    private CreditParser creditParser;
    @Autowired
    private SubjectsParser subjectsParser;
    @Autowired
    private ChangeParser changeParser;
    @Autowired
    private PledgesParser pledgesParser;

    private int totalCount = 0;

    private MetaClass creditMeta, refCreditTypeMeta, pledgeMeta;

    public int getTotalCount() {
        return totalCount;
    }

    public void setCurrentBaseEntity(BaseEntity currentBaseEntity) {
        this.currentBaseEntity = currentBaseEntity;
        creditParser.setCurrentBaseEntity(currentBaseEntity);
    }

    @Override
    public void init() {
        creditMeta = metaClassRepository.getMetaClass("credit");
        refCreditTypeMeta = metaClassRepository.getMetaClass("ref_credit_type");
        pledgeMeta = metaClassRepository.getMetaClass("pledge");
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName)
            throws SAXException {

        switch (localName) {
            case "packages":
                break;
            case "package":
                currentBaseEntity = new BaseEntity(creditMeta, respondentId, batch.getReportDate(), batch.getId());
                creditParser.setCurrentBaseEntity(currentBaseEntity);
                break;
            case "primary_contract":
                primaryContractParser.parse(xmlReader, batch, index, respondentId);
                BaseEntity primaryContract = primaryContractParser.getCurrentBaseEntity();
                currentBaseEntity.put("primary_contract", new BaseValue(primaryContract));

                for (String e : primaryContractParser.getCurrentBaseEntity().getValidationErrors())
                    getCurrentBaseEntity().addValidationError(e);
                primaryContractParser.getCurrentBaseEntity().clearValidationErrors();
                break;
            case "credit":
                creditParser.parse(xmlReader, batch, index, respondentId);
                BaseEntity credit = creditParser.getCurrentBaseEntity();

                if(event.asStartElement().getAttributeByName(new QName("credit_type")) != null) {
                    String creditTypeCode = event.asStartElement().getAttributeByName(new QName("credit_type")).getValue();
                    BaseEntity creditType = new BaseEntity(refCreditTypeMeta, respondentId, batch.getReportDate(), batch.getId());
                    creditType.put("code", new BaseValue(creditTypeCode));
                    credit.put("credit_type", new BaseValue(creditType));
                }

                break;
            case "subjects":
                while (true) {
                    subjectsParser.parse(xmlReader, batch, index, respondentId);
                    if (subjectsParser.hasMore()) {
                        BaseEntity subject = subjectsParser.getCurrentBaseEntity();
                        if (subject != null)
                            currentBaseEntity.put("subject", new BaseValue(subject));
                    } else break;
                }
                break;
            case "pledges":
                BaseSet pledges = new BaseSet(pledgeMeta);
                while (true) {
                    pledgesParser.parse(xmlReader, batch, index, respondentId);
                    if (pledgesParser.hasMore())
                        pledges.put(new BaseValue(pledgesParser.getCurrentBaseEntity()));
                    else break;
                }
                currentBaseEntity.put("pledges", new BaseValue(pledges));
                break;
            case "change":
                changeParser.parse(xmlReader, batch, index, respondentId);
                currentBaseEntity.put("change", new BaseValue(changeParser.getCurrentBaseEntity()));

                for (String e : changeParser.getCurrentBaseEntity().getValidationErrors())
                    getCurrentBaseEntity().addValidationError(e);
                changeParser.getCurrentBaseEntity().clearValidationErrors();

                if (changeParser.getMaturityDate() != null)
                    currentBaseEntity.put("maturity_date", changeParser.getMaturityDate());

                if (changeParser.getProlongationDate() != null)
                    currentBaseEntity.put("prolongation_date", changeParser.getProlongationDate());
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "packages":
                hasMore = false;
                break;
            case "package":
                totalCount++;
                hasMore = true;
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return true;
    }
}
