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
public class ChangeCreditFlowParser extends BatchParser {
    public ChangeCreditFlowParser() {
        super();
    }

    private BaseEntity currentProvisionGroup;
    private BaseEntity currentProvisionKfn;
    private BaseEntity currentProvisionMsfo;
    private BaseEntity currentProvisionMsfoOverB;

    private MetaClass refClassificationMeta, refBalanceAccountMeta, refProvisionGroupMeta, refProvisionMeta;

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("credit_flow"), respondentId, batch.getReportDate(), batch.getId());
        currentProvisionGroup = null;
        currentProvisionKfn = null;
        currentProvisionMsfo = null;
        currentProvisionMsfoOverB = null;

        refClassificationMeta = metaClassRepository.getMetaClass("ref_classification");
        refBalanceAccountMeta = metaClassRepository.getMetaClass("ref_balance_account");
        refProvisionGroupMeta = metaClassRepository.getMetaClass("provision_group");
        refProvisionMeta = metaClassRepository.getMetaClass("provision");
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName)
            throws SAXException {
        switch (localName) {
            case "credit_flow":
                break;
            case "classification":
                BaseEntity classification = new BaseEntity(refClassificationMeta, respondentId, batch.getReportDate(), batch.getId());

                event = (XMLEvent) xmlReader.next();

                classification.put("code", new BaseValue(trim(event.asCharacters().getData())));
                currentBaseEntity.put("classification", new BaseValue(classification));
                break;
            case "provision":
                currentProvisionGroup = new BaseEntity(refProvisionGroupMeta, respondentId, batch.getReportDate(), batch.getId());

                currentProvisionKfn = null;
                currentProvisionMsfo = null;
                currentProvisionMsfoOverB = null;
                break;
            case "value":
                event = (XMLEvent) xmlReader.next();
                getCurrentProvisionKfn().put("value", new BaseValue(new Double(trim(event.asCharacters().getData()))));
                break;
            case "balance_account": {
                event = (XMLEvent) xmlReader.next();
                BaseEntity balanceAccount = new BaseEntity(refBalanceAccountMeta, respondentId, batch.getReportDate(), batch.getId());

                balanceAccount.put("no_", new BaseValue( trim(event.asCharacters().getData())));
                getCurrentProvisionKfn().put("balance_account", new BaseValue(balanceAccount));
                break;
            }
            case "value_msfo":
                event = (XMLEvent) xmlReader.next();
                getCurrentProvisionMsfo().put("value",new BaseValue(new Double(trim(event.asCharacters().getData()))));
                break;
            case "balance_account_msfo": {
                event = (XMLEvent) xmlReader.next();
                BaseEntity balanceAccount = new BaseEntity(refBalanceAccountMeta, respondentId, batch.getReportDate(), batch.getId());
                balanceAccount.put("no_", new BaseValue(trim(event.asCharacters().getData())));
                getCurrentProvisionMsfo().put("balance_account", new BaseValue(balanceAccount));
                break;
            }
            case "value_msfo_over_balance":
                event = (XMLEvent) xmlReader.next();
                getCurrentProvisionMsfoOverB().put("value", new BaseValue(new Double(trim(event.asCharacters().getData()))));
                break;
            case "balance_account_msfo_over_balance": {
                event = (XMLEvent) xmlReader.next();
                BaseEntity balanceAccount = new BaseEntity(refBalanceAccountMeta, respondentId, batch.getReportDate(), batch.getId());
                balanceAccount.put("no_", new BaseValue(trim(event.asCharacters().getData())));
                getCurrentProvisionMsfoOverB().put("balance_account", new BaseValue(balanceAccount));
                break;
            }
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "credit_flow":
                return true;
            case "classification":
                break;
            case "provision":
                if (currentProvisionKfn != null)
                    currentProvisionGroup.put("provision_kfn",new BaseValue(currentProvisionKfn));

                if (currentProvisionMsfo != null)
                    currentProvisionGroup.put("provision_msfo", new BaseValue(currentProvisionMsfo));

                if (currentProvisionMsfoOverB != null)
                    currentProvisionGroup.put("provision_msfo_over_balance", new BaseValue(currentProvisionMsfoOverB));

                currentBaseEntity.put("provision", new BaseValue(currentProvisionGroup));
                break;
            case "balance_account":
                break;
            case "balance_account_msfo":
                break;
            case "balance_account_msfo_over_balance":
                break;
            case "value":
                break;
            case "value_msfo":
                break;
            case "value_msfo_over_balance":
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    private BaseEntity getCurrentProvisionKfn() {
        if (currentProvisionKfn == null)
            currentProvisionKfn = new BaseEntity(refProvisionMeta, respondentId, batch.getReportDate(), batch.getId());
        return currentProvisionKfn;
    }

    private BaseEntity getCurrentProvisionMsfo() {
        if (currentProvisionMsfo == null)
            currentProvisionMsfo = new BaseEntity(refProvisionMeta, respondentId, batch.getReportDate(), batch.getId());
        return currentProvisionMsfo;
    }

    private BaseEntity getCurrentProvisionMsfoOverB() {
        if (currentProvisionMsfoOverB == null)
            currentProvisionMsfoOverB = new BaseEntity(refProvisionMeta, respondentId, batch.getReportDate(), batch.getId());
        return currentProvisionMsfoOverB;
    }

}
