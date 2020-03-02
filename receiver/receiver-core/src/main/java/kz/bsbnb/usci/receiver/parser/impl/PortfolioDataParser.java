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

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * @author Artur Tkachenko
 * @author Kanat Tulbassiev
 */

@Component
@Scope("prototype")
public class PortfolioDataParser extends BatchParser {
    @Autowired
    private PortfolioFlowParser portfolioFlowParser = new PortfolioFlowParser();
    @Autowired
    private PortfolioFlowMsfoParser portfolioFlowMsfoParser = new PortfolioFlowMsfoParser();

    public PortfolioDataParser() {
        super();
    }
    private BaseSet portfolioFlow;
    private BaseSet portfolioFlowMsfo;

    private MetaClass portfolioFlowKfnMeta, portfolioFlowMsfoMeta;

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("portfolio_data"), respondentId, batch.getReportDate(), batch.getId());

        portfolioFlowKfnMeta = metaClassRepository.getMetaClass("portfolio_flow_kfn");
        portfolioFlowMsfoMeta = metaClassRepository.getMetaClass("portfolio_flow_msfo");
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "portfolio_data":
                break;
            case "portfolio_flow":
                portfolioFlowParser.parse(xmlReader, batch, index, respondentId);
                getPortfolioFlow().put(new BaseValue(portfolioFlowParser.getCurrentBaseEntity()));
                break;
            case "portfolio_flow_msfo":
                portfolioFlowMsfoParser.parse(xmlReader, batch, index, respondentId);
                getPortfolioFlowMsfo().put(new BaseValue(portfolioFlowMsfoParser.getCurrentBaseEntity()));
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        if (localName.equals("portfolio_data")) {
            if (portfolioFlow != null)
                currentBaseEntity.put("portfolio_flows_kfn", new BaseValue(portfolioFlow));

            if (portfolioFlowMsfo != null)
                currentBaseEntity.put("portfolio_flows_msfo", new BaseValue(portfolioFlowMsfo));

            return true;
        } else {
            throw new UnknownTagException(localName);
        }
    }

    private BaseSet getPortfolioFlow() {
        if (portfolioFlow == null)
            portfolioFlow = new BaseSet(portfolioFlowKfnMeta);
        return portfolioFlow;
    }

    private BaseSet getPortfolioFlowMsfo() {
        if (portfolioFlowMsfo == null)
            portfolioFlowMsfo = new BaseSet(portfolioFlowMsfoMeta);
        return portfolioFlowMsfo;
    }
}
