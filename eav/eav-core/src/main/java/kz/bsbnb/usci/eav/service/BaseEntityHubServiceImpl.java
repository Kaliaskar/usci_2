package kz.bsbnb.usci.eav.service;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import kz.bsbnb.usci.eav.dao.BaseEntityHubDao;
import kz.bsbnb.usci.eav.model.Constants;
import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseSet;
import kz.bsbnb.usci.eav.model.base.BaseValue;
import kz.bsbnb.usci.eav.model.core.EavHub;
import kz.bsbnb.usci.eav.model.meta.*;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.persistence.Persistable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @author Jandos Iskakov
 */

@Service
public class BaseEntityHubServiceImpl implements BaseEntityHubService {
    private static final Logger logger = LoggerFactory.getLogger(BaseEntityHubServiceImpl.class);

    private static final Gson gson = new Gson();

    private final BaseEntityHubDao baseEntityHubDao;

    public BaseEntityHubServiceImpl(BaseEntityHubDao baseEntityHubDao) {
        this.baseEntityHubDao = baseEntityHubDao;
    }

    private class KeySet {
        final Short id;
        BaseValue array;
        final List<BaseValue> scalars = new ArrayList<>();
        final List<String> jsonKeys = new ArrayList<>();
        final List<MetaAttribute> emptyKeys = new ArrayList<>();
        final List<MetaAttribute> metaKeys = new ArrayList<>();

        KeySet(Short id) {
            this.id = id;
        }
    }

    private class KeyInfo {
        final BaseValue array;
        final MetaAttribute arrayMeta;
        final List<BaseValue> scalars;

        KeyInfo(BaseValue array, MetaAttribute arrayMeta, List<BaseValue> scalars) {
            this.array = array;
            this.arrayMeta = arrayMeta;
            this.scalars = scalars;
        }
    }

    /**
     * возвращает список ключей по которым можно вести поиск сущности
     * то есть для идентификаций сущности может понадобится один из ключей из списка
     * ключи имеют формат JSON
     * */
    public Map<Short, List<String>> getKeys(final BaseEntity baseEntity) {
        MetaClass metaClass = baseEntity.getMetaClass();
        if (!metaClass.isSearchable())
            throw new UsciException("Метод обрабатывает только мета классы которые содержат ключевые поля");

        if (baseEntity.parentIsKey() && baseEntity.getParentEntity().getId() == null)
            throw new UsciException("Id родительской сущности пустая");

        Map<Short, KeySet> keySetMap = new HashMap<>();

        // получаем все ключевые атрибуты сущности предварительно их отсортировав
        List<MetaAttribute> attributes = metaClass.getAttributes().stream()
                .filter(metaAttribute -> metaAttribute.isKey() || metaAttribute.isNullableKey())
                .sorted(Comparator.comparing(Persistable::getId))
                .collect(toList());

        // группируем ключевые атрибуты, каждая группа содержит свою коллекцию ключей
        for (MetaAttribute metaAttribute : attributes) {
            MetaType metaType = metaAttribute.getMetaType();
            Short keySetId = metaAttribute.getKeySet();

            KeySet keySet = keySetMap.getOrDefault(keySetId, new KeySet(keySetId));
            keySet.metaKeys.add(metaAttribute);

            BaseValue baseValue = baseEntity.getBaseValue(metaAttribute.getName());
            if (baseValue == null || baseValue.getValue() == null)
                keySet.emptyKeys.add(metaAttribute);
            else {
                if (metaType.isSet()) {
                    BaseSet childBaseSet = (BaseSet) baseValue.getValue();

                    if (childBaseSet == null || childBaseSet.getValueCount() == 0)
                        throw new UsciException("Множество должно содержать элементы");
                    if (!metaType.isComplex())
                        throw new UsciException("Простое множество не может быть ключевым");

                    if (keySet.array != null)
                        throw new UsciException("Несколько множеств ключевых сетов не поддерживается");

                    keySet.array = baseValue;
                }
                else
                    keySet.scalars.add(baseValue);

                keySetMap.put(keySetId, keySet);
            }
        }

        for (KeySet keySet : keySetMap.values()) {
            if (keySet.emptyKeys.size() == keySet.metaKeys.size())
                keySetMap.remove(keySet.id);
            else {
                // если в группе есть пустые ключи но не помечены что они могут быть nullable (isKey=2), тогда выдаю ошибку
                if (keySet.emptyKeys.size() > 0 && keySet.metaKeys.size() - keySet.emptyKeys.size() > 0) {
                    Optional<MetaAttribute> emptyKey = keySet.emptyKeys.stream()
                            .filter(metaKey -> !metaKey.isNullableKey()).findFirst();
                    if (emptyKey.isPresent())
                        throw new UsciException(String.format("Ключевой атрибут не может быть пустым. %s", baseEntity.getMetaClass().getClassName()));
                }
            }
        }

        if (keySetMap.size() == 0)
            throw new UsciException(String.format("Отсутствуют ключи для идентификаций сущности. %s", baseEntity));

        // генерируем сами json ключи для поиска в хабе
        for (KeySet keySet : keySetMap.values()) {
            // сохраняем порядок ключей иначе будем получать разный ключ
            // сортируем скалярные значения по идентификатору атрибута в возрастающем порядке
            keySet.scalars.sort(Comparator.comparing(o -> o.getMetaAttribute().getId()));

            // случай когда у группы ключей есть массив
            if (keySet.array != null) {
                MetaAttribute metaAttribute = keySet.array.getMetaAttribute();
                MetaSet metaSet = (MetaSet) metaAttribute.getMetaType();

                BaseSet childBaseSet = (BaseSet) keySet.array.getValue();

                // выбираем в массиве значения по которым можно идентифицировать сущность (новые значения в массиве игнорируем)
                // пример: если пришел новый документ по субьекту то он очевидно не может служит идентификатором
                // то есть мы можем идентифицировать субьект только по существующи документам
                List<BaseValue> childBaseSetValues = new ArrayList<>();
                for (BaseValue childBaseSetValue : childBaseSet.getValues()) {
                    BaseEntity childBaseSetEntity = (BaseEntity) childBaseSetValue.getValue();

                    if (childBaseSetEntity.getId() != null) {
                        childBaseSetValues.add(childBaseSetValue);
                    } else {
                        if (metaSet.getKeyType() == SetKeyType.ALL)
                            return Collections.emptyMap();
                    }
                }

                // ни один элемент ключевого сета не был идентифицирован
                if (childBaseSetValues.size() == 0)
                    return Collections.emptyMap();

                // если любой элемент в сете может служить ключом (SetKeyType.ANY)
                // то каждый элемент + скалярные атрибуты генерируют по отдельности ключи
                if (metaSet.getKeyType() == SetKeyType.ANY) {
                    for (BaseValue childBaseSetValue : childBaseSetValues)
                        keySet.jsonKeys.add(getJsonKey(new KeyInfo(childBaseSetValue, metaAttribute, keySet.scalars)));
                }
                // все элементы сета в совокупности являются ключами то генерируем один единый ключ
                // где элементы сета будут разделены запятыми (то есть получаем только один ключ)
                else if (metaSet.getKeyType() == SetKeyType.ALL)
                    keySet.jsonKeys.add(getJsonKey(new KeyInfo(keySet.array, metaAttribute, keySet.scalars)));
            }
            else
                keySet.jsonKeys.add(getJsonKey(new KeyInfo(null, null, keySet.scalars)));
        }

        return keySetMap.values().stream()
                .collect(Collectors.toMap(o -> o.id, o -> o.jsonKeys));
    }

    private String getJsonKey(KeyInfo keyInfo) {
        // сохраняем порядок ключей иначе будем получать разные ключи, посему был выбран LinkedHashMap
        Map<String, Object> json = new LinkedHashMap<>();

        // массивы в ключе всегда ставим на первую позицию, скалярные значения позади
        if (keyInfo.array != null)
            json.put(keyInfo.arrayMeta.getName(), getKeyValue(keyInfo.arrayMeta.getMetaType(), keyInfo.array));

        if (keyInfo.scalars != null) {
            keyInfo.scalars.forEach(baseValue -> {
                MetaAttribute metaAttribute = baseValue.getMetaAttribute();
                json.put(metaAttribute.getName(), getKeyValue(metaAttribute.getMetaType(), baseValue));
            });
        }

        return gson.toJson(json);
    }

    private Object getKeyValue(MetaType metaType, BaseValue baseValue) {
        if (metaType.isComplex()) {
            if (metaType.isSet()) {
                if (baseValue.getValue() instanceof BaseSet) {
                    // сортируем элементы сета по id иначе получим разные ключи
                    BaseSet childBaseSet = (BaseSet) baseValue.getValue();
                    return childBaseSet.getValues().stream()
                            .map(childBaseSetValue -> ((BaseEntity) childBaseSetValue.getValue()).getId())
                            .sorted(Long::compareTo).toArray();
                }
                else
                    return Collections.singletonList(((BaseEntity) baseValue.getValue()).getId());
            }
            else
                return ((BaseEntity) baseValue.getValue()).getId();
        }
        else {
            if (metaType.isSet())
                throw new UsciException("Простое множество не может быть ключевым");

            MetaValue metaValue = (MetaValue) metaType;
            MetaDataType metaDataType = metaValue.getMetaDataType();

            // примитивные типы данных привожу в формат хаба (дата, boolean) (см. код)
            // необходимо строго сохранят и не менять формат так как у хаба собственный формат
            if (metaDataType == MetaDataType.DOUBLE)
                throw new UsciException(String.format("Тип данных %s не могут участвовать в ключе EAV_HUB", metaValue.getMetaDataType()));
            else if (metaDataType == MetaDataType.DATE) {
                LocalDate date = (LocalDate) baseValue.getValue();
                return date.format(DateTimeFormatter.ISO_DATE);
            } else if (metaDataType == MetaDataType.DATE_TIME) {
                LocalDateTime dateTime = (LocalDateTime) baseValue.getValue();
                return dateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            }  else if (metaDataType == MetaDataType.BOOLEAN) {
                return ((Boolean) baseValue.getValue()) ? 1 : 0;
            } else if (metaDataType == MetaDataType.INTEGER || metaDataType == MetaDataType.STRING) {
                return baseValue.getValue();
            }
        }

        return null;
    }


    @Override
    public void insert(List<BaseEntity> baseEntities) {
        List<EavHub> hubs = new ArrayList<>();

        // в данном блоке получаю сгенерированный ключ по сущности, затем просто заполняю хаб записями
        for (BaseEntity baseEntity : baseEntities) {
            List<String> keys = getKeys(baseEntity).values().stream()
                    .flatMap(Collection::stream)
                    .collect(toList());
            if (keys.size() == 0)
                throw new UsciException(String.format("Ни один ключ не определен для вставки в хаб(insert). s%", baseEntity));

            for (String key : keys)
                hubs.add(new EavHub(baseEntity, key));
        }

        // разбиваю список хабов по порциям (рекомендуемый размер в одном батче 30 элементов)
        // если порция дает оптимальный результат то делаю batch insert, иначе лучше делать обычный insert
        List<List<EavHub>> partitions = Lists.partition(hubs, Constants.OPTIMAL_BATCH_SIZE[1]);

        for (List<EavHub> partition: partitions) {
            if (partition.size() >= Constants.OPTIMAL_BATCH_SIZE[0] && partition.size() <= Constants.OPTIMAL_BATCH_SIZE[1])
                baseEntityHubDao.insert(partition);
            else {
                for (EavHub eavHub : partition)
                    baseEntityHubDao.insert(Collections.singletonList(eavHub));
            }
        }
    }

    @Override
    public void add(BaseEntity baseEntity, List<String> oldKeys) {
        List<EavHub> hubs = new ArrayList<>();

        List<String> keys = getKeys(baseEntity).values().stream()
                .flatMap(Collection::stream)
                .collect(toList());

        keys.removeAll(oldKeys);

        if (keys.size() == 0)
            throw new UsciException(String.format("Ни один ключ не определен для вставки в хаб(add). {}", baseEntity));

        for (String key : keys)
            hubs.add(new EavHub(baseEntity, key));
        List<List<EavHub>> partitions = Lists.partition(hubs, Constants.OPTIMAL_BATCH_SIZE[1]);

        for (List<EavHub> partition: partitions) {
            if (partition.size() >= Constants.OPTIMAL_BATCH_SIZE[0] && partition.size() <= Constants.OPTIMAL_BATCH_SIZE[1])
                baseEntityHubDao.insert(partition);
            else {
                for (EavHub eavHub : partition)
                    baseEntityHubDao.insert(Collections.singletonList(eavHub));
            }
        }
    }

    @Override
    public Optional<Long> find(BaseEntity baseEntity) {
        MetaClass metaClass = baseEntity.getMetaClass();
        if (!metaClass.isSearchable())
            throw new UsciException("Метод обрабатывает только мета классы которые содержат ключевые поля");

        // по новым сущностям отсутствуют данные для идентификаций
        // возвращаем пустой обьект это дозволено, то есть нам не нужно выкидывать ошибку
        if (baseEntity.parentIsKey() && baseEntity.getParentEntity().getId() == null)
            return Optional.empty();

        List<String> keys = getKeys(baseEntity).values().stream()
                .flatMap(Collection::stream)
                .collect(toList());
        if (keys.size() == 0)
            return Optional.empty();

        return baseEntityHubDao.find(new EavHub(baseEntity, null), keys);
    }

    @Override
    public void deleteAll(final EavHub eavHub) {
        Objects.requireNonNull(eavHub.getRespondentId(), "Отсутствует ID кредитора у хаба");
        Objects.requireNonNull(eavHub.getEntityId(), "Отсутствует ID сущности у хаба");
        Objects.requireNonNull(eavHub.getMetaClassId(), "Отсутствует ID мета класса у хаба");
        Objects.requireNonNull(eavHub.getParentEntityId(), "Отсутствует PARENT_ENTITY_ID у хаба");
        Objects.requireNonNull(eavHub.getParentClassId(), "Отсутствует PARENT_CLASS_ID мета класса у хаба");

        baseEntityHubDao.delete(eavHub);
    }

    @Override
    public void update(EavHub eavHub) {
        Objects.requireNonNull(eavHub.getRespondentId(), "Отсутствует ID кредитора у хаба");
        Objects.requireNonNull(eavHub.getEntityId(), "Отсутствует ID сущности у хаба");
        Objects.requireNonNull(eavHub.getEntityKey(), "Отсутствует ENTITY_KEY у хаба");
        Objects.requireNonNull(eavHub.getNewEntityKey(), "Отсутствует NEW_ENTITY_KEY сущности у хаба");
        Objects.requireNonNull(eavHub.getMetaClassId(), "Отсутствует ID мета класса у хаба");
        Objects.requireNonNull(eavHub.getParentEntityId(), "Отсутствует PARENT_ENTITY_ID у хаба");
        Objects.requireNonNull(eavHub.getParentClassId(), "Отсутствует PARENT_CLASS_ID мета класса у хаба");

        baseEntityHubDao.update(eavHub);
    }

}
