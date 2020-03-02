package kz.bsbnb.usci.sync.service.impl;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import kz.bsbnb.usci.eav.client.ProductClient;
import kz.bsbnb.usci.eav.dao.BaseEntityHubDao;
import kz.bsbnb.usci.eav.model.Constants;
import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseSet;
import kz.bsbnb.usci.eav.model.base.BaseValue;
import kz.bsbnb.usci.eav.model.core.EavHub;
import kz.bsbnb.usci.eav.model.meta.*;
import kz.bsbnb.usci.eav.service.ProductService;
import kz.bsbnb.usci.sync.service.BaseEntityHubService;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.persistence.Persistable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static kz.bsbnb.usci.model.Constants.NBK_AS_RESPONDENT_ID;

/**
 * @author Jandos Iskakov
 */

@Service
public class BaseEntityHubServiceImpl implements BaseEntityHubService {

    private static final Gson gson = new Gson();
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    public BaseEntityHubServiceImpl(NamedParameterJdbcTemplate npJdbcTemplate,
                                    JdbcTemplate jdbcTemplate) {
        this.npJdbcTemplate = npJdbcTemplate;
        this.jdbcTemplate = jdbcTemplate;
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


    private Optional<Long> find(BaseEntity baseEntity) {
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

        return findDB(new EavHub(baseEntity, null), keys);
    }



    private Optional<Long> findDB(EavHub eavHub, List<String> keys) {
        try {
            String query = "select distinct ENTITY_ID\n" +
                    "  from EAV_DATA.EAV_HUB\n" +
                    " where CREDITOR_ID = :RESPONDENT_ID\n" +
                    "   and CLASS_ID = :META_CLASS_ID\n" +
                    "   and PARENT_ENTITY_ID = :PARENT_ENTITY_ID\n" +
                    "   and PARENT_CLASS_ID = :PARENT_CLASS_ID\n";

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("RESPONDENT_ID", eavHub.getRespondentId())
                    .addValue("META_CLASS_ID", eavHub.getMetaClassId())
                    .addValue("PARENT_ENTITY_ID", eavHub.getParentEntityId())
                    .addValue("PARENT_CLASS_ID", eavHub.getParentClassId());

            if (keys.size() == 1) {
                query += "   and ENTITY_KEY = :KEY\n";
                params.addValue("KEY", keys.get(0));
            }
            else {
                query += "   and ENTITY_KEY in (:KEY_LIST)\n";
                params.addValue("KEY_LIST", keys);
            }

            return Optional.ofNullable(npJdbcTemplate.queryForObject(query, params, Long.class));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (IncorrectResultSizeDataAccessException e) {
            throw new UsciException(String.format("Найдено более одной записи в таблице EAV_DATA.EAV_HUB по ключам %s и параметрам %s", keys, eavHub));
        }
    }

    @Override
    public BaseEntity prepareBaseEntity(final BaseEntity baseEntity, Long respondentId) {
        return prepareBaseEntity(baseEntity, respondentId, null);
    }

    private BaseEntity prepareBaseEntity(final BaseEntity baseEntity, Long respondentId, MetaAttribute attribute) {
        MetaClass metaClass = baseEntity.getMetaClass();

        // если родителем является справочник то все поддерево тоже является нулевым кредитором (НБК), обратное не верно
        // пример: a) у справочника может быть сущность документ то и он имеет признак нулевого кредитора
        //todo @Olzhas
        if (metaClass.isDictionary()) {
            //List<Long> products = getProductIdsByMetaClassId(metaClass.getId());

            if (metaClass.getProductId().equals(9L)) {
                respondentId = NBK_AS_RESPONDENT_ID;
            }
            //if (!products.get(0).equals(11L)) {
            //    respondentId = NBK_AS_RESPONDENT_ID;
           // }
        }

        baseEntity.setRespondentId(respondentId);

        // блок кода обрабатывает только комплексные атрибуты
        // отрабатывает рекурсивно потому что сущность может зависеть от других сущностей
        // найденный id сущности служит ключом у другой сущности
        for (String attrName : baseEntity.getAttributeNames()) {
            MetaAttribute metaAttribute = metaClass.getMetaAttribute(attrName);
            MetaType metaType = metaAttribute.getMetaType();
            BaseValue baseValue = baseEntity.getBaseValue(attrName);

            if (!metaAttribute.isKey() && !metaAttribute.isNullableKey())
                continue;

            if (metaType.isComplex() && baseValue.getValue() != null) {
                if (metaType.isSet()) {
                    BaseSet childBaseSet = (BaseSet) baseValue.getValue();
                    for (BaseValue childBaseValue : childBaseSet.getValues()) {
                        BaseEntity childBaseEntity = (BaseEntity) childBaseValue.getValue();
                        childBaseEntity.setRespondentId(respondentId);
                        childBaseEntity.setParentInfo(baseEntity, metaAttribute);

                        if (childBaseEntity.getValueCount() != 0)
                            prepareBaseEntity(childBaseEntity, respondentId, metaAttribute);
                    }
                }
                else {
                    BaseEntity childBaseEntity = (BaseEntity) baseValue.getValue();
                    childBaseEntity.setRespondentId(respondentId);
                    childBaseEntity.setParentInfo(baseEntity, metaAttribute);

                    if (childBaseEntity.getValueCount() != 0)
                        prepareBaseEntity(childBaseEntity, respondentId, metaAttribute);
                }
            }
        }

        // нахожу сущность из хаба если она идентифицируемая
        if (metaClass.isSearchable() && baseEntity.getId() == null) {
            baseEntity.setId(find(baseEntity).orElse(null));
        }

        return baseEntity;
    }

    private List<Long> getProductIdsByMetaClassId(Long metaClassId) {
        List<Long> products = jdbcTemplate.queryForList("select PRODUCT_ID\n" +
                "from EAV_CORE.EAV_M_PRODUCT_CLASS\n" +
                "where META_CLASS_ID = ?", new Object[] {metaClassId}, Long.class);

        if (products.size() != 1)
            throw new UsciException("Ошибка определения принадлежности класса к продукту");

        return products;
    }



}
