package kz.bsbnb.usci.eav.model.meta;

import kz.bsbnb.usci.eav.model.base.BaseValue;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import static kz.bsbnb.usci.model.Constants.DATE_FORMAT_ISO;

/**
 * @author Artur Tkachenko
 * @author Alexandr Motov
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 */

public enum MetaDataType {
    INTEGER,
    DATE,
    DATE_TIME,
    STRING,
    BOOLEAN,
    DOUBLE;

    private static final Logger logger = LoggerFactory.getLogger(MetaDataType.class);

    private static final String DATE_FORMAT_DOT = "dd.MM.yyyy";
    private static final String DATE_TIME_FORMAT_DOT = "dd.MM.yyyy HH:mm:ss";

    private static final DateTimeFormatter dateFormatSlash = DateTimeFormatter.ofPattern(DATE_FORMAT_ISO);
    private static final DateTimeFormatter dateFormatDot = DateTimeFormatter.ofPattern(DATE_FORMAT_DOT);
    private static final DateTimeFormatter dateTimeFormatDot = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_DOT);

    public synchronized static LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s, dateFormatDot);
        } catch (DateTimeParseException e) {
            return LocalDate.parse(s, dateFormatSlash);
        }
    }

    public synchronized static LocalDate parseSplashDate(String s) {
        return LocalDate.parse(s, dateFormatSlash);
    }

    public synchronized static String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static Class<?> getDataTypeClass(MetaDataType dataType) {
        switch (dataType) {
            case INTEGER:
                return Integer.class;
            case DATE:
                return LocalDate.class;
            case DATE_TIME:
                return LocalDateTime.class;
            case STRING:
                return String.class;
            case BOOLEAN:
                return Boolean.class;
            case DOUBLE:
                return Double.class;
            default:
                throw new IllegalArgumentException("Неизвестный тип. Не может быть возвращен соответствующий класс.");
        }
    }

    /**
     * конвертация переменной примитивного типа в тип реляционной модели
     - boolean значения конвертируются в varchar2(1)
     - LocalDate конвертируется в sql.date
     - LocalDateTime конвертируется в sql.timestamp
     - остальные значения остаются как есть
    */
    public static Object convertMetaValueToSqlValue(MetaDataType metaDataType, Object value) {
        switch (metaDataType) {
            case DATE:
                if (value instanceof BaseValue) {
                    return SqlJdbcConverter.convertToSqlDate((LocalDate)((BaseValue) value).getValue());
                } else {
                    return SqlJdbcConverter.convertToSqlDate((LocalDate)value);
                }
            case DATE_TIME:
                if (value instanceof BaseValue) {
                    return SqlJdbcConverter.convertToSqlTimestamp((LocalDateTime)((BaseValue) value).getValue());
                } else {
                    return SqlJdbcConverter.convertToSqlTimestamp((LocalDateTime)value);
                }
            case BOOLEAN:
                if (value instanceof BaseValue) {
                    return String.valueOf((Boolean)((BaseValue) value).getValue()? 1: 0);
                } else {
                    return String.valueOf((Boolean)value ? 1: 0);
                }
            default:
                if (value instanceof BaseValue) {
                    return ((BaseValue) value).getValue();
                } else {
                    return value;
                }
        }
    }

    public Class<?> getDataTypeClass() {
        return getDataTypeClass(this);
    }

    public static Object getCastObject(MetaDataType typeCode, String value) {
        switch (typeCode) {
            case INTEGER:
                if (value.contains(".")) {
                    value = value.substring(0,value.indexOf("."));
                    return Integer.parseInt(value);
                } else {
                    return Integer.parseInt(value);
                }
            case DATE:
                LocalDate date = null;

                try {
                    date = LocalDate.parse(value, dateFormatSlash);
                } catch (DateTimeParseException e) {
                    try {
                        date = LocalDate.parse(value, dateFormatDot);
                    } catch (DateTimeParseException ex) {
                        logger.error("Ошибка парсинга даты", ex);
                    }
                }

                return date;
            case DATE_TIME:
                LocalDateTime dateTime = null;

                try {
                    dateTime = LocalDateTime.parse(value, dateTimeFormatDot);
                } catch (DateTimeParseException e) {
                    logger.error("Ошибка парсинга даты и времени", e);
                }

                return dateTime;
            case STRING:
                return value;
            case BOOLEAN:
                try {
                    return Integer.parseInt(value) == 1;
                } catch (NumberFormatException e) {
                    return Boolean.parseBoolean(value);
                }
            case DOUBLE:
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                throw new IllegalArgumentException("Неизвестный тип");
        }
    }

    public static String toString(MetaDataType dataType, Object value) {
        if (value == null)
            return "";

        switch (dataType) {
            case INTEGER:
                return String.valueOf(value);
            case DATE:
                return ((LocalDate) value).format(DateTimeFormatter.ISO_DATE);
            case DATE_TIME:
                return ((LocalDateTime) value).format(DateTimeFormatter.ISO_DATE_TIME);
            case STRING:
                return String.valueOf(value);
            case BOOLEAN:
                return String.valueOf(value);
            case DOUBLE:
                DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(Locale.US);
                formatSymbols.setDecimalSeparator('.');

                DecimalFormat df = new DecimalFormat("#.0#", formatSymbols);

                return df.format(value);
            default:
                throw new IllegalArgumentException("Неизвестный тип");
        }
    }

}
