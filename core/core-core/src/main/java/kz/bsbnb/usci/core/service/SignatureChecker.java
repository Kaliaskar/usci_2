package kz.bsbnb.usci.core.service;

import kz.bsbnb.usci.model.exception.UsciException;
import kz.gamma.asn1.*;
import kz.gamma.cms.Pkcs7Data;
import kz.gamma.jce.X509Principal;
import kz.gamma.jce.provider.GammaTechProvider;
import kz.gamma.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * @author Aidar.Myrzahanov
 */
@Component
public class SignatureChecker {
    private static final String ROOT_CA_NAME = "C=KZ,O=KISC,CN=KISC Root CA";
    private final boolean USE_NEW_EXTRACT_BIN = true;
    private final String ocspServiceUrl = "http://91.195.226.34:62255";

    private final Environment environment;

    Logger log = LoggerFactory.getLogger(SignatureChecker.class);

    public SignatureChecker(Environment environment) {
        this.environment = environment;
    }

    public Map<String, Object> checkAndUpdate(String binNo, String hash, String signature) throws UsciException {
        if (Security.getProvider(GammaTechProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new GammaTechProvider());
        }
        if (binNo == null) {
            throw new UsciException("BIN_NOT_FOUND_FOR_CREDITOR");
        }
        if (signature == null || signature.isEmpty()) {
            throw new UsciException("SIGNATURE_IS_EMPTY");
        }

        try {
            Pkcs7Data pkcs = new Pkcs7Data(Base64.decode(signature));
            X509Certificate certificate = pkcs.getCertificateOfSigner();
            checkHash(pkcs, hash);

            // в режиме разработки проверки дат и тд отключены чтобы тестировать через тестовые ключи
            // иначе проверку не пройдем
            if (Arrays.stream(environment.getActiveProfiles())
                    .noneMatch(env -> (env.equalsIgnoreCase("dev") || env.equalsIgnoreCase("nb-stend")))) {
                checkDates(certificate);
                checkIssuer(certificate);
                checkBin(certificate, binNo);
                checkStatus(certificate);
            }

            Map<String, Object> params = new HashMap<>();
            params.put("info", certificate.getSubjectX500Principal().getName());

            Date signDate = retrieveSigningTime(pkcs);
            LocalDateTime signTime = LocalDateTime.ofInstant(signDate.toInstant(), ZoneId.systemDefault());

            params.put("signTime", signTime);

            return params;
        } catch (UsciException sve) {
            throw sve;
        } catch (Exception e) {
            throw new UsciException("UNEXPECTED_EXCEPTION_DURING_SIGNATURE_VALIDATION, e.getMessage()");
        }
    }

    private void checkHash(Pkcs7Data pkcs, String signedValue) throws UsciException {
        if (!pkcs.verify()) {
            throw new UsciException("SIGNATURE_IS_INCORRECT");
        }
        try {
            String valueFromSignature = new String(pkcs.getData());
            if (!signedValue.equals(valueFromSignature)) {
                throw new UsciException("SIGNATURE_IS_INCORRECT");
            }
        } catch (Exception e) {
            throw new UsciException("SIGNATURE_IS_INCORRECT");
        }

    }

    private void checkDates(X509Certificate certificate) throws UsciException {
        Date now = new Date();
        if (now.after(certificate.getNotAfter())) {
            throw new UsciException("SIGNATURE_IS_OVERDUE, certificate.getNotAfter()");
        }
        if (now.before(certificate.getNotBefore())) {
            throw new UsciException("SIGNATURE_IS_NOT_ACTIVE_YET, certificate.getNotBefore()");
        }
    }

    private void checkIssuer(X509Certificate certificate) throws UsciException {
        String issuerName = certificate.getIssuerX500Principal().getName();
        if (!ROOT_CA_NAME.equals(issuerName)) {
            throw new UsciException("WRONG_ROOT_CA_NAME, issuerName");
        }
    }

    private void checkBin(X509Certificate certificate, String binNo) throws UsciException, IOException {
        String bin = extractBinFromCertificate(certificate);
        if (bin == null) {
            throw new UsciException("SIGNATURE_WITHOUT_BIN");
        }
        if (!bin.equals(binNo)) {
            throw new UsciException("SIGNATURE_BIN_DOESNT_MATCH, bin, binNo");
        }
    }

    private String extractBinFromCertificate(X509Certificate certificate) throws IOException, UsciException {
        if (!USE_NEW_EXTRACT_BIN) {
            return extractBinFromCertificate_Old(certificate);
        } else {
            return extractBinFromCertificate_New(certificate);
        }
    }

    private String extractBinFromCertificate_Old(X509Certificate certificate) throws IOException, UsciException {

        ASN1InputStream extensionStream = null;

        try {

            byte[] extensionBytes = certificate.getExtensionValue("2.5.29.17");
            if (extensionBytes == null) {
                throw new UsciException("SUBJECT_ALTERNATIVE_NAME_FIELD_IS_EMPTY");
            }

            extensionStream = new ASN1InputStream(extensionBytes);
            DEROctetString octetString = (DEROctetString) extensionStream.readObject();
            extensionStream.close();
            extensionStream = new ASN1InputStream(octetString.getOctets());
            DERSequence sequence = (DERSequence) extensionStream.readObject();
            extensionStream.close();
            Enumeration subjectAltNames = sequence.getObjects();
            String authority = null;

            while (subjectAltNames.hasMoreElements()) {
                DERTaggedObject nextElement = (DERTaggedObject) subjectAltNames.nextElement();
                X509Principal x509Principal = new X509Principal(nextElement.getObject().getEncoded());
                String data = x509Principal.toString();
                if (authority == null) {
                    authority = data;
                } else {
                    return data.substring(data.lastIndexOf('=') + 1);
                }
            }

        } finally {
            if (extensionStream != null) {
                try {
                    extensionStream.close();
                } catch (IOException ioe) {
                    //log.log(Level.SEVERE, "", ioe);
                }
            }
        }

        return null;

    }

    private String extractBinFromCertificate_New(X509Certificate userCertificate) throws IOException, UsciException {

        String[] data = null;
        ASN1InputStream extensionStream = null;

        try {

            byte[] extensionBytes = userCertificate.getExtensionValue("2.5.29.17");
            if (extensionBytes == null) {
                throw new UsciException("SUBJECT_ALTERNATIVE_NAME_FIELD_IS_EMPTY");
            }

            extensionStream = new ASN1InputStream(extensionBytes);
            DEROctetString octetString = (DEROctetString) extensionStream.readObject();
            extensionStream.close();
            extensionStream = new ASN1InputStream(octetString.getOctets());
            DERSequence sequence = (DERSequence) extensionStream.readObject();
            extensionStream.close();
            Enumeration subjectAltNames = sequence.getObjects();
            int counter = 0;
            String str1 = null;
            String str2 = null;

            while (subjectAltNames.hasMoreElements()) {
                DERTaggedObject nextElement = (DERTaggedObject) subjectAltNames.nextElement();
                X509Principal x509Principal = new X509Principal(nextElement.getObject().getEncoded());
                if (counter == 1) {
                    String extensionData = x509Principal.toString();
                    Map<String, String> hashmap = convertBIN_IIN(extensionData.trim());
                    str1 = hashmap.get("SERIALNUMBER"); // BIN or IIN
                } else if (counter == 2) {
                    String extensionData = x509Principal.toString();
                    Map<String, String> hashmap = convertBIN_IIN(extensionData.trim());
                    str2 = hashmap.get("SERIALNUMBER"); // IIN
                }
                counter++;
            }

            if (str2 != null && str1 != null) { // est' i bin i iin
                data = new String[2];
                data[0] = str2;
                data[1] = str1;
            } else if (str1 != null) { // tol'ko iin
                data = new String[1];
                data[0] = str1;
            }

        } finally {
            if (extensionStream != null) {
                try {
                    extensionStream.close();
                } catch (IOException e) {
                }
            }
        }

        if (data == null) return null;
        if (data.length == 2) return data[1]; // BIN
        if (data.length == 1) return data[0]; // IIN

        return null;

    }

    private Map<String, String> convertBIN_IIN(String extensionData) {

        Map<String, String> hashmap = new HashMap<>();
        String[] aliasStr = extensionData.split(",");

        for (int i = 0; i < aliasStr.length; i++) {
            int index_of_equal = aliasStr[i].lastIndexOf("=");
            String str1 = aliasStr[i].substring(index_of_equal + 1);
            String str2 = aliasStr[i].substring(0, index_of_equal);
            hashmap.put(str2, str1);
        }

        return hashmap;

    }

    private void checkStatus(X509Certificate certificate) throws UsciException {
        if (ocspServiceUrl == null || " ".equals(ocspServiceUrl)) {
            return;
        }
        OcspRequest request = new OcspRequest(ocspServiceUrl);
        request.check(certificate);
    }

    private Date retrieveSigningTime(Pkcs7Data pkcs) throws Exception {
        byte[] attrValue = pkcs.getAttributeByOid("1.2.840.113549.1.9.5");

        ASN1InputStream extensionStream = null;
        try {
            extensionStream = new ASN1InputStream(attrValue);
            DERUTCTime signingTime = (DERUTCTime) extensionStream.readObject();
            return signingTime.getAdjustedDate();
        } finally {
            if (extensionStream != null) {
                try {
                    extensionStream.close();
                } catch (IOException ioe) {
                    //log.log(Level.SEVERE, "", ioe);
                }
            }
        }
    }

}
