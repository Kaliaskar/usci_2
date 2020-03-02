package kz.bsbnb.usci.eav;

import kz.bsbnb.usci.eav.model.meta.MetaAttribute;
import kz.bsbnb.usci.eav.model.meta.MetaType;
import kz.bsbnb.usci.eav.model.meta.MetaValue;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.util.SqlJdbcConverter;

/**
 * @author Jandos Iskakov
 */

public class EavSqlConverter {

    /**
     * метод конвертирует значение sql jdbc формата в формат java eav (удобный нам формат)
     * пояснения: BOOLEAN в таблицах БД хранится в формате varchar2(1)
     * дата конвертируется из java.sql.Timestamp в LocalDate
     * числа хранятся в БД как NUMBER, конвертируются из BigDecimal в Long, Double и тд
     * String получаем как есть (varchar2)
     * */
    public static Object convertSqlValueToJavaType(MetaAttribute attribute, Object sqlValue) {
        MetaType metaType = attribute.getMetaType();

        if (metaType.isComplex() || metaType.isSet())
            throw new UsciException("Метод предназначен только для конвертаций примитивных типов данных");

        if (sqlValue == null)
            throw new UsciException("Ошибка значения NULL");

        Object javaValue;

        MetaValue metaValue = (MetaValue) metaType;

        switch (metaValue.getMetaDataType()) {
            case DATE:
                javaValue = SqlJdbcConverter.convertToLocalDate((java.sql.Timestamp)sqlValue);
                break;
            case DATE_TIME:
                javaValue = SqlJdbcConverter.convertToLocalDateTime((java.sql.Timestamp)sqlValue);
                break;
            case BOOLEAN:
                javaValue = sqlValue.equals("1")? Boolean.TRUE: Boolean.FALSE;
                break;
            case DOUBLE:
                javaValue = SqlJdbcConverter.convertToDouble(sqlValue);
                break;
            case INTEGER:
                javaValue = SqlJdbcConverter.convertToInt(sqlValue);
                break;
            case STRING:
                javaValue = sqlValue;
                break;
            default:
                throw new UsciException("Не верный тип мета данных");
        }

        return javaValue;
    }

}
