package kz.bsbnb.usci.eav.model.meta;

import kz.bsbnb.usci.model.persistence.Persistable;
import kz.bsbnb.usci.model.ui.UiConfig;

import java.time.LocalDate;
import java.util.*;

/**
 * @author Artur Tkachenko
 * @author Alexandr Motov
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 */

public class MetaClass extends Persistable implements MetaType {
    private static final long serialVersionUID = 1L;

    private String className;
    private String classTitle;
    private LocalDate reportDate;
    private String schemaData;
    private String schemaXml;
    private String sequenceName;
    private String tableName;
    private Short hashSize = 1024;
    private boolean isDeleted;
    private boolean isSearchable = false;
    private boolean isDictionary = false;
    private boolean isOperational = false;
    private Long productId;
    private Map<String, MetaAttribute> attributes = new HashMap<>();
    private boolean isSync = false;
    private PeriodType periodType = PeriodType.CONSTANT;
    private UiConfig uiConfig;

    public MetaClass() {
        super();
        this.reportDate = LocalDate.now();
    }

    public MetaClass(long id) {
        super(id);
        this.reportDate = LocalDate.now();
    }

    public MetaClass(String className) {
        this.className = className;
        this.reportDate = LocalDate.now();
    }

    public MetaClass(MetaClass meta) {
        super(meta.id);

        this.className = meta.className;
        this.classTitle = meta.classTitle;
        this.reportDate = meta.reportDate;
        this.schemaData = meta.schemaData;
        this.schemaXml = meta.schemaXml;
        this.sequenceName = meta.sequenceName;
        this.tableName = meta.tableName;
        this.hashSize = meta.hashSize;
        this.isDeleted = meta.isDeleted;
        this.isSearchable = meta.isSearchable;
        this.isDictionary = meta.isDictionary;
        this.isOperational = meta.isOperational;

        attributes.putAll(meta.attributes);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public String getSchemaData() {
        return schemaData;
    }

    public void setSchemaData(String schemaData) {
        this.schemaData = schemaData;
    }

    public String getSchemaXml() {
        return schemaXml;
    }

    public void setSchemaXml(String schemaXml) {
        this.schemaXml = schemaXml;
    }

    public void removeAttribute(String name) {
        attributes.remove(name);

        isSearchable = false;

        for (MetaAttribute metaAttribute : attributes.values()) {
            if (metaAttribute.isKey()) {
                isSearchable = true;
                break;
            }
        }
    }

    public void setMetaAttribute(String name, MetaAttribute metaAttribute) {
        if (!isSearchable && metaAttribute.isKey())
            isSearchable = metaAttribute.isKey();

        attributes.put(name, metaAttribute);
        metaAttribute.setName(name);
    }

    public void removeAttributes() {
        isSearchable = false;
        attributes.clear();
    }

    public String getSequenceName() {
        return sequenceName;
    }

    public void setSequenceName(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    public boolean isOperational() {
        return isOperational;
    }

    public void setOperational(boolean operational) {
        this.isOperational = operational;
    }

    public PeriodType getPeriodType() {
        return periodType;
    }

    public void setPeriodType(PeriodType periodType) {
        this.periodType = periodType;
    }

    public Collection<MetaAttribute> getAttributes() {
        return attributes.values();
    }

    public MetaType getEl(String path) {
        StringTokenizer tokenizer = new StringTokenizer(path, ".");

        MetaClass meta = this;
        MetaType valueOut = null;

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();

            MetaAttribute attribute = meta.getMetaAttribute(token);

            if (attribute == null)
                return null;

            MetaType type = attribute.getMetaType();

            valueOut = type;

            if (type.isSet()) {
                while (type.isSet()) {
                    valueOut = type;
                    type = ((MetaSet) type).getMetaType();
                }
            }

            if (valueOut.isComplex()) {
                if (!valueOut.isSet()) {
                    meta = (MetaClass) valueOut;
                } else {
                    meta = (MetaClass) type;
                }
            } else {
                if (tokenizer.hasMoreTokens()) {
                    throw new IllegalArgumentException("Неправильный путь/Путь не может иметь простые элементы");
                }
            }
        }

        return valueOut;
    }

    public MetaAttribute getElAttribute(String path) {
        StringTokenizer tokenizer = new StringTokenizer(path, ".");

        MetaClass meta = this;
        MetaType valueOut;
        MetaAttribute attribute = null;

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();

            attribute = meta.getMetaAttribute(token);

            if (attribute == null)
                return null;

            MetaType type = attribute.getMetaType();

            valueOut = type;

            if (type.isSet()) {
                while (type.isSet()) {
                    valueOut = type;
                    type = ((MetaSet) type).getMetaType();
                }
            }

            if (valueOut.isComplex()) {
                if (!valueOut.isSet()) {
                    meta = (MetaClass) valueOut;
                } else {
                    meta = (MetaClass) type;
                }
            } else {
                if (tokenizer.hasMoreTokens())
                    throw new IllegalArgumentException("Неправильный путь/Путь не может иметь простые элементы");
            }
        }

        return attribute;
    }

    @Override
    public boolean isSet() {
        return false;
    }

    @Override
    public boolean isComplex() {
        return true;
    }

    public boolean isSearchable() {
        return isSearchable;
    }

    @Override
    public boolean isDictionary() {
        return isDictionary;
    }

    public void setDictionary(boolean value) {
        isDictionary = value;
    }

    public String getClassTitle() {
        return classTitle;
    }

    public void setClassTitle(String classTitle) {
        this.classTitle = classTitle;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public boolean isSync() {
        return isSync;
    }

    public void setSync(boolean sync) {
        isSync = sync;
    }

    public Short getHashSize() {
        return hashSize;
    }

    public void setHashSize(Short hashSize) {
        this.hashSize = hashSize;
    }

    public Set<String> getAttributeNames() {
        return attributes.keySet();
    }

    public MetaAttribute getMetaAttribute(String name) {
        return attributes.get(name);
    }

    public MetaType getAttributeType(String name) {
        MetaAttribute metaAttribute = attributes.get(name);

        if (metaAttribute == null)
            throw new IllegalArgumentException(String.format("Атрибут: %s не найден в мета классе: %s", name, this.getClassName()));

        return metaAttribute.getMetaType();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || getClass() != obj.getClass())
            return false;

        MetaClass tmp = (MetaClass) obj;
        if (this.getId() != null && tmp.getId() != null && this.getId().equals(tmp.getId()))
            return true;

        if (tmp.getAttributes().size() != this.getAttributes().size())
            return false;

        Set<String> thisNames = this.getAttributeNames();
        for (String name : thisNames)
            if (!this.getAttributeType(name).equals(tmp.getAttributeType(name)))
                return false;

        return tmp.getReportDate().equals(this.getReportDate()) &&
               tmp.getClassName().equals(this.getClassName());
    }
    
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + className.hashCode();
        result = 31 * result + reportDate.hashCode();
        result = 31 * result + attributes.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return toString("");
    }

    @Override
    protected Object clone() {
        MetaClass meta = new MetaClass();

        meta.className = this.className;
        meta.classTitle = this.classTitle;
        meta.reportDate = this.reportDate;
        meta.isDictionary = this.isDictionary;
        meta.sequenceName = this.sequenceName;
        meta.tableName = this.tableName;
        meta.schemaData = this.schemaData;
        meta.schemaXml = this.schemaXml;
        meta.isOperational = this.isOperational;
        meta.hashSize = this.hashSize;

        meta.attributes.putAll(this.attributes);

        return meta;
    }

    @Override
    public String toString(String prefix) {
        StringBuilder str = new StringBuilder(className + ":metaClass(" + getId() + "_" + isSearchable + ");");

        String[] names;

        names = attributes.keySet().toArray(new String[0]);

        Arrays.sort(names);

        for (String memberName : names) {
            MetaAttribute attribute = attributes.get(memberName);
            MetaType type = attribute.getMetaType();

            String key = "";

            if (attribute.isKey()) key = "*";

            str.append("\n")
                .append(prefix)
                .append(memberName)
                .append("(").append(attribute.getId()).append(")")
                .append(key).append(": ")
                .append(type.toString(prefix + "\t"));
        }

        return str.toString();
    }

    public UiConfig getUiConfig() {
        return uiConfig;
    }

    public void setUiConfig(UiConfig uiConfig) {
        this.uiConfig = uiConfig;
    }
}
