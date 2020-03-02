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

/**
 * @author Artur Tkachenko
 * @author Kanat Tulbassiev
 */

@Component
@Scope("prototype")
public class PortfolioFlowParser extends BatchParser {
    public PortfolioFlowParser() {
        super();
    }

    private BaseSet currentDetails;
    private BaseEntity currentDetail;

    private MetaClass refPortfolioMeta, portfolioFlowDetailMeta, refBalanceAccountMeta;

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("portfolio_flow_kfn"), respondentId, batch.getReportDate(), batch.getId());

        currentDetails = null;
        currentDetail = null;

        refPortfolioMeta = metaClassRepository.getMetaClass("ref_portfolio");
        portfolioFlowDetailMeta = metaClassRepository.getMetaClass("portfolio_flow_detail");
        refBalanceAccountMeta = metaClassRepository.getMetaClass("ref_balance_account");
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "portfolio_flow":
                break;
            case "portfolio":
                BaseEntity portfolio = new BaseEntity(refPortfolioMeta, respondentId, batch.getReportDate(), batch.getId());

                event = (XMLEvent) xmlReader.next();

                portfolio.put("code",
                        new BaseValue(trim(event.asCharacters().getData())));

                currentBaseEntity.put("portfolio",
                        new BaseValue(portfolio));
                break;
            case "details":
                currentDetails = new BaseSet(portfolioFlowDetailMeta);
                break;
            case "detail":
                currentDetail = new BaseEntity(portfolioFlowDetailMeta, respondentId, batch.getReportDate(), batch.getId());
                break;
            case "balance_account":
                BaseEntity ba = new BaseEntity(refBalanceAccountMeta, respondentId, batch.getReportDate(), batch.getId());
                event = (XMLEvent) xmlReader.next();
                ba.put("no_", new BaseValue(trim(event.asCharacters().getData())));
                currentDetail.put(localName, new BaseValue(ba));
                break;
            case "value":
                event = (XMLEvent) xmlReader.next();
                currentDetail.put(localName, new BaseValue(new Double(trim(event.asCharacters().getData()))));
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "portfolio_flow":
                return true;
            case "portfolio":
                break;
            case "details":
                currentBaseEntity.put(localName, new BaseValue(currentDetails));
                break;
            case "detail":
                currentDetails.put(new BaseValue(currentDetail));
                break;
            case "balance_account":
                break;
            case "value":
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }
}
