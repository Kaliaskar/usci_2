//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.09.03 at 02:49:03 PM ALMT 
//


package kz.bsbnb.usci.wsclient.jaxb.ctr;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the kz.bsbnb.usci.wsclient.jaxb.ctr package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Entities_QNAME = new QName("", "entities");
    private final static QName _CtrTransactionContSum_QNAME = new QName("", "cont_sum");
    private final static QName _CtrTransactionCurrTransDate_QNAME = new QName("", "curr_trans_date");
    private final static QName _CtrTransactionBeneficiary_QNAME = new QName("", "beneficiary");
    private final static QName _CtrTransactionContNum_QNAME = new QName("", "cont_num");
    private final static QName _CtrTransactionSender_QNAME = new QName("", "sender");
    private final static QName _CtrTransactionRefCurrTransPpc_QNAME = new QName("", "ref_curr_trans_ppc");
    private final static QName _CtrTransactionRefCurrency_QNAME = new QName("", "ref_currency");
    private final static QName _CtrTransactionContDate_QNAME = new QName("", "cont_date");
    private final static QName _CtrTransactionContRegNum_QNAME = new QName("", "cont_reg_num");
    private final static QName _RefCountryCodeAlpha2_QNAME = new QName("", "code_alpha_2");
    private final static QName _RefCurrencyCode_QNAME = new QName("", "code");
    private final static QName _RefCurrencyShortName_QNAME = new QName("", "short_name");
    private final static QName _CtrSubjectRefCountry_QNAME = new QName("", "ref_country");
    private final static QName _CtrSubjectRefResidency_QNAME = new QName("", "ref_residency");
    private final static QName _CtrSubjectBinIin_QNAME = new QName("", "bin_iin");
    private final static QName _CtrSubjectRefEconSector_QNAME = new QName("", "ref_econ_sector");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: kz.bsbnb.usci.wsclient.jaxb.ctr
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Entities }
     * 
     */
    public Entities createEntities() {
        return new Entities();
    }

    /**
     * Create an instance of {@link CtrSubject }
     * 
     */
    public CtrSubject createCtrSubject() {
        return new CtrSubject();
    }

    /**
     * Create an instance of {@link RefCountry }
     * 
     */
    public RefCountry createRefCountry() {
        return new RefCountry();
    }

    /**
     * Create an instance of {@link RefCurrTransPpc }
     * 
     */
    public RefCurrTransPpc createRefCurrTransPpc() {
        return new RefCurrTransPpc();
    }

    /**
     * Create an instance of {@link RefResidency }
     * 
     */
    public RefResidency createRefResidency() {
        return new RefResidency();
    }

    /**
     * Create an instance of {@link RefCurrency }
     * 
     */
    public RefCurrency createRefCurrency() {
        return new RefCurrency();
    }

    /**
     * Create an instance of {@link RefEconSector }
     * 
     */
    public RefEconSector createRefEconSector() {
        return new RefEconSector();
    }

    /**
     * Create an instance of {@link CtrTransaction }
     * 
     */
    public CtrTransaction createCtrTransaction() {
        return new CtrTransaction();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Entities }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "entities")
    public JAXBElement<Entities> createEntities(Entities value) {
        return new JAXBElement<Entities>(_Entities_QNAME, Entities.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Double }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "cont_sum", scope = CtrTransaction.class)
    public JAXBElement<Double> createCtrTransactionContSum(Double value) {
        return new JAXBElement<Double>(_CtrTransactionContSum_QNAME, Double.class, CtrTransaction.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "curr_trans_date", scope = CtrTransaction.class)
    public JAXBElement<String> createCtrTransactionCurrTransDate(String value) {
        return new JAXBElement<String>(_CtrTransactionCurrTransDate_QNAME, String.class, CtrTransaction.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CtrSubject }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "beneficiary", scope = CtrTransaction.class)
    public JAXBElement<CtrSubject> createCtrTransactionBeneficiary(CtrSubject value) {
        return new JAXBElement<CtrSubject>(_CtrTransactionBeneficiary_QNAME, CtrSubject.class, CtrTransaction.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "cont_num", scope = CtrTransaction.class)
    public JAXBElement<String> createCtrTransactionContNum(String value) {
        return new JAXBElement<String>(_CtrTransactionContNum_QNAME, String.class, CtrTransaction.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CtrSubject }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "sender", scope = CtrTransaction.class)
    public JAXBElement<CtrSubject> createCtrTransactionSender(CtrSubject value) {
        return new JAXBElement<CtrSubject>(_CtrTransactionSender_QNAME, CtrSubject.class, CtrTransaction.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RefCurrTransPpc }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "ref_curr_trans_ppc", scope = CtrTransaction.class)
    public JAXBElement<RefCurrTransPpc> createCtrTransactionRefCurrTransPpc(RefCurrTransPpc value) {
        return new JAXBElement<RefCurrTransPpc>(_CtrTransactionRefCurrTransPpc_QNAME, RefCurrTransPpc.class, CtrTransaction.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RefCurrency }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "ref_currency", scope = CtrTransaction.class)
    public JAXBElement<RefCurrency> createCtrTransactionRefCurrency(RefCurrency value) {
        return new JAXBElement<RefCurrency>(_CtrTransactionRefCurrency_QNAME, RefCurrency.class, CtrTransaction.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "cont_date", scope = CtrTransaction.class)
    public JAXBElement<String> createCtrTransactionContDate(String value) {
        return new JAXBElement<String>(_CtrTransactionContDate_QNAME, String.class, CtrTransaction.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "cont_reg_num", scope = CtrTransaction.class)
    public JAXBElement<String> createCtrTransactionContRegNum(String value) {
        return new JAXBElement<String>(_CtrTransactionContRegNum_QNAME, String.class, CtrTransaction.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "code_alpha_2", scope = RefCountry.class)
    public JAXBElement<String> createRefCountryCodeAlpha2(String value) {
        return new JAXBElement<String>(_RefCountryCodeAlpha2_QNAME, String.class, RefCountry.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "code", scope = RefCurrency.class)
    public JAXBElement<String> createRefCurrencyCode(String value) {
        return new JAXBElement<String>(_RefCurrencyCode_QNAME, String.class, RefCurrency.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "short_name", scope = RefCurrency.class)
    public JAXBElement<String> createRefCurrencyShortName(String value) {
        return new JAXBElement<String>(_RefCurrencyShortName_QNAME, String.class, RefCurrency.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RefCountry }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "ref_country", scope = CtrSubject.class)
    public JAXBElement<RefCountry> createCtrSubjectRefCountry(RefCountry value) {
        return new JAXBElement<RefCountry>(_CtrSubjectRefCountry_QNAME, RefCountry.class, CtrSubject.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RefResidency }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "ref_residency", scope = CtrSubject.class)
    public JAXBElement<RefResidency> createCtrSubjectRefResidency(RefResidency value) {
        return new JAXBElement<RefResidency>(_CtrSubjectRefResidency_QNAME, RefResidency.class, CtrSubject.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "bin_iin", scope = CtrSubject.class)
    public JAXBElement<String> createCtrSubjectBinIin(String value) {
        return new JAXBElement<String>(_CtrSubjectBinIin_QNAME, String.class, CtrSubject.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RefEconSector }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "ref_econ_sector", scope = CtrSubject.class)
    public JAXBElement<RefEconSector> createCtrSubjectRefEconSector(RefEconSector value) {
        return new JAXBElement<RefEconSector>(_CtrSubjectRefEconSector_QNAME, RefEconSector.class, CtrSubject.class, value);
    }

}
