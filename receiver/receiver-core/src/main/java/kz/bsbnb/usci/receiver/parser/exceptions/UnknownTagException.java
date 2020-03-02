package kz.bsbnb.usci.receiver.parser.exceptions;

import org.xml.sax.SAXException;

/**
 * @author Kanat Tulbassiev
 */

public class UnknownTagException extends SAXException {
    private static final long serialVersionUID = 1L;

    public UnknownTagException(String tagName) {
        super("Неизвестный тэг: " + tagName);
    }
}
