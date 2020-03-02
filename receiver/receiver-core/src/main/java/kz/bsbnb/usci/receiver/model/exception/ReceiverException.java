package kz.bsbnb.usci.receiver.model.exception;

import kz.bsbnb.usci.model.exception.UsciException;

/**
 * @author Jandos Iskakov
 */

public class ReceiverException extends UsciException {

    public ReceiverException() {
    }

    public ReceiverException(String message) {
        super(message);
    }

}
