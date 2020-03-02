package kz.bsbnb.usci.eav.model.base;

import kz.bsbnb.usci.eav.model.meta.MetaType;
import kz.bsbnb.usci.eav.model.meta.MetaValue;

import java.io.Serializable;
import java.util.*;

/**
 * @author Artur Tkachenko
 * @author Alexandr Motov
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 */

public class BaseSet implements BaseContainer, Cloneable, Serializable {
    private MetaType metaType;

    // использование HashMap вместе HashSet сделано преднамеренно
    // необходимо допускать дублирование записей в коллекцию во время парсинга,
    // и затем выявлять дубликаты через бизнес правила
    // иначе бы HashSet перетирал дубликаты и в коллекций оставались бы только уникальные записи
    // в качестве ключей HashMap служат UUID которые генерируют уникальные значения
    // обычные массивы тоже можно было использовать, но ключи HashMap можно зайдествовать в коде
    private Map<String, BaseValue> values = new HashMap<>();

    public BaseSet() {
        /*Пустой конструктор*/
    }

    public BaseSet(MetaType metaType) {
        this.metaType = metaType;
    }

    @Override
    public MetaType getMetaType() {
        return metaType;
    }

    public void put(BaseValue baseValue) {
        if (baseValue == null)
            throw new IllegalArgumentException("Добавлять в сет пустое значение не допустимо");

        UUID uuid = UUID.randomUUID();
        put(uuid.toString(), baseValue);
    }

    private void put(String name, BaseValue baseValue) {
        if (baseValue == null || baseValue.getValue() == null)
            throw new IllegalArgumentException("Добавлять в множество пустое значение не допустимо");

        if (name == null) {
            UUID uuid = UUID.randomUUID();
            put(uuid.toString(), baseValue);
        }

        if (baseValue.getValue() instanceof BaseSet)
            throw new UnsupportedOperationException("Множество не может быть значением в множестве");

        values.put(name, baseValue);
    }

    @Override
    public Collection<BaseValue> getValues() {
        return values.values();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;

        for (BaseValue value : values.values()) {
            if (first) {
                sb.append(value.getValue().toString());
                first = false;
            } else {
                sb.append(", ").append(value.getValue().toString());
            }
        }

        sb.append("]");

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || getClass() != obj.getClass())
            return false;

        BaseSet that = (BaseSet) obj;

        MetaType thisMetaType = this.getMetaType();
        MetaType thatMetaType = that.getMetaType();
        if (!thisMetaType.equals(thatMetaType))
            return false;

        if (this.getValueCount() != that.getValueCount())
            return false;

        Set<UUID> uuids = new HashSet<>();
        for (BaseValue thisBaseValue : this.getValues()) {
            boolean found = false;

            for (BaseValue thatBaseValue : that.getValues()) {
                if (uuids.contains(thatBaseValue.getUuid()))
                    continue;

                Object thisObject = thisBaseValue.getValue();
                if (thisObject == null)
                    throw new RuntimeException("Элемент множества не может быть равен null");

                Object thatObject = thatBaseValue.getValue();
                if (thatObject == null)
                    throw new RuntimeException("Элемент множества не может быть равен null");

                if (thisObject.equals(thatObject)) {
                    uuids.add(thatBaseValue.getUuid());
                    found = true;
                }
            }

            if (!found)
                return false;
        }

        return true;
    }
    
    @Override
    public int hashCode() {
        int result = super.hashCode();
        for (BaseValue baseValue : values.values())
            result += 31 * result + baseValue.getValue().hashCode();

        return result;
    }

    @Override
    public BaseSet clone() {
        BaseSet baseSetCloned;
        try {
            baseSetCloned = (BaseSet) super.clone();

            HashMap<String, BaseValue> valuesCloned = new HashMap<>();

            for (String attribute : values.keySet()) {
                BaseValue baseValue = values.get(attribute);
                BaseValue baseValueCloned = baseValue.clone();
                valuesCloned.put(attribute, baseValueCloned);
            }

            baseSetCloned.values = valuesCloned;
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException("BaseSet класс не реализует интерфейс Cloneable");
        }

        return baseSetCloned;
    }

    @Override
    public boolean isSet() {
        return true;
    }

    @Override
    public boolean isComplex() {        
        return metaType.isComplex();
    }

    public Object getElSimple(String filter) {
        if (metaType.isComplex() || metaType.isSet()) {
            throw new IllegalArgumentException("Простой метод был вызван для комплексного атрибута или массива");
        }

        for (BaseValue value : values.values()) {
            Object innerValue = value.getValue();
            if (innerValue == null)
                continue;

            if (value.equalsToString(filter, ((MetaValue) metaType).getMetaDataType()))
                return innerValue;
        }

        return null;
    }

    public Object getElComplex(String filter) {
        if (!metaType.isComplex() || metaType.isSet())
            throw new IllegalArgumentException("Комплексный метод был вызван для простого атрибута или массива");

        StringTokenizer tokenizer = new StringTokenizer(filter, ",");

        HashMap<String, String> params = new HashMap<>();

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();

            StringTokenizer innerTokenizer = new StringTokenizer(token, "=");

            String fieldName = innerTokenizer.nextToken().trim();
            if (!innerTokenizer.hasMoreTokens())
                throw new IllegalStateException("Ожидалось значение поля");

            String fieldValue = innerTokenizer.nextToken().trim();

            params.put(fieldName, fieldValue);
        }

        for (BaseValue value : values.values()) {
            Object innerValue = value.getValue();
            if (innerValue == null)
                continue;

            if (((BaseEntity) innerValue).equalsToString(params))
                return innerValue;
        }

        return null;
    }

    public Object getEl(String filter) {
        if (metaType.isComplex())
            return getElComplex(filter);

        return getElSimple(filter);
    }

    @Override
    public int getValueCount() {
        return values.size();
    }

}
