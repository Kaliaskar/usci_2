package kz.bsbnb.usci.eav.model.base;

import kz.bsbnb.usci.eav.model.meta.*;

/**
 * @author Artur Tkachenko
 * @author Alexandr Motov
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 */

public class BaseEntityOutput {

    public static String toString(BaseEntity entity, boolean isShort) {
        return toString(entity, "", isShort);
    }

    public static String toString(BaseEntity baseEntity, String prefix, boolean isShort) {
        if (baseEntity == null) return "null";

        StringBuilder str = new StringBuilder(baseEntity.getMetaClass().getClassName() + "|" + baseEntity.getMetaClass().getClassTitle() + "(" + baseEntity.getId() + ", ");
        str.append(baseEntity.getReportDate() == null ? "-" : MetaDataType.formatDate(baseEntity.getReportDate()) + ", ");
        str.append(baseEntity.getBatchId() == null ? "-)" : baseEntity.getBatchId() + ");");

        MetaClass metaClass = baseEntity.getMetaClass();

        if (!isShort) {
            for (String attrName : metaClass.getAttributeNames()) {
                MetaAttribute metaAttribute = metaClass.getMetaAttribute(attrName);
                MetaType metaType = metaAttribute.getMetaType();

                if (metaClass.isDictionary() && !metaAttribute.isKey())
                    continue;

                BaseValue baseValue = baseEntity.getBaseValue(attrName);

                String valueToString = "null";

                if (baseValue == null) {
                    valueToString = "not set";
                } else if (baseValue.getValue() == null) {
                    valueToString = "null";
                }

                if (baseValue != null && baseValue.getValue() != null) {
                    if (metaType.isComplex()) {
                        if (!metaType.isSet()) {
                            valueToString = toString((BaseEntity) baseValue.getValue(), prefix + "\t", false);
                        } else {
                            valueToString = complexSet((BaseSet) baseValue.getValue(), prefix + "\t", (MetaSet) metaType);
                        }
                    } else {
                        valueToString = baseValue.getValue().toString();
                    }
                }

                if (baseValue != null) {
                    if (metaAttribute.isKey()) {
                        str.append("\n").append(prefix).append(attrName).append(" : ");
                    } else {
                        str.append("\n").append(prefix).append(attrName).append(" : ");
                    }

                    str.append(valueToString);
                }
            }
        }

        return str.toString();
    }

    private static String complexSet(BaseSet set, String prefix, MetaSet metaSet) {
        StringBuilder str = new StringBuilder();

        for (BaseValue value : set.getValues()) {
            if (metaSet.isSet()) {
                if (metaSet.isComplex()) {
                    str.append("\n").append(prefix).append(toString((BaseEntity) value.getValue(), prefix + "\t", false));
                } else {
                    str.append(value.getValue().toString());
                }
            }
        }

        return str.toString();
    }

    /**
     * Выводит сущности в UI читабельном виде для юзеров
     * Берем все ключевые поля и возвращаем строку
     */
    public static String getEntityAsString(BaseEntity entity, boolean asRoot) {
        if (entity == null) return null;

        MetaClass metaClass = entity.getMetaClass();

        String classTitle = metaClass.getClassTitle() != null? metaClass.getClassTitle(): metaClass.getClassName();
        if (!metaClass.isSearchable()) {
            return entity.getId() != null? (classTitle + " = " + entity.getId()): classTitle;
        }

        StringBuilder str = new StringBuilder(asRoot? classTitle: "");
        str.append("(");

        boolean isFirst = true;

        for (String attrName : metaClass.getAttributeNames()) {
            MetaAttribute attribute = metaClass.getMetaAttribute(attrName);
            MetaType type = attribute.getMetaType();

            if (!attribute.isKey() || attribute.getName().equals("ref_respondent"))
                continue;

            BaseValue value;
            if (entity.getBaseValue(attrName) != null) {
                value = entity.getBaseValue(attrName).clone();
            } else {
                value = null;
            }
            if (value != null) {
                if (value.getNewBaseValue() != null)
                    value = value.getNewBaseValue().clone();
            }

            String valueToString = "null";
            boolean valueIsNull = false;

            if (value == null) {
                valueToString = "not set";
                valueIsNull = true;
            } else {
                if (value.getValue() == null) {
                    valueToString = "null";
                }
            }

            if (value != null && value.getValue() != null) {
                if (type.isComplex()) {
                    if (!type.isSet()) {
                        valueToString = getEntityAsString((BaseEntity) value.getValue(), false);
                    } else {
                        valueToString = getComplexSetAsString((BaseSet) value.getValue(), (MetaSet) type);
                    }
                } else {
                    valueToString = MetaDataType.toString(((MetaValue) type).getMetaDataType(), value.getValue());
                }
            }

            if (!valueIsNull) {
                String attrTitle = attribute.getTitle() != null? attribute.getTitle(): attrName;

                if (!isFirst)
                    str.append(", ");

                str.append(attrTitle).append(": ").append(valueToString);
            }

            isFirst = false;
        }

        str.append(")");

        return str.toString();
    }

    private static String getComplexSetAsString(BaseSet set, MetaSet metaSet) {
        StringBuilder str = new StringBuilder();

        str.append("[");

        boolean isFirst = true;
        for (BaseValue value : set.getValues()) {
            if (!isFirst)
                str.append(",");

            if (metaSet.isSet()) {
                if (metaSet.isComplex()) {
                    str.append(getEntityAsString((BaseEntity) value.getValue(), false));
                } else {
                    str.append(value.getValue().toString());
                }
            }

            isFirst = false;
        }

        str.append("]");

        return str.toString();
    }

}
