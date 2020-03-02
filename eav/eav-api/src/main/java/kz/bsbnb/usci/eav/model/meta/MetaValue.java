package kz.bsbnb.usci.eav.model.meta;

/**
 * @author Artur Tkachenko
 * @author Alexandr Motov
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 */

public class MetaValue implements MetaType {
    private static final long serialVersionUID = -6508043791119539004L;

    private MetaDataType metaDataType;

    public MetaValue() {
        /*Пустой конструктор*/
    }

    public MetaValue(MetaDataType metaDataType) {
        this.metaDataType = metaDataType;
    }

    public MetaDataType getMetaDataType() {
        return metaDataType;
    }

    public void setMetaDataType(MetaDataType type) {
        this.metaDataType = type;
    }

    @Override
    public boolean isSet() {
        return false;
    }

    @Override
    public boolean isComplex() {
        return false;
    }

    @Override
    public boolean isDictionary() {
        return false;
    }

    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || getClass() != obj.getClass())
            return false;

        MetaValue tmp = (MetaValue) obj;
        return tmp.getMetaDataType() == this.getMetaDataType();
    }

    public int hashCode() {
        return metaDataType.hashCode();
    }

    @Override
    public String toString() {
        return toString("");
    }

    @Override
    public String toString(String prefix) {
        return "metaValue: " + metaDataType;
    }

}

