package kz.bsbnb.usci.model;

import java.time.format.DateTimeFormatter;

/**
 * @author Jandos Iskakov
 */

public class Constants {
    public static final String DATE_FORMAT_ISO = "yyyy-MM-dd";
    public static final DateTimeFormatter DATE_FORMATTER_ISO = DateTimeFormatter.ofPattern(DATE_FORMAT_ISO);

    public static final Long NBK_AS_RESPONDENT_ID = 0L;

    public static final String NBRK_BIN = "941240001151";

    public static final String DOC_TYPE_BIN = "07";
    public static final String DOC_TYPE_RNN = "11";
    public static final String DOC_TYPE_BIK = "15";

    public static final int ZIP_BUFFER_SIZE = 1024;

    public static final int MAX_SYNC_QUEUE_SIZE = 2028;

    public static final int SYNC_RMI_PORT = 1091;

}
