package kz.bsbnb.usci.eav.model.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import kz.bsbnb.usci.eav.model.meta.*;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.persistence.Persistable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static kz.bsbnb.usci.model.Constants.NBK_AS_RESPONDENT_ID;


/**
 * @author Artur Tkachenko
 * @author Alexandr Motov
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 */

public class BaseEntity extends Persistable implements BaseContainer, Cloneable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(BaseEntity.class);

    private UUID uuid = UUID.randomUUID();
    private MetaClass metaClass;
    private LocalDate reportDate;
    private LocalDate oldReportDate;
    private LocalDateTime batchReceiptDate;
    private Long respondentId;
    private Long batchId;
    private Long productId;
    private OperType operation;
    private String oldEntityKey;
    private Map<String, BaseValue> values = new HashMap<>();
    private Boolean mock = Boolean.FALSE;
    private Long userId;
    private BaseEntityInfo info;
    private Long index;
    private Set<String> received = new HashSet<>();
    private Set<String> validationErrors = new HashSet<>();
    private boolean keyElementsInstalled = false;
    private boolean maintenance = false;
    private boolean approved = false;
    private Long eavXmlId;
    @JsonIgnore
    private final List<BaseEntity> keyElements = new ArrayList<>();

    public BaseEntity() {
        /*Пустой конструктор*/
    }

    public BaseEntity(MetaClass metaClass) {
        this.metaClass = metaClass;
    }

    public BaseEntity(MetaClass metaClass, LocalDate reportDate) {
        this.metaClass = metaClass;
        this.reportDate = reportDate;
        this.respondentId = NBK_AS_RESPONDENT_ID;
    }

    public BaseEntity(MetaClass metaClass, LocalDate reportDate, Long respondentId) {
        this.metaClass = metaClass;
        this.reportDate = reportDate;
        this.respondentId = respondentId;
    }

    public BaseEntity(Long id, MetaClass metaClass, Long respondentId) {
        super(id);
        this.metaClass = metaClass;
        this.respondentId = respondentId;
    }

    public BaseEntity(Long id, MetaClass metaClass, Long respondentId, LocalDate reportDate) {
        super(id);
        this.metaClass = metaClass;
        this.reportDate = reportDate;
        this.respondentId = respondentId;
    }

    public BaseEntity(Long id, MetaClass metaClass, Long respondentId, LocalDate reportDate, Long batchId) {
        super(id);
        this.metaClass = metaClass;
        this.reportDate = reportDate;
        this.respondentId = respondentId;
        this.batchId = batchId;
    }

    public BaseEntity(MetaClass metaClass, Long respondentId, LocalDate reportDate, Long batchId) {
        this.metaClass = metaClass;
        this.reportDate = reportDate;
        this.respondentId = respondentId;
        this.batchId = batchId;
    }

    public BaseEntity(MetaClass metaClass, Long respondentId, LocalDate reportDate, Long batchId, LocalDateTime batchReceiptDate) {
        this.metaClass = metaClass;
        this.reportDate = reportDate;
        this.respondentId = respondentId;
        this.batchId = batchId;
        this.batchReceiptDate = batchReceiptDate;
    }

    public BaseEntity(BaseEntity baseEntity) {
        this.id = baseEntity.getId();
        this.metaClass = baseEntity.getMetaClass();
        this.respondentId = baseEntity.getRespondentId();
        this.reportDate = baseEntity.getReportDate();
        this.batchId = baseEntity.getBatchId();
        this.operation = baseEntity.getOperation();
        this.info = baseEntity.getInfo();
        this.approved = baseEntity.isApproved();
        this.maintenance = baseEntity.isMaintenance();
        this.batchReceiptDate = baseEntity.getBatchReceiptDate();
        this.productId = baseEntity.getProductId();
    }

    public BaseEntity(BaseEntity baseEntity, LocalDate reportDate) {
        this.id = baseEntity.getId();
        this.metaClass = baseEntity.getMetaClass();
        this.respondentId = baseEntity.getRespondentId();
        this.reportDate = reportDate;
        this.maintenance = baseEntity.isMaintenance();
    }

    public boolean isClosed() {
        return operation == OperType.CLOSE;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public LocalDate getOldReportDate() {
        return oldReportDate;
    }

    public void setOldReportDate(LocalDate oldReportDate) {
        this.oldReportDate = oldReportDate;
    }

    public LocalDateTime getBatchReceiptDate() {
        return batchReceiptDate;
    }

    public void setBatchReceiptDate(LocalDateTime batchReceiptDate) {
        this.batchReceiptDate = batchReceiptDate;
    }

    public OperType getOperation() {
        return operation;
    }

    public void setOperation(OperType operation) {
        this.operation = operation;
    }

    public MetaClass getMetaClass() {
        return metaClass;
    }

    public void setMetaClass(MetaClass metaClass) {
        this.metaClass = metaClass;
    }

    public void addValidationError(String errorMsg) {
        validationErrors.add(errorMsg);
    }

    public void clearValidationErrors() {
        validationErrors.clear();
    }

    public Set<String> getValidationErrors() {
        return validationErrors;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Boolean getMock() {
        return mock;
    }

    public void setMock(Boolean mock) {
        this.mock = mock;
    }

    public void put(final String attribute, BaseValue baseValue) {
        MetaAttribute metaAttribute = metaClass.getMetaAttribute(attribute);
        MetaType metaType = metaAttribute.getMetaType();

        if (metaType == null)
            throw new IllegalArgumentException(String.format("Тип %s , не найден в классе: %s", attribute, metaClass.getClassName()));

        if (baseValue == null)
            throw new IllegalArgumentException("Значение не может быть пустым");

        // пустые теги <tag/> и null значения не проверяем
        if (baseValue.getValue() != null) {
            Class<?> valueClass = baseValue.getValue().getClass();
            Class<?> expValueClass;

            if (baseValue.getValue() != null) {
                if (metaType.isComplex())
                    if (metaType.isSet())
                        expValueClass = BaseSet.class;
                    else
                        expValueClass = BaseEntity.class;
                else {
                    if (metaType.isSet()) {
                        expValueClass = BaseSet.class;
                        valueClass = baseValue.getValue().getClass();
                    } else {
                        MetaValue metaValue = (MetaValue) metaType;
                        expValueClass = metaValue.getMetaDataType().getDataTypeClass();
                    }
                }

                if (expValueClass == null || !expValueClass.isAssignableFrom(valueClass))
                    throw new IllegalArgumentException(String.format("Несоответствие типов в классе: %s . Нужный %s , получен: %s", metaClass.getClassName(), expValueClass, valueClass));
            }
        }

        baseValue.setMetaAttribute(metaAttribute);

        values.put(attribute, baseValue);
    }

    public void setParentInfo(BaseEntity parentEntity, MetaAttribute metaAttribute) {
        if (info == null)
            info = new BaseEntityInfo();

        info.setParent(parentEntity);
        info.setAttribute(metaAttribute);
    }

    public BaseValue getBaseValue(String attribute) {
        if (attribute.contains(".")) {
            int index = attribute.indexOf(".");
            String parentAttribute = attribute.substring(0, index);
            String childAttribute = attribute.substring(index, attribute.length() - 1);

            MetaType metaType = metaClass.getMetaAttribute(parentAttribute).getMetaType();
            if (metaType == null)
                throw new IllegalArgumentException(String.format("Мета класс %s не содержит атрибут %s", metaClass.getClassName(), parentAttribute));

            if (metaType.isComplex() && !metaType.isSet()) {
                BaseValue baseValue = values.get(parentAttribute);
                if (baseValue == null)
                    return null;

                BaseEntity baseEntity = (BaseEntity) baseValue.getValue();
                if (baseEntity == null)
                    return null;
                else
                    return baseEntity.getBaseValue(childAttribute);
            } else {
                return null;
            }
        } else {
            MetaType metaType = metaClass.getMetaAttribute(attribute).getMetaType();

            if (metaType == null)
                throw new IllegalArgumentException(String.format("Мета класс %s не содержит атрибут %s", metaClass.getClassName(), attribute));

            return values.get(attribute);
        }
    }

    public Object getEl(String path) {
        if (path.equals("ROOT"))
            return getId();

        StringTokenizer tokenizer = new StringTokenizer(path, ".");

        BaseEntity entity = this;
        MetaClass theMeta = metaClass;
        Object valueOut = null;

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            String arrayIndexes = null;

            if (token.contains("[")) {
                arrayIndexes = token.substring(token.indexOf("[") + 1, token.length() - 1);
                token = token.substring(0, token.indexOf("["));
            }

            MetaAttribute attribute = theMeta.getMetaAttribute(token);

            MetaType type = null;
            try {
                type = attribute.getMetaType();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (entity == null)
                return null;

            BaseValue value = entity.getBaseValue(token);

            if (value == null || value.getValue() == null) {
                valueOut = null;
                break;
            }

            valueOut = value.getValue();

            if (type == null)
                throw new IllegalStateException("MetaType не может быть null");

            if (type.isSet()) {
                if (arrayIndexes != null) {
                    valueOut = ((BaseSet) valueOut).getEl(arrayIndexes.replaceAll("->", "."));
                    type = ((MetaSet) type).getMetaType();
                } else {
                    return valueOut;
                }
            }

            if (type.isComplex()) {
                entity = (BaseEntity) valueOut;
                theMeta = (MetaClass) type;
            } else {
                if (tokenizer.hasMoreTokens()) {
                    throw new IllegalArgumentException("Путь не может иметь промежуточные простые значения");
                }
            }
        }

        return valueOut;
    }

    public Object getEls(String path) {
        Queue<Object> queue = new LinkedList<>();

        StringBuilder str = new StringBuilder();
        String[] operations = new String[500];
        boolean[] isFilter = new boolean[500];
        String function = null;

        if (!path.startsWith("{")) throw new RuntimeException("Функция должна быть указана");
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '}') {
                function = path.substring(1, i);
                path = path.substring(i + 1);
                break;
            }
        }

        if (function == null) throw new RuntimeException("Нет функций");

        Set<Object> allowedSet = new TreeSet<>();

        if (function.startsWith("set")) {
            String[] elems = function.substring(function.indexOf('(') + 1, function.indexOf(')')).split(",");
            if (function.startsWith("setInt")) {
                allowedSet = new TreeSet<>();
                for (String e : elems)
                    allowedSet.add(Integer.parseInt(e.trim()));
            } else if (function.startsWith("setLong")) {
                allowedSet = new TreeSet<>();
                for (String e : elems)
                    allowedSet.add(Long.parseLong(e.trim()));
            } else if (function.startsWith("setString")) {
                allowedSet = new TreeSet<>();
                for (String e : elems)
                    allowedSet.add(e.trim());
            }
        }

        if (function.startsWith("hasDuplicates")) {
            String pattern = "hasDuplicates\\((\\S+)\\)";
            Matcher m = Pattern.compile(pattern).matcher(function);
            String downPath;
            boolean ret = false;

            if (m.find()) {
                downPath = m.group(1);
            } else {
                throw new RuntimeException("Функциональные дубликаты не правильные: пример {hasDuplicates(subjects)}ref_doc_type.code,date");
            }

            LinkedList list = (LinkedList) getEls("{get}" + downPath);

            String[] fields = path.split(",");

            Set<Object> controlSet;

            if (fields.length == 1)
                controlSet = new HashSet<>();
            else if (fields.length == 2)
                controlSet = new HashSet<>();
            else
                throw new RuntimeException("Бизнес правила пока не реализована");

            for (Object o : list) {
                BaseEntity entity = (BaseEntity) o;
                Object entry;

                if (fields.length == 1)
                    entry = entity.getEl(fields[0]);
                else { // fields.length  == 2
                    entry = new AbstractMap.SimpleEntry<>(entity.getEl(fields[0]), entity.getEl(fields[1]));
                }


                if (controlSet.contains(entry)) {
                    ret = true;
                } else {
                    controlSet.add(entry);
                }
            }
            return ret;
        }


        int yk = 0;
        int open = 0;
        int eqCnt = 0;

        for (int i = 0; i <= path.length(); i++) {
            if (i == path.length()) {
                if (open != 0)
                    throw new RuntimeException("Не правильная открывающая скобка");
                break;
            }
            if (path.charAt(i) == '=') eqCnt++;
            if (path.charAt(i) == '!' && (i + 1 == path.length() || path.charAt(i + 1) != '='))
                throw new RuntimeException("Нет знака равенства\\Знак равенства должна присутствовать после '!'");

            if (path.charAt(i) == '[') open++;
            if (path.charAt(i) == ']') {
                open--;
                if (eqCnt != 1) throw new RuntimeException("Не правильный знак равенства\\Только один знак равенства в фильтре и только в фильтр");
                eqCnt = 0;
            }
            if (open < 0 || open > 1) throw new RuntimeException("Не правильные скобки");
        }

        for (int i = 0; i <= path.length(); i++) {
            if (i == path.length()) {
                if (str.length() > 0) {
                    String[] arr = str.toString().split("\\.");
                    for (String anArr : arr) {
                        operations[yk] = anArr;
                        isFilter[yk] = false;
                        yk++;
                    }
                }
                break;
            }
            char c = path.charAt(i);
            if (c == '[' || c == ']') {
                if (str.length() > 0) {
                    if (c == ']') {
                        operations[yk] = str.toString();
                        isFilter[yk] = true;
                        yk++;
                    } else {
                        String[] arr = str.toString().split("\\.");
                        for (String anArr : arr) {
                            operations[yk] = anArr;
                            isFilter[yk] = false;
                            yk++;
                        }
                    }
                    str.setLength(0);
                }
            } else {
                str.append(c);
            }
        }

        List<Object> ret = new LinkedList<>();
        queue.add(this);
        queue.add(0);
        int retCount = 0;

        while (queue.size() > 0) {
            Object curO = queue.poll();
            int step = (Integer) queue.poll();

            if (curO == null)
                continue;

            if (step == yk) {
                if (function.startsWith("count")) {
                    retCount++;
                } else if (function.startsWith("set"))
                    if (allowedSet.contains(curO))
                        retCount++;
                ret.add(curO);
                continue;
            }

            //noinspection ConstantConditions
            BaseEntity curBE = (BaseEntity) curO;
            MetaClass curMeta = curBE.getMetaClass();

            if (!isFilter[step]) {
                MetaAttribute nextAttribute = curMeta.getMetaAttribute(operations[step]);

                if (!nextAttribute.getMetaType().isComplex()) { // transition to BASIC type
                    queue.add(curBE.getEl(operations[step]));
                    queue.add(step + 1);
                } else if (nextAttribute.getMetaType().isSet()) { //transition to array
                    BaseSet next = (BaseSet) curBE.getEl(operations[step]);
                    if (next != null) {
                        for (Object o : next.getValues()) {
                            {
                                queue.add(((BaseValue) o).getValue());
                                queue.add(step + 1);
                            }
                        }
                    }
                } else { //transition to simple
                    BaseEntity next = (BaseEntity) curBE.getEl(operations[step]);
                    queue.add(next);
                    queue.add(step + 1);
                }
            } else {
                String[] parts;
                boolean inv = false;

                if (operations[step].contains("!")) {
                    parts = operations[step].split("!=");
                    inv = true;
                } else
                    parts = operations[step].split("=");

                Object o = curBE.getEl(parts[0]);

                boolean expr = (o == null && parts[1].equals("null")) || (o != null && o.toString().equals(parts[1]));
                if (inv) expr = !expr;

                if (expr) {
                    queue.add(curO);
                    queue.add(step + 1);
                }
            }
        }

        if (function.startsWith("get"))
            return ret;

        return retCount;
    }

    boolean equalsToString(HashMap<String, String> params) {
        for (String fieldName : params.keySet()) {
            String ownFieldName;
            String innerPath = null;
            if (fieldName.contains(".")) {
                ownFieldName = fieldName.substring(0, fieldName.indexOf("."));
                innerPath = fieldName.substring(fieldName.indexOf(".") + 1);
            } else {
                ownFieldName = fieldName;
            }

            MetaType metaType = metaClass.getAttributeType(ownFieldName);

            if (metaType == null)
                throw new IllegalArgumentException(String.format("Нет такого поля: %s", fieldName));

            if (metaType.isSet())
                throw new IllegalArgumentException(String.format("Не можете работать с массивами %s", fieldName));

            BaseValue baseValue = getBaseValue(ownFieldName);

            if (metaType.isComplex()) {
                baseValue = ((BaseEntity) (baseValue.getValue())).getBaseValue(innerPath);
                metaType = ((MetaClass) metaType).getAttributeType(innerPath);
            }

            if (!baseValue.equalsToString(params.get(fieldName), ((MetaValue) metaType).getMetaDataType()))
                return false;
        }

        return true;
    }

    public List<BaseEntity> getKeyElements() {
        if (!keyElementsInstalled) {
            boolean containsComplexKey = metaClass.getAttributes().stream()
                    .anyMatch(metaAttribute -> metaAttribute.getMetaType().isComplex() && metaAttribute.isKey() && !metaAttribute.isReference());

            if (!containsComplexKey && metaClass.isSearchable())
                keyElements.add(this);

            for (String name : metaClass.getAttributeNames()) {
                MetaAttribute metaAttribute = metaClass.getMetaAttribute(name);
                MetaType metaType = metaAttribute.getMetaType();

                if (metaAttribute.isReference())
                    continue;

                if (metaType.isComplex()) {
                    BaseValue baseValue = getBaseValue(name);

                    if (baseValue == null || baseValue.getValue() == null) continue;

                    if (!metaType.isSet()) {
                        keyElements.addAll(((BaseEntity) baseValue.getValue()).getKeyElements());
                    } else {
                        BaseSet baseSet = (BaseSet) baseValue.getValue();
                        for (BaseValue childBaseValue : baseSet.getValues())
                            keyElements.addAll(((BaseEntity) childBaseValue.getValue()).getKeyElements());
                    }
                }
            }

            keyElementsInstalled = true;
        }

        return keyElements;
    }

    // метод предназначен для сверки двух сущностей по ключевым полям (все значения ключей должны совпадать)
    // сверка сетов в качестве ключей не предусмотрена
    // сущности должны быть одного мета класса и респондента
    public boolean equalsByKey(BaseEntity baseEntity) {
        if (this == baseEntity)
            return true;

        if (baseEntity == null || getClass() != baseEntity.getClass())
            return false;

        if (!this.getMetaClass().equals(baseEntity.getMetaClass()))
            return false;

        if (!this.respondentId.equals(baseEntity.getRespondentId()))
            return false;

        for (String name : this.metaClass.getAttributeNames()) {
            MetaAttribute metaAttribute = this.metaClass.getMetaAttribute(name);
            MetaType metaType = metaAttribute.getMetaType();

            if (metaAttribute.isKey()) {
                BaseValue thisBaseValue = this.getBaseValue(name);
                BaseValue thatBaseValue = baseEntity.getBaseValue(name);

                if (metaType.isComplex()) {
                    if (metaType.isSet()) {
                        BaseSet thisBaseSet = (BaseSet)thisBaseValue.getValue();
                        BaseSet thatBaseSet = (BaseSet)thatBaseValue.getValue();

                    /*    if (thisBaseSet.getValueCount() != thatBaseSet.getValueCount())
                            return false;*/

                        boolean result = false;
                        for (BaseValue childThisBaseValue : thisBaseSet.getValues()) {
                            result = thatBaseSet.getValues().stream()
                                    .anyMatch(baseValue -> ((BaseEntity) baseValue.getValue()).equalsByKey((BaseEntity) childThisBaseValue.getValue()));
                        }
                       return result;
                    }
                    else {
                        try {
                            if (!((BaseEntity) thisBaseValue.getValue()).equalsByKey((BaseEntity) thatBaseValue.getValue()))
                                return false;
                        }
                    catch (NullPointerException ex) {
                        logger.debug("NullPointerException baseEntityId=" + baseEntity.getId() + " , batchId=" + baseEntity.getBatchId() + ", attributeName=" + name);
                        return false;
                        }
                    }
                } else {
                    try {
                        if (!thisBaseValue.getValue().equals(thatBaseValue.getValue()))
                            return false;
                    } catch (NullPointerException ex) {
                        throw new UsciException("NullPointerException baseEntityId=" + baseEntity.getId() + " , batchId=" + baseEntity.getBatchId() + ", attributeName=" + name);
                    }
                }
            }

            if (metaAttribute.isNullableKey()) {
                if (!metaType.isComplex()) {
                    BaseValue thisBaseValue = this.getBaseValue(name);
                    BaseValue thatBaseValue = baseEntity.getBaseValue(name);

                    if ((thisBaseValue == null && thatBaseValue != null) || (thisBaseValue != null && thatBaseValue == null))
                        return false;

                    if (thisBaseValue != null && thatBaseValue != null) {
                        if ((thisBaseValue.getValue() == null && thatBaseValue.getValue() != null) ||
                            (thisBaseValue.getValue() != null && thatBaseValue.getValue() == null))
                            return false;

                        if (thisBaseValue.getValue() != null && thatBaseValue.getValue() != null)
                            if (!thisBaseValue.getValue().equals(thatBaseValue.getValue()))
                                return false;
                    }
                } else
                    throw new IllegalStateException("isComplex isNullableKey not supported");
            }
        }

        if (this.parentIsKey()) {
            if ((this.getInfo() != null && baseEntity.getInfo() == null) || (this.getInfo() == null && baseEntity.getInfo() != null))
                return false;
           return this.getInfo() == null || this.getInfo().equals(baseEntity.getInfo());
        }

        return true;
    }

    @Override
    public Collection<BaseValue> getValues() {
        return values.values();
    }

    // проверяет на соответсвие атрибутов
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || getClass() != obj.getClass())
            return false;

        BaseEntity that = (BaseEntity) obj;

        if (!this.getMetaClass().getId().equals(that.getMetaClass().getId()))
            return false;

        int thisValueCount = this.getValueCount();
        int thatValueCount = that.getValueCount();

        if (thisValueCount != thatValueCount)
            return false;

        for (String attributeName : metaClass.getAttributeNames()) {
            MetaAttribute metaAttribute = metaClass.getMetaAttribute(attributeName);
            MetaType metaType = metaAttribute.getMetaType();

            BaseValue thisBaseValue = this.getBaseValue(attributeName);
            BaseValue thatBaseValue = that.getBaseValue(attributeName);

            if (thisBaseValue == null && thatBaseValue == null)
                continue;

            if (thisBaseValue == null || thatBaseValue == null)
                return false;

            if (metaType.isSet()) {
                BaseSet thisBaseSet = (BaseSet) thisBaseValue.getValue();
                BaseSet thatBaseSet = (BaseSet) thatBaseValue.getValue();

                if (thisBaseSet == null && thatBaseSet == null)
                    continue;

                if (thisBaseSet == null || thatBaseSet == null)
                    return false;

                if (thisBaseSet.getValueCount() != thatBaseSet.getValueCount())
                    return false;

                for (BaseValue thisChildBaseValue : thisBaseSet.getValues()) {
                    Object thisChildValue = thisChildBaseValue.getValue();

                    boolean childValueFound = false;

                    for (BaseValue thatChildBaseValue : thatBaseSet.getValues()) {
                        Object thatChildValue = thatChildBaseValue.getValue();

                        if (metaType.isComplex() && metaAttribute.isReference()) {
                            try {
                                if (!((BaseEntity) thisChildValue).getId().equals(((BaseEntity) thatChildValue).getId()))
                                    return false;
                            } catch (NullPointerException ex) {
                                logger.debug("NullPointerException baseEntityId=" + that.getId() + " , batchId=" + that.getBatchId() + ", attributeName=" + attributeName);
                                return false;
                            }

                            continue;
                        }

                        if (thisChildValue.equals(thatChildValue))
                            childValueFound = true;
                    }

                    if (!childValueFound)
                        return false;
                }
            } else {
                Object thisValue = thisBaseValue.getValue();
                Object thatValue = thatBaseValue.getValue();

                if (thisValue == null && thatValue == null)
                    continue;

                if (thisValue == null || thatValue == null)
                    return false;

                if (metaType.isComplex() && metaAttribute.isReference()) {
                    if (!((BaseEntity) thisValue).getId().equals(((BaseEntity) thatValue).getId()))
                        return false;

                    continue;
                }

                if (!thisValue.equals(thatValue))
                    return false;

                // Проверка на изменение ключевых полей
                if (!metaType.isComplex() && (thisBaseValue.getNewBaseValue() != null || thatBaseValue.getNewBaseValue() != null))
                    return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return BaseEntityOutput.toString(this, true);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result += 31 * result + metaClass.hashCode();
        result += 31 * result + (int) (respondentId ^ (respondentId >>> 32));
        result += 31 * result + values.hashCode();

        return result;
    }

    @Override
    public BaseEntity clone() {
        BaseEntity baseEntityCloned;

        try {
            baseEntityCloned = (BaseEntity) super.clone();

            Map<String, BaseValue> valuesCloned = new HashMap<>();

            for (String attribute : values.keySet()) {
                BaseValue baseValue = values.get(attribute);
                BaseValue baseValueCloned = baseValue.clone();

                baseValueCloned.setMetaAttribute(getMetaAttribute(attribute));
                valuesCloned.put(attribute, baseValueCloned);
            }

            baseEntityCloned.values = valuesCloned;
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException("BaseEntity класс не реализует интерфейс Cloneable");
        }

        return baseEntityCloned;
    }

    @Override
    public MetaClass getMetaType() {
        return metaClass;
    }

    public MetaType getAttributeType(String attribute) {
        if (attribute.contains(".")) {
            int index = attribute.indexOf(".");
            String parentIdentifier = attribute.substring(0, index);

            MetaType metaType = metaClass.getAttributeType(parentIdentifier);
            if (metaType.isComplex() && !metaType.isSet()) {
                MetaClass childMeta = (MetaClass) metaType;
                String childIdentifier = attribute.substring(index, attribute.length() - 1);
                return childMeta.getAttributeType(childIdentifier);
            } else {
                return null;
            }
        } else {
            return metaClass.getAttributeType(attribute);
        }
    }

    public MetaAttribute getMetaAttribute(String attribute) {
        return metaClass.getMetaAttribute(attribute);
    }

    public Long getRespondentId() {
        return respondentId;
    }

    public void setRespondentId(Long respondentId) {
        this.respondentId = respondentId;
    }

    public String getOldEntityKey() {
        return oldEntityKey;
    }

    public void setOldEntityKey(String oldEntityKey) {
        this.oldEntityKey = oldEntityKey;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Long getBatchIndex() {
        return index;
    }

    public void setBatchIndex(Long index) {
        this.index = index;
    }

    public Set<String> getReceived() {
        return received;
    }

    public void setReceived(Set<String> received) {
        this.received = received;
    }

    public Set<String> getAttributeNames() {
        return values.keySet();
    }

    public Stream<MetaAttribute> getAttributes() {
        return values.keySet().stream().map(metaClass::getMetaAttribute);
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BaseEntityInfo getInfo() {
        return info;
    }

    public void setInfo(BaseEntityInfo info) {
        this.info = info;
    }

    public BaseEntity getParentEntity() {
        return parentIsKey()? info.getParent(): null;
    }

    /**
     * Метод добавлен так как из BRMS идет вызов getCreditorId по старой логике
     */
    public Long getCreditorId() {
        return respondentId;
    }

    @Override
    public boolean isSet() {
        return false;
    }

    @Override
    public boolean isComplex() {
        return true;
    }

    public boolean parentIsKey() {
        return info != null && info.getAttribute() != null && info.getAttribute().isParentIsKey();
    }

    @Override
    public int getValueCount() {
        return values.size();
    }

    public boolean isMaintenance() {
        return maintenance;
    }

    public void setMaintenance(boolean maintenance) {
        this.maintenance = maintenance;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public Long getEavXmlId() {
        return eavXmlId;
    }

    public void setEavXmlId(Long eavXmlId) {
        this.eavXmlId = eavXmlId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}


