package kz.bsbnb.usci.wsclient.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.WebServiceMessage;
import org.springframework.xml.transform.TransformerObjectSupport;

import java.io.ByteArrayOutputStream;

public class HttpLoggingUtils extends TransformerObjectSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpLoggingUtils.class);

    private HttpLoggingUtils() {}

    public static String getMessage(WebServiceMessage webServiceMessage) {
        try {
            ByteArrayOutputStream byteArrayOutputStream =
                    new ByteArrayOutputStream();
            webServiceMessage.writeTo(byteArrayOutputStream);

            return new String(byteArrayOutputStream.toByteArray());
        } catch (Exception e) {
            LOGGER.error("Unable to get HTTP message.", e);
            return null;
        }
    }
}