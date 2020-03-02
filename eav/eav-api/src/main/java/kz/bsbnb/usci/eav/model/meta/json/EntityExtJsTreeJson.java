package kz.bsbnb.usci.eav.model.meta.json;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс для вывода ENTITY в json формате
 * @author Jandos Iskakov
 */

public class EntityExtJsTreeJson {
    private String text;
    private String title;
    private String name;
    private Object value;
    private Object newValue = null;
    private boolean key;
    private boolean leaf = false;
    private boolean simple = false;
    private boolean array = false;
    private boolean root = false;
    private boolean dictionary = false;
    private boolean cumulative = false;
    private Long classId = 0L;
    private String metaType;
    private String iconCls;
    private Long refClassId;
    private String refType;
    private String typeCode;
    private Integer totalCount;
    private LocalDate openDate;
    private LocalDate closeDate;
    private boolean required = false;
    private List<EntityExtJsTreeJson> children = new ArrayList<>();

    public EntityExtJsTreeJson() {
        super();
    }

    public String getText() {
        return text;
    }

    public EntityExtJsTreeJson setText(String text) {
        this.text = text;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public EntityExtJsTreeJson setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getName() {
        return name;
    }

    public EntityExtJsTreeJson setName(String name) {
        this.name = name;
        return this;
    }

    public Object getValue() {
        return value;
    }

    public EntityExtJsTreeJson setValue(Object value) {
        this.value = value;
        return this;
    }

    public Object getNewValue() {
        return newValue;
    }

    public EntityExtJsTreeJson setNewValue(Object newValue) {
        this.newValue = newValue;
        return this;
    }

    public boolean isKey() {
        return key;
    }

    public EntityExtJsTreeJson setKey(boolean key) {
        this.key = key;
        return this;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public EntityExtJsTreeJson setLeaf(boolean leaf) {
        this.leaf = leaf;
        return this;
    }

    public boolean isSimple() {
        return simple;
    }

    public EntityExtJsTreeJson setSimple(boolean simple) {
        this.simple = simple;
        return this;
    }

    public boolean isArray() {
        return array;
    }

    public EntityExtJsTreeJson setArray(boolean array) {
        this.array = array;
        return this;
    }

    public boolean isDictionary() {
        return dictionary;
    }

    public EntityExtJsTreeJson setDictionary(boolean dictionary) {
        this.dictionary = dictionary;
        return this;
    }

    public boolean isCumulative() {
        return cumulative;
    }

    public EntityExtJsTreeJson setCumulative(boolean cumulative) {
        this.cumulative = cumulative;
        return this;
    }

    public boolean isRoot() {
        return root;
    }

    public EntityExtJsTreeJson setRoot(boolean root) {
        this.root = root;
        return this;
    }

    public String getMetaType() {
        return metaType;
    }

    public EntityExtJsTreeJson setMetaType(String metaType) {
        this.metaType = metaType;
        return this;
    }

    public Long getClassId() {
        return classId;
    }

    public EntityExtJsTreeJson setClassId(Long classId) {
        this.classId = classId;
        return this;
    }

    public String getIconCls() {
        return iconCls;
    }

    public EntityExtJsTreeJson setIconCls(String iconCls) {
        this.iconCls = iconCls;
        return this;
    }

    public Long getRefClassId() {
        return refClassId;
    }

    public EntityExtJsTreeJson setRefClassId(Long refClassId) {
        this.refClassId = refClassId;
        return this;
    }

    public String getRefType() {
        return refType;
    }

    public EntityExtJsTreeJson setRefType(String refType) {
        this.refType = refType;
        return this;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public EntityExtJsTreeJson setTypeCode(String typeCode) {
        this.typeCode = typeCode;
        return this;
    }

    public List<EntityExtJsTreeJson> getChildren() {
        return children;
    }

    public EntityExtJsTreeJson setChildren(List<EntityExtJsTreeJson> children) {
        this.children = children;
        return this;
    }

    public EntityExtJsTreeJson addChild(EntityExtJsTreeJson child) {
        children.add(child);

        return this;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public EntityExtJsTreeJson setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
        return this;
    }

    public LocalDate getOpenDate() {
        return openDate;
    }

    public EntityExtJsTreeJson setOpenDate(LocalDate openDate) {
        this.openDate = openDate;
        return this;
    }

    public LocalDate getCloseDate() {
        return closeDate;
    }

    public EntityExtJsTreeJson setCloseDate(LocalDate closeDate) {
        this.closeDate = closeDate;
        return this;
    }

    public boolean isRequired() {
        return required;
    }

    public EntityExtJsTreeJson setRequired(boolean required) {
        this.required = required;
        return this;
    }

}
