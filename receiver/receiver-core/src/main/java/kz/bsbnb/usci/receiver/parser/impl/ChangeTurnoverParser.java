package kz.bsbnb.usci.receiver.parser.impl;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseValue;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.receiver.parser.BatchParser;
import kz.bsbnb.usci.receiver.parser.exceptions.UnknownTagException;
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
public class ChangeTurnoverParser extends BatchParser {
    public ChangeTurnoverParser() {
        super();
    }

    private BaseEntity currentIssue;
    private BaseEntity currentInterest;
    boolean interestFlag = false;
    private BaseEntity currentDebt;
    boolean debtFlag = false;

    private MetaClass refTurnOverIssueMeta, refTurnOverIssueDebtMeta, refTurnOverissueInterestMeta;

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("turnover"), respondentId, batch.getReportDate(), batch.getId());

        currentIssue = null;
        currentInterest = null;
        interestFlag = false;
        currentDebt = null;
        debtFlag = false;

        refTurnOverIssueMeta = metaClassRepository.getMetaClass("turnover_issue");
        refTurnOverIssueDebtMeta = metaClassRepository.getMetaClass("turnover_issue_debt");
        refTurnOverissueInterestMeta = metaClassRepository.getMetaClass("turnover_issue_interest");
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "turnover":
                break;
            case "issue":
                currentIssue = new BaseEntity(refTurnOverIssueMeta, respondentId, batch.getReportDate(), batch.getId());
                break;
            case "debt":
                currentDebt = new BaseEntity(refTurnOverIssueDebtMeta, respondentId, batch.getReportDate(), batch.getId());
                debtFlag = true;
                break;
            case "interest":
                currentInterest = new BaseEntity(refTurnOverissueInterestMeta, respondentId, batch.getReportDate(), batch.getId());
                interestFlag = true;
                break;
            case "amount":
                if (interestFlag) {
                    event = (XMLEvent) xmlReader.next();
                    currentInterest.put("amount", new BaseValue(new Double(trim(event.asCharacters().getData()))));
                } else if (debtFlag) {
                    event = (XMLEvent) xmlReader.next();
                    currentDebt.put("amount",  new BaseValue(new Double(trim(event.asCharacters().getData()))));
                }
                break;
            case "amount_currency":
                if (interestFlag) {
                    event = (XMLEvent) xmlReader.next();
                    currentInterest.put("amount_currency", new BaseValue(new Double(trim(event.asCharacters().getData()))));
                } else if (debtFlag) {
                    event = (XMLEvent) xmlReader.next();
                    currentDebt.put("amount_currency", new BaseValue(new Double(trim(event.asCharacters().getData()))));
                }
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "turnover":
                return true;
            case "issue":
                currentBaseEntity.put("issue", new BaseValue(currentIssue));
                break;
            case "debt":
                debtFlag = false;
                currentIssue.put("debt", new BaseValue(currentDebt));
                break;
            case "interest":
                interestFlag = false;
                currentIssue.put("interest", new BaseValue(currentInterest));
                break;
            case "amount":
                break;
            case "amount_currency":
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }
}
