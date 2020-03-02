package kz.bsbnb.usci.eav.model.meta.json;

/**
 * @author Jandos Iskakov
 */

public class MetaAttributeJson {
    private Long id;
    private Long classId;
    private String name;
    private String title;
    private Byte type;//Тип атрибута: 0-simple, 1-complex,2-simple set,3-complex set
    private boolean array = false;
    private boolean simple = false;
    private boolean dictionary = false;
    private Byte keyType = 0;
    private Short keySet = 1;
    private boolean isKey = false;
    private boolean isRequired = false;
    private boolean isDeleted = false;
    private boolean parentIsKey = false;
    private boolean isReference = false;
    private boolean isCumulative = false;
    private boolean isNullify = false;
    private boolean nullableKey;
    private String setKeyType;
    private Long refClassId;
    private String refMetaType;// вид мета данных ссылки: META_CLASS, META_SET
    private String typeCode;// простые типы: DATE, DOUBLE, INTEGER, STRING
    private String metaType;// вид мета данных: META_CLASS, META_SET, DOUBLE, INTEGER, DATE
    private String attrPathPart;
    private boolean isSync = false;
    private boolean hidden = false;
    private String value;

    public Long getId() {
        return id;
    }

    public MetaAttributeJson setId(Long id) {
        this.id = id;
        return this;
    }

    public boolean isArray() {
        return array;
    }

    public MetaAttributeJson setArray(boolean array) {
        this.array = array;
        return this;
    }

    public boolean isSimple() {
        return simple;
    }

    public MetaAttributeJson setSimple(boolean simple) {
        this.simple = simple;
        return this;
    }

    public boolean isDictionary() {
        return dictionary;
    }

    public MetaAttributeJson setDictionary(boolean dictionary) {
        this.dictionary = dictionary;
        return this;
    }

    public Long getClassId() {
        return classId;
    }

    public MetaAttributeJson setClassId(Long classId) {
        this.classId = classId;
        return this;
    }

    public boolean isHidden() {
        return hidden;
    }

    public MetaAttributeJson setHidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public String getMetaType() {
        return metaType;
    }

    public MetaAttributeJson setMetaType(String metaType) {
        this.metaType = metaType;
        return this;
    }

    public String getName() {
        return name;
    }

    public MetaAttributeJson setName(String name) {
        this.name = name;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public MetaAttributeJson setTitle(String title) {
        this.title = title;
        return this;
    }

    public Byte getType() {
        return type;
    }

    public MetaAttributeJson setType(Byte type) {
        this.type = type;
        return this;
    }

    public Byte getKeyType() {
        return keyType;
    }

    public MetaAttributeJson setKeyType(Byte keyType) {
        this.keyType = keyType;
        return this;
    }

    public Short getKeySet() {
        return keySet;
    }

    public MetaAttributeJson setKeySet(Short keySet) {
        this.keySet = keySet;
        return this;
    }

    public boolean isKey() {
        return isKey;
    }

    public MetaAttributeJson setKey(boolean key) {
        isKey = key;
        return this;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public MetaAttributeJson setDeleted(boolean deleted) {
        isDeleted = deleted;
        return this;
    }

    public boolean isParentIsKey() {
        return parentIsKey;
    }

    public MetaAttributeJson setParentIsKey(boolean parentIsKey) {
        this.parentIsKey = parentIsKey;
        return this;
    }

    public boolean isReference() {
        return isReference;
    }

    public MetaAttributeJson setReference(boolean reference) {
        isReference = reference;
        return this;
    }

    public boolean isCumulative() {
        return isCumulative;
    }

    public MetaAttributeJson setCumulative(boolean cumulative) {
        isCumulative = cumulative;
        return this;
    }

    public boolean isNullify() {
        return isNullify;
    }

    public MetaAttributeJson setNullify(boolean nullify) {
        isNullify = nullify;
        return this;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public MetaAttributeJson setRequired(boolean required) {
        isRequired = required;
        return this;
    }

    public String getSetKeyType() {
        return setKeyType;
    }

    public void setSetKeyType(String setKeyType) {
        this.setKeyType = setKeyType;
    }

    public Long getRefClassId() {
        return refClassId;
    }

    public void setRefClassId(Long refClassId) {
        this.refClassId = refClassId;
    }

    public String getRefMetaType() {
        return refMetaType;
    }

    public void setRefMetaType(String refMetaType) {
        this.refMetaType = refMetaType;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public boolean isNullableKey() {
        return nullableKey;
    }

    public void setNullableKey(boolean nullableKey) {
        this.nullableKey = nullableKey;
    }

    public String getAttrPathPart() {
        return attrPathPart;
    }

    public void setAttrPathPart(String attrPathPart) {
        this.attrPathPart = attrPathPart;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isSync() {
        return isSync;
    }

    public MetaAttributeJson setSync(boolean sync) {
        isSync = sync;
        return this;
    }

}
