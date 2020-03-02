package kz.bsbnb.usci.util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @author Jandos Iskakov
 */

public class SqlJdbcConverter {

    public static Timestamp convertToSqlTimestamp(LocalDateTime date) {
        return date == null? null: Timestamp.valueOf(date);
    }

    public static java.sql.Date convertToSqlDate(LocalDate date) {
        return date == null? null: java.sql.Date.valueOf(date);
    }

    public static java.sql.Date convertToSqlDate(java.util.Date date) {
        return date == null? null: new java.sql.Date(date.getTime());
    }

    public static java.sql.Date convertToSqlDate(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate localDate = LocalDate.parse(date,formatter);
        return (date == null || date.equals("")) ? null: java.sql.Date.valueOf(localDate);
    }

    public static Long convertToLong(Object value) {
        return value == null? null: (value instanceof BigDecimal)? ((BigDecimal)value).longValue() : (Long)value;
    }

    public static Integer convertToInt(Object value) {
        return value == null? null: (value instanceof BigDecimal)? ((BigDecimal)value).intValue() : (Integer)value;
    }

    public static Short convertToShort(Object value) {
        return value == null? null: (value instanceof BigDecimal)? ((BigDecimal)value).shortValue() : (Short)value;
    }

    public static Byte convertToByte(Object value) {
        return value == null? null: (value instanceof BigDecimal)? ((BigDecimal)value).byteValue() : (Byte)value;
    }

    public static Byte convertToByte(Boolean value) {
        return value ? Byte.valueOf("1") : 0;
    }

    public static Boolean convertToBoolean(Object value) {
        return value == null? null: convertToByte(value) == 1;
    }

    public static Double convertToDouble(Object value) {
        return value == null? null: (value instanceof BigDecimal)? ((BigDecimal)value).doubleValue() : (Double)value;
    }

    public static Byte convertBooleanToByte(Boolean value) {
        return value == null? null: value? (byte)1: (byte)0;
    }

    public static LocalDateTime convertToLocalDateTime(Timestamp timestamp) {
        return timestamp == null? null: timestamp.toLocalDateTime();
    }

    public static LocalDateTime convertToLocalDateTime(Object timestamp) {
        return timestamp == null? null: convertToLocalDateTime((Timestamp) timestamp);
    }

    public static LocalDate convertToLocalDate(java.sql.Date date) {
        return date == null? null: date.toLocalDate();
    }

    public static LocalDate convertToLocalDate(Object date) {
        return date == null? null: convertToLocalDate((Timestamp) date);
    }

    public static Date convertToDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDate convertToLocalDate(java.util.Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * Отдельный метод был добавлен так как String.valueOf возвращает "null" строку когда value = null
     */
    public static String convertObjectToString(Object value) {
        return value == null? null: (String) value;
    }

    public static LocalDate convertToLocalDate(java.sql.Timestamp date) {
        return date == null? null: date.toLocalDateTime().toLocalDate();
    }

    public static Boolean convertVarchar2ToBoolean(String string) {
        return string == null? null: (string.equals("1")? true: (string.equals("0")? false: null));
    }

    public static Boolean convertVarchar2ToBoolean(Object string) {
        return string == null? null: (string.equals("1")? true: (string.equals("0")? false: null));
    }

    public static String convertToString(LocalDate value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String formattedString = value.format(formatter);
        return value == null? null: formattedString;
    }

    public static LocalDate convertToLocalDate(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return date == null? null: LocalDate.parse(date, formatter);
    }

}
