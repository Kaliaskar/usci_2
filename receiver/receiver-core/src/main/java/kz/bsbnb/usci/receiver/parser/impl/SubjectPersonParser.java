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
public class SubjectPersonParser extends BatchParser {
    @Autowired
    private SubjectPersonDocsParser subjectPersonDocsParser;

    public SubjectPersonParser() {
        super();
    }
    private BaseSet names;
    private BaseEntity currentName;
    private BaseSet addresses;
    private BaseEntity currentAddress;
    private BaseSet contacts;
    private BaseSet bankRelations;
    private BaseEntity personInfo;

    private MetaClass refCountryMeta, refOffshoreMeta, bankRelationMeta, refBankRelationMeta, addressMeta,
            refRegionMeta, contactMeta, refContactTypeMeta, personNameMeta, documentMeta;

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("subject"), respondentId, batch.getReportDate(), batch.getId());
        personInfo = new BaseEntity(metaClassRepository.getMetaClass("person_info"), respondentId, batch.getReportDate(), batch.getId());

        names = null;
        currentName = null;
        addresses = null;
        currentAddress = null;
        bankRelations = null;

        refCountryMeta = metaClassRepository.getMetaClass("ref_country");
        refOffshoreMeta = metaClassRepository.getMetaClass("ref_offshore");
        bankRelationMeta = metaClassRepository.getMetaClass("bank_relation");
        refBankRelationMeta = metaClassRepository.getMetaClass("ref_bank_relation");
        addressMeta = metaClassRepository.getMetaClass("address");
        refRegionMeta = metaClassRepository.getMetaClass("ref_region");
        contactMeta = metaClassRepository.getMetaClass("contact");
        refContactTypeMeta = metaClassRepository.getMetaClass("ref_contact_type");
        personNameMeta = metaClassRepository.getMetaClass("person_name");
        documentMeta = metaClassRepository.getMetaClass("document");
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName)
            throws SAXException {
        switch (localName) {
            case "person":
                break;
            case "country":
                event = (XMLEvent) xmlReader.next();
                BaseEntity country = new BaseEntity(refCountryMeta, respondentId, batch.getReportDate(), batch.getId());
                country.put("code_numeric", new BaseValue(new Integer(trim(event.asCharacters().getData()))));
                personInfo.put("country", new BaseValue(country));
                break;
            case "offshore":
                event = (XMLEvent) xmlReader.next();
                BaseEntity ref_offshore = new BaseEntity(refOffshoreMeta, respondentId, batch.getReportDate(), batch.getId());
                ref_offshore.put("code", new BaseValue(trim(event.asCharacters().getData())));
                personInfo.put("offshore",  new BaseValue(ref_offshore));
                break;
            case "bank_relations":
                bankRelations = new BaseSet(bankRelationMeta);
                break;
            case "bank_relation":
                event = (XMLEvent) xmlReader.next();
                BaseEntity bankRelation = new BaseEntity(bankRelationMeta, respondentId, batch.getReportDate(), batch.getId());
                BaseEntity refBankRelation = new BaseEntity(refBankRelationMeta, respondentId, batch.getReportDate(), batch.getId());
                refBankRelation.put("code", new BaseValue(trim(event.asCharacters().getData())));
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
                event = (XMLEvent) xmlReader.next();
                BaseEntity region = new BaseEntity(refRegionMeta, respondentId, batch.getReportDate(), batch.getId());
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
                BaseEntity currentContact = new BaseEntity(contactMeta, respondentId, batch.getReportDate(), batch.getId());

                BaseEntity contactType = new BaseEntity(refContactTypeMeta, respondentId, batch.getReportDate(), batch.getId());
                contactType.put("code", new BaseValue(event.asStartElement().getAttributeByName(new QName("contact_type")).getValue()));
                currentContact.put("contact_type",new BaseValue(contactType));
                BaseSet contactDetails = new BaseSet(new MetaValue(MetaDataType.STRING));

                event = (XMLEvent) xmlReader.next();
                contactDetails.put(new BaseValue(trim(event.asCharacters().getData())));
                currentContact.put("details", new BaseValue(contactDetails));
                contacts.put(new BaseValue(currentContact));
                break;
            case "names":
                names = new BaseSet(personNameMeta);
                break;
            case "name":
                currentName = new BaseEntity(personNameMeta, respondentId, batch.getReportDate(), batch.getId());
                currentName.put("lang", new BaseValue(event.asStartElement().getAttributeByName(new QName("lang")).getValue()));
                break;
            case "firstname":
                event = (XMLEvent) xmlReader.next();
                currentName.put("firstname", new BaseValue(trim(event.asCharacters().getData())));
                break;
            case "lastname":
                event = (XMLEvent) xmlReader.next();
                currentName.put("lastname", new BaseValue(trim(event.asCharacters().getData())));
                break;
            case "middlename":
                event = (XMLEvent) xmlReader.next();
                currentName.put("middlename", new BaseValue(trim(event.asCharacters().getData())));
                break;
            case "docs":
                BaseSet personDocs = new BaseSet(documentMeta);

                while (true) {
                    subjectPersonDocsParser.parse(xmlReader, batch, index, respondentId);

                    if (subjectPersonDocsParser.hasMore()) {
                        personDocs.put(new BaseValue(subjectPersonDocsParser.getCurrentBaseEntity()));
                    } else break;
                }

                currentBaseEntity.put("docs",new BaseValue(personDocs));
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "person":
                currentBaseEntity.put("person_info", new BaseValue(personInfo));
                currentBaseEntity.put("is_person", new BaseValue(true));
                currentBaseEntity.put("is_organization", new BaseValue(false));
                currentBaseEntity.put("is_creditor", new BaseValue(false));
                return true;
            case "country":
                break;
            case "offshore":
                break;
            case "bank_relations":
                personInfo.put("bank_relations", new BaseValue(bankRelations));
                break;
            case "bank_relation":
                break;
            case "addresses":
                personInfo.put("addresses", new BaseValue(addresses));
                break;
            case "address":
                addresses.put(new BaseValue(currentAddress));
                break;
            case "region":
                break;
            case "details":
                break;
            case "contacts":
                personInfo.put("contacts",  new BaseValue(contacts));
                break;
            case "contact":
                break;
            case "names":
                personInfo.put("names", new BaseValue(names));
                break;
            case "name":
                names.put(new BaseValue(currentName));
                break;
            case "firstname":
                break;
            case "lastname":
                break;
            case "middlename":
                break;
            case "docs":
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }
}
