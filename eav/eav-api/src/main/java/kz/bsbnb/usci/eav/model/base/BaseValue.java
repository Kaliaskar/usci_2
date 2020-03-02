package kz.bsbnb.usci.eav.model.base;

import kz.bsbnb.usci.eav.model.meta.MetaAttribute;
import kz.bsbnb.usci.eav.model.meta.MetaDataType;
import kz.bsbnb.usci.eav.model.meta.MetaSet;
import kz.bsbnb.usci.eav.model.meta.MetaType;
import kz.bsbnb.usci.model.exception.UsciException;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * @author Artur Tkachenko
 * @author Alexandr Motov
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 */

public class BaseValue implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    private final UUID uuid = UUID.randomUUID();
    private MetaAttribute metaAttribute;
    private BaseValue newBaseValue = null;
    private BaseValue oldBaseValue = null;
    private Object value;
    private Boolean changed = Boolean.FALSE;

    public BaseValue() {
        /*Пустой конструктор*/
    }

    public BaseValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setNewBaseValue(BaseValue newBaseValue) {
        this.newBaseValue = newBaseValue;
    }

    public BaseValue getNewBaseValue() {
        return newBaseValue;
    }

    public void setOldBaseValue(BaseValue newBaseValue) {
        this.oldBaseValue = newBaseValue;
    }

    public BaseValue getOldBaseValue() {
        return oldBaseValue;
    }

    public MetaAttribute getMetaAttribute() {
        return metaAttribute;
    }

    public void setMetaAttribute(MetaAttribute metaAttribute) {
        this.metaAttribute = metaAttribute;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Boolean isChanged() {
        return changed;
    }

    public void setChanged(Boolean changed) {
        this.changed = changed;
    }

    public Boolean getMock() {
        if (value instanceof BaseEntity)
            return ((BaseEntity) value).getMock();

        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || (getClass() != obj.getClass()))
            return false;

        BaseValue that = (BaseValue) obj;
        return value != null ? value.equals(that.value) : that.value == null;
    }

    public boolean equalsToString(String str, MetaDataType type) {
        switch (type) {
            case INTEGER:
                return value.equals(Integer.parseInt(str));
            case DATE:
                throw new UnsupportedOperationException("Дата не поддерживается");
            case STRING:
                return value.equals(str);
            case BOOLEAN:
                return value.equals(Boolean.parseBoolean(str));
            case DOUBLE:
                return value.equals(Double.parseDouble(str));
            default:
                throw new IllegalStateException(String.format("Неизвестный тип данных %s", type));
        }
    }

    public boolean equalsByValue(BaseValue baseValue) {
        MetaAttribute thisMetaAttribute = this.getMetaAttribute();
        MetaAttribute thatMetaAttribute = baseValue.getMetaAttribute();

        if (thisMetaAttribute == null || thatMetaAttribute == null)
            throw new IllegalStateException("Сравнение значений двух объектов BaseValue без метаданных невозможно");

        return thisMetaAttribute.getId().equals(thatMetaAttribute.getId()) &&
                this.equalsByValue(thisMetaAttribute.getMetaType(), baseValue, thisMetaAttribute);
    }

    public boolean equalsByValue(MetaType metaType, BaseValue baseValue, MetaAttribute metaAttribute) {
        Object thisValue = this.getValue();
        Object thatValue = baseValue.getValue();

        if (thisValue == null || thatValue == null)
            throw new UsciException("Сравнение значений двух объектов BaseValue с значениями null невозможно");

        if (metaType.isComplex()) {
            if (metaType.isSet()) {
                BaseSet thisBaseSet = (BaseSet) thisValue;
                BaseSet thatBaseSet = (BaseSet) thatValue;

                if (thisBaseSet.getValueCount() != thatBaseSet.getValueCount() && !metaAttribute.isCumulative())
                    return false;

                boolean baseValueNotFound;
                Set<UUID> processedUuids = new HashSet<>();
                for (BaseValue thisBaseValue : thisBaseSet.getValues()) {
                    BaseEntity thisBaseSetValue = (BaseEntity) thisBaseValue.getValue();

                    baseValueNotFound = true;
                    for (BaseValue thatBaseValue : thatBaseSet.getValues()) {
                        if (processedUuids.contains(thatBaseValue.getUuid()))
                            continue;

                        BaseEntity thatBaseSetValue = (BaseEntity) thatBaseValue.getValue();

                        if (thisBaseSetValue.equals(thatBaseSetValue) || Objects.equals(thisBaseSetValue.getId(), thatBaseSetValue.getId())) {
                            processedUuids.add(thatBaseValue.getUuid());
                            baseValueNotFound = false;
                            break;
                        }
                    }

                    if (baseValueNotFound)
                        return false;
                }

                return true;
            } else {
                BaseEntity thisBaseEntity = (BaseEntity) thisValue;
                BaseEntity thatBaseEntity = (BaseEntity) thatValue;
                return Objects.equals(thisBaseEntity.getId(), thatBaseEntity.getId());
            }
        } else {
            if (metaType.isSet()) {
                MetaSet metaSet = (MetaSet) metaType;
                MetaType childMetaType = metaSet.getMetaType();

                BaseSet thisBaseSet = (BaseSet) thisValue;
                BaseSet thatBaseSet = (BaseSet) thatValue;

                if (thisBaseSet.getValueCount() != thatBaseSet.getValueCount())
                    return false;

                boolean baseValueNotFound;
                Set<UUID> processedUuids = new HashSet<>();
                for (BaseValue thisBaseValue : thisBaseSet.getValues()) {
                    baseValueNotFound = true;
                    for (BaseValue thatBaseValue : thatBaseSet.getValues()) {
                        if (processedUuids.contains(thatBaseValue.getUuid()))
                            continue;

                        if (thisBaseValue.equalsByValue(childMetaType, thatBaseValue, metaAttribute)) {
                            processedUuids.add(thatBaseValue.getUuid());
                            baseValueNotFound = false;
                            break;
                        }
                    }

                    if (baseValueNotFound)
                        return false;
                }

                return true;
            } else {
                return Objects.equals(thisValue, thatValue);
            }
        }
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public BaseValue clone() {
        BaseValue baseValue;
        try {
            baseValue = (BaseValue) super.clone();

            if (value != null) {
                if (value instanceof BaseEntity)
                    baseValue.setValue(((BaseEntity) value).clone());
                if (value instanceof BaseSet)
                    baseValue.setValue(((BaseSet) value).clone());
            }
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException("BaseValue класс не реализует интерфейс Cloneable");
        }

        return baseValue;
    }

    @Override
    public String toString() {
        if (value == null)
            return null;

        return value.toString();
    }

}


