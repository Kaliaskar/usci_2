package kz.bsbnb.usci.brms.exception;

import kz.bsbnb.usci.model.exception.UsciException;

import java.util.List;
import java.util.Set;

/**
 * @author Ernur Bakash
 */

public class BrmsException extends UsciException {

    public BrmsException(List<String> errorMessages) {
        super(errorMessages);
    }

    public BrmsException(Set<String> errorMessages) {
        super(errorMessages);
    }

}
