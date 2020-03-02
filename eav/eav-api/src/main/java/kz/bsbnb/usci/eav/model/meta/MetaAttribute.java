package kz.bsbnb.usci.eav.model.meta;

import kz.bsbnb.usci.model.persistence.Persistable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * @author Artur Tkachenko
 * @author Alexandr Motov
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 */

public class MetaAttribute extends Persistable implements Serializable {
    private static final long serialVersionUID = 1L;

    private LocalDate reportDate;
    private String name;
    private String title;
    private MetaType metaType;
    private Byte keyType = 0;
    private Short keySet = 1;
    private boolean isDeleted = false;
    private boolean parentIsKey = false;
    private boolean isReference = false;
    private boolean isCumulative = false;
    private boolean isRequired = false;
    private boolean isNullify = true;
    private String columnName;
    private String columnType;
    private boolean isSync = false;

    public MetaAttribute() {
        /*Пустой конструктор*/
    }

    public MetaAttribute(Long id) {
        super(id);
    }

    public MetaAttribute(Long id, MetaType metaType) {
        super(id);
        this.metaType = metaType;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public Byte getKeyType() {
        return keyType;
    }

    public void setKeyType(Byte keyType) {
        this.keyType = keyType;
    }

    /**
     * Признак обязательного не пустого ключа
     */
    public boolean isKey() {
        return keyType == 1;
    }

    /**
     * Признак ключа который может быть пустым
     */
    public boolean isNullableKey() {
        return keyType == 2;
    }

    public void setKeySet(Short keySet) {
        this.keySet = keySet;
    }

    public Short getKeySet() {
        return keySet;
    }

    public boolean isNullify() {
        return isNullify;
    }

    public void setNullify(boolean isNullable) {
        this.isNullify = isNullable;
    }

    public MetaType getMetaType() {
        return metaType;
    }

    public void setMetaType(MetaType metaType) {
        this.metaType = metaType;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        if (title != null) return title;
        return getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCumulative() {
        return isCumulative;
    }

    public void setCumulative(boolean isCumulative) {
        this.isCumulative = isCumulative;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public boolean isReference() {
        return isReference;
    }

    public void setReference(boolean reference) {
        isReference = reference;
    }

    public boolean isParentIsKey() {
        return parentIsKey;
    }

    public void setParentIsKey(boolean parentIsKey) {
        this.parentIsKey = parentIsKey;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    public boolean isSync() {
        return isSync;
    }

    public void setSync(boolean sync) {
        isSync = sync;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MetaAttribute that = (MetaAttribute) o;

        if (!keyType.equals(that.keyType)) return false;
        if (!keySet.equals(that.keySet)) return false;
        if (parentIsKey != that.parentIsKey) return false;
        if (isReference != that.isReference) return false;
        if (isRequired != that.isRequired) return false;
        if (isCumulative != that.isCumulative) return false;
        if (isNullify != that.isNullify) return false;
        if (!name.equals(that.name)) return false;
        if (!title.equals(that.title)) return false;
        if (!Objects.equals(columnName, that.columnName)) return false;
        if (!Objects.equals(columnType, that.columnType)) return false;

        return metaType.equals(that.metaType);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + metaType.hashCode();
        result = 31 * result + keyType.hashCode();
        result = 31 * result + keySet.hashCode();
        result = 31 * result + (parentIsKey ? 1 : 0);
        result = 31 * result + (isReference ? 1 : 0);
        result = 31 * result + (isCumulative ? 1 : 0);
        result = 31 * result + (isNullify ? 1 : 0);
        result = 31 * result + (isRequired ? 1 : 0);

        if (columnName != null)
            result = 31 * result + columnName.hashCode();
        if (columnType != null)
            result = 31 * result + columnType.hashCode();

        return result;
    }

    @Override
    public String toString() {
        return "MetaAttribute{" +
                "name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", metaType=" + metaType +
                ", keyType=" + keyType +
                ", keySet=" + keySet +
                ", parentIsKey=" + parentIsKey +
                ", isReference=" + isReference +
                ", isCumulative=" + isCumulative +
                ", isNullify=" + isNullify +
                ", columnName='" + columnName + '\'' +
                ", columnType='" + columnType + '\'' +
                ", id=" + id +
                '}';
    }
}



