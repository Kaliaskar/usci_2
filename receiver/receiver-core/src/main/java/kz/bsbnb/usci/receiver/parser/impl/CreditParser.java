package kz.bsbnb.usci.receiver.parser.impl;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseValue;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.receiver.parser.BatchParser;
import kz.bsbnb.usci.receiver.parser.exceptions.UnknownTagException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * @author Artur Tkachenko
 * @author Kanat Tulbassiev
 */

@Component
@Scope("prototype")
public class CreditParser extends BatchParser {
    private int portfolioCount;
    private BaseEntity currentPortfolio;

    @Autowired
    private CreditContractParser creditContractParser;

    @Autowired
    private CreditorBranchParser creditorBranchParser;

    private MetaClass refCurrencyMeta, refCreditPurposeMeta, refCreditObjectMeta, refFinanceSourceMeta, refPortfolioMeta,
            portfolioMeta;

    public CreditParser() {
        super();
    }

    @Override
    public void init() {
        refCurrencyMeta = metaClassRepository.getMetaClass("ref_currency");
        refCreditPurposeMeta = metaClassRepository.getMetaClass("ref_credit_purpose");
        refCreditObjectMeta = metaClassRepository.getMetaClass("ref_credit_object");
        refFinanceSourceMeta = metaClassRepository.getMetaClass("ref_finance_source");
        refPortfolioMeta = metaClassRepository.getMetaClass("ref_portfolio");
        portfolioMeta = metaClassRepository.getMetaClass("portfolio");
    }

    public void setCurrentBaseEntity(BaseEntity baseEntity) {
        currentBaseEntity = baseEntity;
    }

    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {

        switch (localName) {
            case "credit":
                break;
            case "contract":
                creditContractParser.parse(xmlReader, batch, index, respondentId);
                BaseEntity creditContract = creditContractParser.getCurrentBaseEntity();
                currentBaseEntity.put("contract", new BaseValue(creditContract));
                break;
            case "currency":
                event = (XMLEvent) xmlReader.next();
                BaseEntity currency = new BaseEntity(refCurrencyMeta, respondentId, batch.getReportDate(), batch.getId());
                currency.put("short_name", new BaseValue(trim(event.asCharacters().getData())));
                currentBaseEntity.put("currency", new BaseValue(currency));
                break;
            case "interest_rate_yearly":
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("interest_rate_yearly", new BaseValue(new Double(trim(event.asCharacters().getData()))));
                break;
            case "contract_maturity_date": {
                event = (XMLEvent) xmlReader.next();
                String dateRaw = trim(event.asCharacters().getData());
                try {
                    currentBaseEntity.put("contract_maturity_date", new BaseValue(LocalDate.parse(dateRaw)));
                } catch (DateTimeParseException e) {
                   currentBaseEntity.addValidationError("Неправильная дата: " + dateRaw);
                }
                break;
            }
            case "actual_issue_date": {
                event = (XMLEvent) xmlReader.next();
                String dateRaw = trim(event.asCharacters().getData());
                try {
                    currentBaseEntity.put("actual_issue_date", new BaseValue(LocalDate.parse(dateRaw)));
                } catch (DateTimeParseException e) {
                    currentBaseEntity.addValidationError("Неправильная дата: " + dateRaw);
                }
                break;
            }
            case "credit_purpose":
                event = (XMLEvent) xmlReader.next();
                BaseEntity creditPurpose = new BaseEntity(refCreditPurposeMeta, respondentId, batch.getReportDate(), batch.getId());
                creditPurpose.put("code", new BaseValue(trim(event.asCharacters().getData())));
                currentBaseEntity.put("credit_purpose", new BaseValue(creditPurpose));
                break;
            case "credit_object":
                event = (XMLEvent) xmlReader.next();
                BaseEntity creditObject = new BaseEntity(refCreditObjectMeta, respondentId, batch.getReportDate(), batch.getId());
                creditObject.put("code", new BaseValue(trim(event.asCharacters().getData())));
                currentBaseEntity.put("credit_object", new BaseValue(creditObject));
                break;
            case "amount":
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("amount", new BaseValue(new Double(trim(event.asCharacters().getData()))));
                break;
            case "finance_source":
                event = (XMLEvent) xmlReader.next();
                BaseEntity financeSource = new BaseEntity(refFinanceSourceMeta, respondentId, batch.getReportDate(), batch.getId());
                financeSource.put("code", new BaseValue(trim(event.asCharacters().getData())));
                currentBaseEntity.put("finance_source",  new BaseValue(financeSource));
                break;
            case "has_currency_earn":
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("has_currency_earn", new BaseValue(new Boolean(trim(event.asCharacters().getData()))));
                break;
            case "creditor_branch":
                creditorBranchParser.parse(xmlReader, batch, index, respondentId);
                BaseEntity creditorBranch = creditorBranchParser.getCurrentBaseEntity();
                currentBaseEntity.put("creditor_branch", new BaseValue(creditorBranch));
                break;
            case "portfolio":
                portfolioCount++;

                if (portfolioCount == 2) {
                    String value = getNullableTagValue(localName, event, xmlReader);

                    if (value != null) {
                        BaseEntity portfolio = new BaseEntity(refPortfolioMeta, respondentId, batch.getReportDate(), batch.getId());
                        portfolio.put("code", new BaseValue(value));
                        currentPortfolio.put("portfolio", new BaseValue(portfolio));
                    } else {
                        currentPortfolio.put("portfolio", new BaseValue(null));
                    }
                } else {
                    currentPortfolio = new BaseEntity(portfolioMeta, respondentId, batch.getReportDate(), batch.getId());
                }
                break;
            case "portfolio_msfo":
                String value = getNullableTagValue(localName, event, xmlReader);

                if (value != null) {
                    BaseEntity portfolioMSFO = new BaseEntity(refPortfolioMeta, respondentId, batch.getReportDate(), batch.getId());
                    portfolioMSFO.put("code", new BaseValue(value));
                    currentPortfolio.put("portfolio_msfo", new BaseValue(portfolioMSFO));
                } else {
                    currentPortfolio.put("portfolio_msfo", new BaseValue(null));
                }
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    private String getNullableTagValue(String localName, XMLEvent event, XMLEventReader xmlReader) {
        Attribute attrNullify = event.asStartElement().getAttributeByName(QName.valueOf("nullify"));

        if (attrNullify == null || !"true".equals(attrNullify.getValue())) {
            event = (XMLEvent) xmlReader.next();
            if(event.isCharacters()) {
                return trim(event.asCharacters().getData());
            }
        }

        return null;
    }

    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "credit":
                return true;
            case "contract":
                break;
            case "currency":
                break;
            case "interest_rate_yearly":
                break;
            case "contract_maturity_date":
                break;
            case "actual_issue_date":
                break;
            case "credit_purpose":
                break;
            case "credit_object":
                break;
            case "amount":
                break;
            case "finance_source":
                break;
            case "has_currency_earn":
                break;
            case "creditor_branch":
                break;
            case "portfolio":
                portfolioCount--;
                if (portfolioCount == 0)
                    currentBaseEntity.put("portfolio",
                            new BaseValue(currentPortfolio));

                break;
            case "portfolio_msfo":
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }
}

