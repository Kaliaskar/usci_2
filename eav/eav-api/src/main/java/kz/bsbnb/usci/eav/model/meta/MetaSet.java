package kz.bsbnb.usci.eav.model.meta;

import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.persistence.Persistable;

import java.util.Objects;

/**
 * @author Artur Tkachenko
 * @author Alexandr Motov
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 */

public class MetaSet extends Persistable implements MetaType {
    private static final long serialVersionUID = -8685213083933324775L;

    private MetaType metaType;
    private SetKeyType keyType = SetKeyType.ALL;
    private String nestedTable;

    public MetaSet() {
        /*Пустой конструктор*/
    }

    public MetaSet(MetaType metaType) {
        Objects.requireNonNull(metaType, "MetaType не может быть null");

        this.metaType = metaType;
    }

    @Override
    public boolean isSet() {
        return true;
    }

    @Override
    public boolean isComplex() {
        return metaType.isComplex();
    }

    @Override
    public boolean isDictionary() {
        return metaType.isDictionary();
    }

    public void setMetaType(MetaType metaType) {
        Objects.requireNonNull(metaType, "MetaType не может быть null");

        this.metaType = metaType;
    }

    public MetaType getMetaType() {
        return metaType;
    }

    public SetKeyType getKeyType() {
        return keyType;
    }

    public void setKeyType(SetKeyType keyType) {
        this.keyType = keyType;
    }

    public MetaDataType getMetaDataType() {
        if (isComplex() || metaType.isSet())
            throw new UsciException("Массив не простой");

        return ((MetaValue) metaType).getMetaDataType();
    }

    public String getNestedTable() {
        return nestedTable;
    }

    public void setNestedTable(String nestedTable) {
        this.nestedTable = nestedTable;
    }

    @Override
    public String toString() {
        return toString("");
    }

    @Override
    public String toString(String prefix) {
        return "metaSet(" + getId() + "), " + metaType.toString(prefix);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MetaSet)) return false;

        MetaSet metaSet = (MetaSet) o;

        if (!metaType.equals(metaSet.metaType)) return false;
        if (keyType != metaSet.keyType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = metaType.hashCode();
        result = 31 * result + (keyType != null ? keyType.hashCode() : 0);
        return result;
    }

}
