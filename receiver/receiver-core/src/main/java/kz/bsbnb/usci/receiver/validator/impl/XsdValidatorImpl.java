package kz.bsbnb.usci.receiver.validator.impl;

import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.receiver.model.exception.ReceiverException;
import kz.bsbnb.usci.receiver.validator.XsdValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;

/**
 * Вадидирует XML файл по правилам XSD
 * @author Yernur Bakash
 */

@Service
public class XsdValidatorImpl implements XsdValidator {
    private static final Logger logger = LoggerFactory.getLogger(XsdValidatorImpl.class);

    @Override
    public void validateSchema(InputStream xsdInputStream, InputStream xmlInputStream) {
        Source xsd = new StreamSource(xsdInputStream);
        Source xml = new StreamSource(xmlInputStream);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        Schema schema;
        try {
            schema = schemaFactory.newSchema(xsd);
        } catch (SAXException e) {
            throw new UsciException(e.getMessage());
        }

        Validator validator = schema.newValidator();

        try {
            validator.validate(xml);
        } catch (SAXParseException e) {
            int line = e.getLineNumber();
            int col = e.getColumnNumber();
            logger.error(String.format("XML не прошёл проверку XSD: Line %d Column %d %s", line, col, e.getMessage()));
            throw new ReceiverException(String.format("Line %d Column %d %s", line, col, e.getMessage()));
        } catch (SAXException | IOException e) {
            logger.error("XML не прошёл проверку XSD: {}", e.getMessage());
            throw new ReceiverException(e.getMessage());
        }
    }

}
