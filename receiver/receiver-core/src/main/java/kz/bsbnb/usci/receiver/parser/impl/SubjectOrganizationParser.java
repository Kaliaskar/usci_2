package kz.bsbnb.usci.receiver.parser.impl;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseSet;
import kz.bsbnb.usci.eav.model.base.BaseValue;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.eav.model.meta.MetaDataType;
import kz.bsbnb.usci.eav.model.meta.MetaValue;
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
public class SubjectOrganizationParser extends BatchParser {
    @Autowired
    private SubjectOrganizationNamesParser subjectOrganizationNamesParser;

    @Autowired
    private SubjectOrganizationDocsParser subjectOrganizationDocsParser;

    @Autowired
    private SubjectOrganizationHeadParser subjectOrganizationHeadParser;

    public SubjectOrganizationParser() {
        super();
    }

    private BaseSet bankRelations;
    private BaseSet addresses;
    private BaseSet contacts;

    private BaseEntity currentContact;
    private BaseEntity currentAddress;
    private BaseEntity organizationInfo;

    private MetaClass refCountryMeta, refOffshoreMeta, bankRelationMeta, refBankRelationMeta, addressMeta, refRegionMeta,
            contactMeta, refContactType, organizationNameMeta, refLegalFormMeta, refEnterpriseTypeMeta, refEconTradeMeta,
            documentMeta;

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("subject"), respondentId, batch.getReportDate(), batch.getId());

        organizationInfo = new BaseEntity(metaClassRepository.getMetaClass("organization_info"), respondentId, batch.getReportDate(), batch.getId());

        bankRelations = null;
        addresses = null;
        contacts = null;
        currentContact = null;
        currentAddress = null;

        refCountryMeta = metaClassRepository.getMetaClass("ref_country");
        refOffshoreMeta = metaClassRepository.getMetaClass("ref_offshore");
        bankRelationMeta = metaClassRepository.getMetaClass("bank_relation");
        refBankRelationMeta = metaClassRepository.getMetaClass("ref_bank_relation");
        addressMeta = metaClassRepository.getMetaClass("address");
        refRegionMeta = metaClassRepository.getMetaClass("ref_region");
        contactMeta = metaClassRepository.getMetaClass("contact");
        refContactType = metaClassRepository.getMetaClass("ref_contact_type");
        organizationNameMeta = metaClassRepository.getMetaClass("organization_name");
        refLegalFormMeta = metaClassRepository.getMetaClass("ref_legal_form");
        refEnterpriseTypeMeta = metaClassRepository.getMetaClass("ref_enterprise_type");
        refEconTradeMeta = metaClassRepository.getMetaClass("ref_econ_trade");
        documentMeta = metaClassRepository.getMetaClass("document");
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "organization":
                break;
            case "country":
                event = (XMLEvent) xmlReader.next();
                BaseEntity country = new BaseEntity(refCountryMeta, respondentId, batch.getReportDate(), batch.getId());
                country.put("code_numeric",  new BaseValue(new Integer(trim(event.asCharacters().getData()))));
                organizationInfo.put("country", new BaseValue(country));
                break;
            case "offshore":
                event = (XMLEvent) xmlReader.next();
                BaseEntity offshore = new BaseEntity(refOffshoreMeta, respondentId, batch.getReportDate(), batch.getId());
                offshore.put("code", new BaseValue(trim(event.asCharacters().getData())));
                organizationInfo.put("offshore", new BaseValue(offshore));
                break;
            case "bank_relations":
                bankRelations = new BaseSet(bankRelationMeta);
                break;
            case "bank_relation":
                event = (XMLEvent) xmlReader.next();
                BaseEntity bankRelation = new BaseEntity(bankRelationMeta, respondentId, batch.getReportDate(), batch.getId());
                BaseEntity refBankRelation = new BaseEntity(refBankRelationMeta, respondentId, batch.getReportDate(), batch.getId());
                refBankRelation.put("code",new BaseValue(trim(event.asCharacters().getData())));
                bankRelation.put("bank_relation", new BaseValue(refBankRelation));
                bankRelations.put(new BaseValue(bankRelation));
                break;
            case "addresses":
                addresses = new BaseSet(addressMeta);
                break;
            case "address":
                currentAddress = new BaseEntity(addressMeta, respondentId, batch.getReportDate(), batch.getId());
                currentAddress.put("type", new BaseValue(event.asStartElement().getAttributeByName(new QName("type")).getValue()));
                break;
            case "region":
                BaseEntity region = new BaseEntity(refRegionMeta, respondentId, batch.getReportDate(),batch.getId());
                event = (XMLEvent) xmlReader.next();
                region.put("code", new BaseValue(trim(event.asCharacters().getData())));
                currentAddress.put("region", new BaseValue(region));
                break;
            case "details":
                event = (XMLEvent) xmlReader.next();
                currentAddress.put("details", new BaseValue(trim(event.asCharacters().getData())));
                break;
            case "contacts":
                contacts = new BaseSet(contactMeta);
                break;
            case "contact":
                currentContact = new BaseEntity(contactMeta, respondentId, batch.getReportDate(), batch.getId());

                BaseEntity contactType = new BaseEntity(refContactType, respondentId, batch.getReportDate(), batch.getId());

                contactType.put("code", new BaseValue(event.asStartElement().getAttributeByName(new QName("contact_type")).getValue()));
                currentContact.put("contact_type", new BaseValue(contactType));
                BaseSet contactDetails = new BaseSet(new MetaValue(MetaDataType.STRING));

                event = (XMLEvent) xmlReader.next();
                contactDetails.put(new BaseValue(trim(event.asCharacters().getData())));
                currentContact.put("details", new BaseValue(contactDetails));
                contacts.put(new BaseValue(currentContact));
                break;
            case "names":
                BaseSet organizationNames = new BaseSet(organizationNameMeta);

                while (true) {
                    subjectOrganizationNamesParser.parse(xmlReader, batch, index, respondentId);
                    if (subjectOrganizationNamesParser.hasMore()) {
                        organizationNames.put(new BaseValue(subjectOrganizationNamesParser.getCurrentBaseEntity()));
                    } else break;
                }
                organizationInfo.put("names",new BaseValue(organizationNames));
                break;
            case "head":
                subjectOrganizationHeadParser.parse(xmlReader, batch, index, respondentId);
                organizationInfo.put("head", new BaseValue(subjectOrganizationHeadParser.getCurrentBaseEntity()));
                break;
            case "legal_form":
                BaseEntity legalForm = new BaseEntity(refLegalFormMeta, respondentId, batch.getReportDate(), batch.getId());
                event = (XMLEvent) xmlReader.next();
                legalForm.put("code", new BaseValue(trim(event.asCharacters().getData())));
                organizationInfo.put("legal_form", new BaseValue(legalForm));
                break;
            case "enterprise_type":
                event = (XMLEvent) xmlReader.next();
                BaseEntity enterpriseType = new BaseEntity(refEnterpriseTypeMeta, respondentId, batch.getReportDate(), batch.getId());
                enterpriseType.put("code", new BaseValue(trim(event.asCharacters().getData())));
                organizationInfo.put("enterprise_type", new BaseValue(enterpriseType));
                break;
            case "econ_trade":
                event = (XMLEvent) xmlReader.next();
                BaseEntity econTrade = new BaseEntity(refEconTradeMeta, respondentId, batch.getReportDate(), batch.getId());
                econTrade.put("code",  new BaseValue(trim(event.asCharacters().getData())));
                organizationInfo.put("econ_trade", new BaseValue(econTrade));
                break;
            case "is_se":
                event = (XMLEvent) xmlReader.next();
                organizationInfo.put("is_se",new BaseValue(new Boolean((trim(event.asCharacters().getData())))));
                break;
            case "docs":
                BaseSet organizationDocs = new BaseSet(documentMeta);

                while (true) {
                    subjectOrganizationDocsParser.parse(xmlReader, batch, index, respondentId);
                    if (subjectOrganizationDocsParser.hasMore()) {
                        organizationDocs.put(new BaseValue(subjectOrganizationDocsParser.getCurrentBaseEntity()));
                    } else break;
                }

                currentBaseEntity.put("docs", new BaseValue(organizationDocs));
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "organization":
                currentBaseEntity.put("organization_info",new BaseValue(organizationInfo));
                currentBaseEntity.put("is_person", new BaseValue(false));
                currentBaseEntity.put("is_organization", new BaseValue(true));
                currentBaseEntity.put("is_creditor", new BaseValue(false));
                return true;
            case "country":
                break;
            case "offshore":
                break;
            case "bank_relations":
                organizationInfo.put("bank_relations",  new BaseValue( bankRelations));
                break;
            case "bank_relation":
                break;
            case "addresses":
                organizationInfo.put("addresses", new BaseValue(addresses));
                break;
            case "address":
                addresses.put(new BaseValue(currentAddress));
                break;
            case "region":
                break;
            case "details":
                break;
            case "contacts":
                break;
            case "contact":
                organizationInfo.put("contacts",
                        new BaseValue(contacts));
                break;
            case "names":
                break;
            case "head":
                break;
            case "legal_form":
                break;
            case "enterprise_type":
                break;
            case "econ_trade":
                break;
            case "is_se":
                break;
            case "docs":
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }
}
