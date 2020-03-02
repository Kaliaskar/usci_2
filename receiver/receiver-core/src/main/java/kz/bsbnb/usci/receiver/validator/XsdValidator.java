package kz.bsbnb.usci.receiver.validator;

import java.io.InputStream;

/**
 * @author Yernur Bakash
 */

public interface XsdValidator {

    void validateSchema(InputStream xsdInputStream, InputStream xmlInputStream);

}
