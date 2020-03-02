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

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Artur Tkachenko
 * @author Kanat Tulbassiev
 */

@Component
@Scope("prototype")
public class PortfolioFlowMsfoParser extends BatchParser {
    public PortfolioFlowMsfoParser() {
        super();
    }

    private BaseSet currentDetails;
    private BaseEntity currentDetail;

    private MetaClass refPortfolioMeta, portfolioFlowDetailMeta, refBalanceAccountMeta;

    private Map<String, BaseEntity> balanceAccountMap;

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("portfolio_flow_msfo"), respondentId, batch.getReportDate(), batch.getId());

        refPortfolioMeta = metaClassRepository.getMetaClass("ref_portfolio");
        portfolioFlowDetailMeta = metaClassRepository.getMetaClass("portfolio_flow_detail");
        refBalanceAccountMeta = metaClassRepository.getMetaClass("ref_balance_account");
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "portfolio_flow_msfo":
                break;
            case "portfolio":
                BaseEntity portfolio = new BaseEntity(refPortfolioMeta, respondentId, batch.getReportDate(), batch.getId());
                event = (XMLEvent) xmlReader.next();
                portfolio.put("code", new BaseValue(trim(event.asCharacters().getData())));

                currentBaseEntity.put("portfolio",
                        new BaseValue(portfolio));
                break;
            case "details":
                currentDetails = new BaseSet(portfolioFlowDetailMeta);
                balanceAccountMap = new HashMap<>();
                break;
            case "detail":
                currentDetail = new BaseEntity(portfolioFlowDetailMeta, respondentId, batch.getReportDate(), batch.getId());
                break;
            case "balance_account":
                BaseEntity ba = new BaseEntity(refBalanceAccountMeta, respondentId, batch.getReportDate(), batch.getId());
                event = (XMLEvent) xmlReader.next();
                String balanceAccountNo = trim(event.asCharacters().getData());
                balanceAccountMap.put(balanceAccountNo, currentDetail);
                ba.put("no_", new BaseValue(balanceAccountNo));
                currentDetail.put(localName, new BaseValue(ba));
                break;
            case "value":
                event = (XMLEvent) xmlReader.next();
                currentDetail.put(localName,  new BaseValue(new Double(trim(event.asCharacters().getData()))));
                break;
            case "discounted_value":
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put(localName, new BaseValue(new Double(trim(event.asCharacters().getData()))));
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "portfolio_flow_msfo":
                return true;
            case "portfolio":
                break;
            case "details":
                currentBaseEntity.put(localName,new BaseValue(currentDetails));
                for (String no : balanceAccountMap.keySet()) {
                    currentDetails.put(new BaseValue(balanceAccountMap.get(no)));
                }
                break;
            case "detail":
                break;
            case "balance_account":
                break;
            case "value":
                break;
            case "discounted_value":
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }
}
