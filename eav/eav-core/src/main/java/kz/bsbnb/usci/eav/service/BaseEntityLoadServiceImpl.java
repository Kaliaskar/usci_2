package kz.bsbnb.usci.eav.service;

import kz.bsbnb.usci.eav.EavSqlConverter;
import kz.bsbnb.usci.eav.dao.BaseEntityRegistryDao;
import kz.bsbnb.usci.eav.model.Constants;
import kz.bsbnb.usci.eav.model.base.*;
import kz.bsbnb.usci.eav.model.core.BaseEntityUtils;
import kz.bsbnb.usci.eav.model.meta.*;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import oracle.jdbc.OracleArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static kz.bsbnb.usci.model.Constants.NBK_AS_RESPONDENT_ID;

/**
 * @author Jandos Iskakov
 */

@Repository
public class BaseEntityLoadServiceImpl implements BaseEntityLoadService {
    private static final Logger logger = LoggerFactory.getLogger(BaseEntityProcessorImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final BaseEntityDateSerivce entityDateService;
    private final BaseEntityRegistryDao baseEntityRegistryDao;
    private final ProductService productService;

    public BaseEntityLoadServiceImpl(JdbcTemplate jdbcTemplate,
                                     BaseEntityDateSerivce entityDateService,
                                     BaseEntityRegistryDao baseEntityRegistryDao,
                                     ProductService productService) {
        this.jdbcTemplate = jdbcTemplate;
        this.entityDateService = entityDateService;
        this.baseEntityRegistryDao = baseEntityRegistryDao;
        this.productService = productService;

        jdbcTemplate.setFetchSize(Constants.ORACLE_OPTIMAL_FETCH_SIZE);
        this.npJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public BaseEntity loadByMaxReportDate(BaseEntity baseEntity, LocalDate savingReportDate) {
        Objects.requireNonNull(baseEntity.getId(), String.format("Отсутствует ID у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getRespondentId(), String.format("Отсутствует ID кредитора у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getMetaClass(), String.format("Отсутствует мета класс у сущности %s", baseEntity));
        Objects.requireNonNull(savingReportDate, "Отчетная дата не задана");

        BaseEntity mock = new BaseEntity(baseEntity, savingReportDate);

        LocalDate maxReportDate = entityDateService.getMaxReportDate(mock, savingReportDate);
        if (maxReportDate == null)
            throw new UsciException(String.format("Запись %s за отчётный период %s не доступна", baseEntity, savingReportDate));

        return loadBaseEntity(mock, maxReportDate, savingReportDate, true);
    }

    @Override
    public BaseEntity loadBaseEntity(BaseEntity baseEntity, LocalDate savingReportDate) {
        // очень важный момент сущность может создаваться после savingReportDate
        // то есть мы не сможем прямо получить ее через max date
        // так что проверяем ее сперва до и потом после даты savingReportDate
        LocalDate reportDate = getBaseEntityDate(baseEntity, savingReportDate);

        return loadBaseEntity(baseEntity, reportDate, savingReportDate, true);
    }

    /**
     * метод подгружает сущность из таблицы БД по схеме одна сущность = одна запись в таблице
     * само получение сущности из БД означает что все атрибуты тоже будут подхвачены
     * комлексные атрибуты (сеты и сущности) тоже подгружаются но уже каждый отдельным запросом
     * то есть получение сущности из бд влечет получение других зависимых сущностей
     * см. код BaseEntityStoreService (как данные храненятся в таблицах БД)
     * параметр queryOnDate нужен чтобы делать запрос конкретно на дату когда известно что сущность на дату обязательно существует
     * */
    private BaseEntity loadBaseEntity(BaseEntity baseEntity, LocalDate existingReportDate, LocalDate savingReportDate, boolean queryOnDate) {
        Objects.requireNonNull(baseEntity.getId(), String.format("Отсутствует ID у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getRespondentId(), String.format("Отсутствует ID кредитора у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getMetaClass(), String.format("Отсутствует мета класс у сущности %s", baseEntity));
        Objects.requireNonNull(savingReportDate, "Отчетная дата не задана");

        MetaClass metaClass = baseEntity.getMetaClass();

        // получаем отчетную дату для operational=false
        LocalDate loadingReportDate = null;
        if (metaClass.isOperational())
            loadingReportDate = savingReportDate;
        else if (queryOnDate)
            loadingReportDate = existingReportDate;
        else
            loadingReportDate = existingReportDate == null ? savingReportDate: savingReportDate.compareTo(existingReportDate) >= 0 ? savingReportDate : existingReportDate;

        BaseEntity baseEntityLoad = new BaseEntity(baseEntity);

        String tableName = String.join(".", metaClass.getSchemaData(), metaClass.getTableName());

        StringBuilder query = new StringBuilder();
        query.append("select ENTITY_ID,\n");
        query.append("REPORT_DATE,\n");
        query.append("CREDITOR_ID,\n");
        query.append("BATCH_ID,\n");
        query.append("OPERATION_ID,\n");
        query.append("X_RECEIVED\n");

        metaClass.getAttributes().forEach(attribute -> {
            query.append(",\n");
            query.append(attribute.getColumnName());
        });

        query.append("\n");
        query.append("from ");
        query.append(tableName).append(" t1\n");
        query.append("where ENTITY_ID = :ENTITY_ID\n");
        query.append("  and CREDITOR_ID = :RESPONDENT_ID\n");

        if (metaClass.isOperational() || queryOnDate)
            query.append("  and REPORT_DATE = :REPORT_DATE\n");
        else {
            String subQuery = "select max(t2.REPORT_DATE)\n" +
                              "  from " + tableName + " t2\n" +
                              " where t2.ENTITY_ID = :ENTITY_ID\n" +
                              "   and t2.CREDITOR_ID = :RESPONDENT_ID\n" +
                              "   and t2.REPORT_DATE <= :REPORT_DATE\n";
            query.append("  and REPORT_DATE = (").append(subQuery).append(")\n");
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("ENTITY_ID", baseEntity.getId())
            .addValue("RESPONDENT_ID", baseEntity.getRespondentId())
            .addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(loadingReportDate));

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query.toString(), params);

        if (rows.size() > 1)
            throw new UsciException(String.format("Найдено более одной записи по сущности %s", baseEntity));

        if (rows.size() < 1) {
            if (!metaClass.isOperational())
                throw new UsciException(String.format("Запись не была найдена по сущности %s", baseEntity));

            return null;
        }

        Map<String, Object> values = rows.get(0);
        fillEntityAttributes(values, baseEntityLoad, savingReportDate);

        return baseEntityLoad;
    }

    @Override
    public List<BaseEntity> loadBaseEntityEntries(BaseEntity baseEntity, LocalDate beginDate) {
        Objects.requireNonNull(baseEntity.getId(), String.format("Отсутствует ID у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getRespondentId(), String.format("Отсутствует ID кредитора у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getMetaClass(), String.format("Отсутствует мета класс у сущности %s", baseEntity));

        MetaClass metaClass = baseEntity.getMetaClass();

        StringBuilder query = new StringBuilder();

        query.append("select ENTITY_ID,\n");
        query.append("REPORT_DATE,\n");
        query.append("CREDITOR_ID,\n");
        query.append("BATCH_ID,\n");
        query.append("OPERATION_ID,\n");
        query.append("X_RECEIVED\n");

        metaClass.getAttributes().forEach(attribute -> {
            query.append(",\n");
            query.append(attribute.getColumnName());
        });

        query.append("\n");
        query.append("from ");
        query.append(String.join(".", metaClass.getSchemaData(), metaClass.getTableName()));
        query.append("\n");

        query.append("where ENTITY_ID = :ENTITY_ID\n");
        query.append("  and CREDITOR_ID = :RESPONDENT_ID\n");
        query.append("  and REPORT_DATE >= :REPORT_DATE\n");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ENTITY_ID", baseEntity.getId())
                .addValue("RESPONDENT_ID", baseEntity.getRespondentId())
                .addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(beginDate));

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query.toString(), params);

        List<BaseEntity> baseEntityEntries = new ArrayList<>();
        for (Map<String, Object> values : rows) {
            LocalDate reportDate = SqlJdbcConverter.convertToLocalDate((java.sql.Timestamp) values.get("REPORT_DATE"));

            BaseEntity baseEntityEntry = new BaseEntity(metaClass, reportDate);
            fillEntityAttributes(values, baseEntityEntry, reportDate);

            baseEntityEntries.add(baseEntityEntry);
        }

        return baseEntityEntries;
    }

    @Override
    public BaseEntity loadBaseEntity(BaseEntity baseEntity) {
        LocalDate maxReportDate = entityDateService.getMaxReportDate(baseEntity);

        if (maxReportDate == null)
            throw new UnsupportedOperationException(String.format("В базе отсутсвует отчетная дата на id = %d", baseEntity.getId()));

        return loadBaseEntity(baseEntity, maxReportDate, maxReportDate, true);
    }

    @Override
    public List<BaseEntity> loadBaseEntitiesByMetaClass(MetaClass metaClass) {
        //TODO: есть возможность оптимизировать; метод пока нет времени
        List<Long> list = jdbcTemplate.queryForList(String.format("select distinct ENTITY_ID from EAV_DATA.%s where operation_id <> 3", metaClass.getTableName()), Long.class);
        //baseEntityRegistryDao.getBaseEntitiesByMetaClass(metaClass.getId());
        List<BaseEntity> baseEntities = new ArrayList<>();

        for (Long registryId : list)
            baseEntities.add(loadBaseEntity(new BaseEntity(registryId, metaClass, 0L)));

        return baseEntities;
    }



  /*  @Override
    public List<BaseEntity> loadBaseEntitiesByMetaClass(MetaClass metaClass) {
        //TODO: есть возможность оптимизировать; метод пока нет времени
        List<BaseEntityRegistry> entitiesRepo = baseEntityRegistryDao.getBaseEntitiesByMetaClass(metaClass.getId());
        List<BaseEntity> baseEntities = new ArrayList<>();

        for (BaseEntityRegistry registry : entitiesRepo)
            baseEntities.add(loadBaseEntity(new BaseEntity(registry.getEntityId(), metaClass, registry.getRespondentId())));

        return baseEntities;
    }*/

    /**
     * метод заполняет атрибуты сущности значениями полученными из таблицы БД (см. пояснения в коде)
     * */
    private void fillEntityAttributes(Map<String, Object> values, BaseEntity baseEntity, LocalDate savingReportDate) {
        MetaClass metaClass = baseEntity.getMetaClass();

        baseEntity.setId(SqlJdbcConverter.convertToLong(values.get("ENTITY_ID")));
        baseEntity.setReportDate(SqlJdbcConverter.convertToLocalDate((java.sql.Timestamp) values.get("REPORT_DATE")));
        baseEntity.setRespondentId(SqlJdbcConverter.convertToLong(values.get("CREDITOR_ID")));
        baseEntity.setBatchId(SqlJdbcConverter.convertToLong(values.get("BATCH_ID")));

        if (values.get("OPERATION_ID") != null)
            baseEntity.setOperation(OperType.getOperType(SqlJdbcConverter.convertToShort(values.get("OPERATION_ID"))));

        if (values.containsKey("X_RECEIVED"))
            baseEntity.setReceived(BaseEntityUtils.getReceivedAsSet(metaClass, String.valueOf(values.get("X_RECEIVED"))));

        for (MetaAttribute attribute : metaClass.getAttributes()) {
            MetaType metaType = attribute.getMetaType();

            Object sqlValue = values.get(attribute.getColumnName());
            if (sqlValue == null)
                continue;

            BaseValue baseValue = null;

            if (metaType.isComplex()) {
                if (metaType.isSet()) {
                    BaseSet baseSet;
                    try {
                        OracleArray oracleArray = (OracleArray) sqlValue;
                        long[] ids = oracleArray.getLongArray();
                        if (ids == null || ids.length == 0)
                            continue;
                        if (ids[0] == 0) {
                            boolean isZero = true;
                            int i = 0;
                            while (isZero) {
                                Map<String, Object> valueFail = loadBaseEntityFail(baseEntity, savingReportDate);
                                Object sqlValueFail = valueFail.get(attribute.getColumnName());
                                OracleArray oracleArrayFail = (OracleArray) sqlValueFail;
                                ids = oracleArrayFail.getLongArray();
                                i++;
                                if (ids[0] != 0 || i > 3)
                                    isZero = false;

                            }
                        }

                        MetaSet childMetaSet = (MetaSet) attribute.getMetaType();
                        MetaClass childMetaClass = (MetaClass) childMetaSet.getMetaType();
                        baseSet = loadComplexSet(baseEntity, attribute, ids, savingReportDate);

                        // сравниваю кол-во записей в сете в столбце сущности и кол-во сущностей в сете которые были подгружены отдельным запросом
                        if (!childMetaClass.isOperational())
                        if (baseSet.getValueCount() != ids.length)
                            throw new UsciException(String.format("Кол-во элементов во множестве не верное %s ", baseSet));
                    } catch (SQLException e) {
                        throw new UsciException(e.getMessage());
                    }
                    if (baseSet != null)
                       baseValue = new BaseValue(baseSet);
                } else {
                    // отдельно подгружаю зависимую сущность, в столбце хранится id зависимой сущности
                    MetaClass childMetaClass = (MetaClass) metaType;

                    //TODO need to corrected
                    Long creditorId = baseEntity.getRespondentId();

                    if (childMetaClass.isDictionary()) {
                        //List<Long> products = productService.getProductIdsByMetaClassId(childMetaClass.getId());

                        if (childMetaClass.getProductId().equals(9L)) {
                            creditorId = NBK_AS_RESPONDENT_ID;
                        }
                    }

                    BaseEntity childMockEntity = new BaseEntity(SqlJdbcConverter.convertToLong(values.get(attribute.getColumnName())),
                            childMetaClass, creditorId, baseEntity.getReportDate());

                    BaseEntity childBaseEntity = loadBaseEntity(childMockEntity, baseEntity.getReportDate(), savingReportDate, false);

                    if (childBaseEntity != null)
                        baseValue = new BaseValue(childBaseEntity);

                    // случай когда по оперативным данным на дату батча отсутствуют значения, сохраняем значение как mock обьект
                    // это нужно для того чтобы не дублировать записи
                    // пример: по кредиту атрибут change является оперативной и так как на дату батча он может не иметь значения
                    // необходимо понимать это так: фактический по кредиту есть значение а то что у change нет значения не важно
                    else if (!metaClass.isOperational() && childMetaClass.isOperational()) {
                        baseValue = new BaseValue(childMockEntity);
                        childMockEntity.setMock(Boolean.TRUE);
                    }
                }
            } else {
                if (metaType.isSet()) {
                    MetaValue metaValue = (MetaValue) ((MetaSet)metaType).getMetaType();
                    /*if (metaValue.getMetaDataType() != MetaDataType.STRING)
                        throw new UsciException("Простые массивы поддерживают только строковые типы данных");*/

                    BaseSet baseSet = new BaseSet(new MetaSet(metaType));

                    if (metaValue.getMetaDataType() == MetaDataType.DATE) {
                        try {
                            OracleArray oracleArray = (OracleArray) sqlValue;

                            Timestamp[] timestampArray = (Timestamp[]) oracleArray.getArray();
                            for (Timestamp timestamp : timestampArray)
                                baseSet.put(new BaseValue(SqlJdbcConverter.convertToLocalDate(timestamp)));

                        } catch (SQLException e) {
                            throw new UsciException(e.getMessage());
                        }
                    } else if (metaValue.getMetaDataType() == MetaDataType.STRING) {
                        try {
                            OracleArray oracleArray = (OracleArray) sqlValue;

                            String[] varcharArray = (String[]) oracleArray.getArray();
                            for (String varchar : varcharArray)
                                baseSet.put(new BaseValue(varchar));

                        } catch (SQLException e) {
                            throw new UsciException(e.getMessage());
                        }
                    } else if (metaValue.getMetaDataType() == MetaDataType.DATE_TIME) {
                        try {
                            OracleArray oracleArray = (OracleArray) sqlValue;

                            Timestamp[] timestampArray = (Timestamp[]) oracleArray.getArray();
                            for (Timestamp timestamp : timestampArray)
                                baseSet.put(new BaseValue(SqlJdbcConverter.convertToLocalDateTime(timestamp)));

                        } catch (SQLException e) {
                            throw new UsciException(e.getMessage());
                        }
                    } else if (metaValue.getMetaDataType() == MetaDataType.BOOLEAN) {
                        try {
                            OracleArray oracleArray = (OracleArray) sqlValue;

                            String[] varcharArray = (String[]) oracleArray.getArray();
                            for (String varchar : varcharArray)
                                baseSet.put(new BaseValue(SqlJdbcConverter.convertVarchar2ToBoolean(varchar)));

                        } catch (SQLException e) {
                            throw new UsciException(e.getMessage());
                        }
                    }

                    baseValue = new BaseValue(baseSet);
                } else
                    baseValue = new BaseValue(EavSqlConverter.convertSqlValueToJavaType(attribute, sqlValue));
            }

            if (baseValue != null)
                baseEntity.put(attribute.getName(), baseValue);
        }
    }

    /**
     * метод загружает сущности комлексного сета который хранится в таблице родительской сущности в столбце как массив id
     * пример: кредит хранит список id залогов в столбце PLEDGE_IDS
     * чтобы загрузить все залоги разом необходимо оборатить столбец ключевым словом table(наименование столбца) кредита
     * пример sql запроса:
     * select p.*
     from EAV_DATA.PLEDGE p,
     EAV_DATA.CREDIT c,
     table(c.PLEDGES_IDS) cp
     where p.ENTITY_ID = cp.COLUMN_VALUE
     and ...
     также необходимо добавить подзапрос чтобы получить последнюю актуальную запись сущности
     * */
    private BaseSet loadComplexSet(BaseEntity baseEntity, MetaAttribute metaAttribute, long[] ids, LocalDate savingReportDate) {
        if (ids == null || ids.length == 0)
            throw new UsciException(String.format("Пустое множество %s %s", baseEntity, metaAttribute.getName()));

        MetaClass metaClass = baseEntity.getMetaClass();
        String tableAlias = metaClass.getClassName().toLowerCase();

        MetaSet childMetaSet = (MetaSet) metaAttribute.getMetaType();
        MetaClass childMetaClass = (MetaClass) childMetaSet.getMetaType();

        String childTableAlias = childMetaClass.getClassName().toLowerCase();

        MapSqlParameterSource params = new MapSqlParameterSource();

        // теоретический если в сете больше 10 тыс то делаю селект join nestedTable иначе беру через выражение "IN"
        boolean fetchNestedTable = ids.length > Constants.BASE_SET_FETCH_SIZE;

        // выбираю максимальную дату из родителя или даты батча
        // это сделано для случая когда подгрузка идет задним числом
        // то есть дата батча может опережать дату сущности
        LocalDate loadingReportDate = savingReportDate.compareTo(baseEntity.getReportDate()) >= 0? savingReportDate: baseEntity.getReportDate();
        if (childMetaClass.isOperational())
            loadingReportDate = savingReportDate;

        StringBuilder queryBuilder = new StringBuilder("select ENTITY_ID,\n");
        queryBuilder.append("REPORT_DATE,\n");
        queryBuilder.append("CREDITOR_ID,\n");
        queryBuilder.append("BATCH_ID,\n");
        queryBuilder.append("OPERATION_ID,\n");
        queryBuilder.append("X_RECEIVED\n");

        // вместо * в select, непосредственно прописываю столбцы которые необходимо получить
        // потому что не все атрибуты могут действовать на отчетную дату
        childMetaClass.getAttributes().forEach(attribute -> {
            queryBuilder.append(",\n");
            queryBuilder.append(attribute.getColumnName());
        });

        queryBuilder.append("\n");
        queryBuilder.append("from $schema.$tableName $tableAlias\n");

        if (fetchNestedTable) {
            queryBuilder.append(",");
            queryBuilder.append("$schema.$parentTableName $parentTableAlias,\n");

            // делаю join столбца массив (массивы NESTED TABLE хранятся в Oracle как физические таблицы)
            queryBuilder.append("table($parentTableAlias.$arrayColumnName) $arrayTableAlias\n");
        }

        queryBuilder.append("where $tableAlias.CREDITOR_ID = :RESPONDENT_ID\n");

        int orCount = (ids.length -1) / 1000;

        if (fetchNestedTable) {
            queryBuilder.append("and $parentTableAlias.ENTITY_ID = :PARENT_ENTITY_ID\n");
            queryBuilder.append("and $parentTableAlias.CREDITOR_ID = :RESPONDENT_ID\n");
            queryBuilder.append("and $parentTableAlias.REPORT_DATE = :PARENT_REPORT_DATE\n");
            queryBuilder.append("and $tableAlias.ENTITY_ID = $arrayTableAlias.COLUMN_VALUE\n");
        }
        else {
            if (ids.length > 1) {
                if (orCount > 0) {
                    queryBuilder.append("and ($tableAlias.ENTITY_ID in ($entityIds0)\n");
                    for (int i = 1; i <= orCount; i++)
                        if (i == orCount) {
                            queryBuilder.append("or $tableAlias.ENTITY_ID in ($entityIds" + i + "))\n");
                        } else {
                            queryBuilder.append("or $tableAlias.ENTITY_ID in ($entityIds" + i + ")\n");
                        }
                } else {
                    queryBuilder.append("and $tableAlias.ENTITY_ID in ($entityIds0)\n");
                }
            }
            else
                queryBuilder.append("and $tableAlias.ENTITY_ID = :ENTITY_ID\n");
        }

        if (childMetaClass.isOperational())
            queryBuilder.append("and $tableAlias.REPORT_DATE = :REPORT_DATE");
        else
            queryBuilder.append("and $tableAlias.REPORT_DATE = \n" +
                                "    (select max($subTableAlias.REPORT_DATE)\n" +
                                "       from $schema.$tableName $subTableAlias\n" +
                                "      where $subTableAlias.ENTITY_ID = $tableAlias.ENTITY_ID\n" +
                                "        and $subTableAlias.CREDITOR_ID = $tableAlias.CREDITOR_ID\n" +
                                "        and $subTableAlias.REPORT_DATE <= :REPORT_DATE)\n");

        String query = queryBuilder.toString();

        query = query.replace("$schema", childMetaClass.getSchemaData());
        query = query.replace("$tableName", childMetaClass.getTableName());
        query = query.replace("$tableAlias", childTableAlias);
        query = query.replace("$subTableAlias", "sub_" + childMetaClass.getTableName().toLowerCase());

        if (fetchNestedTable) {
            query = query.replace("$parentTableName", metaClass.getTableName());
            query = query.replace("$parentTableAlias", tableAlias);
            query = query.replace("$arrayTableAlias", metaAttribute.getColumnName().toLowerCase());
            query = query.replace("$arrayColumnName", metaAttribute.getColumnName());

            params.addValue("PARENT_ENTITY_ID", baseEntity.getId());
            params.addValue("PARENT_REPORT_DATE", SqlJdbcConverter.convertToSqlDate(baseEntity.getReportDate()));
        }
        else {
            if (ids.length > 1) {
                for (int i = 0; i <= orCount; i++) {
                    if (i == 0) {
                        String idsSql = StringUtils.collectionToCommaDelimitedString(Arrays.stream(Arrays.copyOfRange(ids, i * 1000, 1000)).boxed().collect(Collectors.toSet()));
                        query = query.replace("$entityIds" + i, idsSql);
                    } else if (i == orCount) {
                        String idsSql = StringUtils.collectionToCommaDelimitedString(Arrays.stream(Arrays.copyOfRange(ids, i * 1000, ids.length)).boxed().collect(Collectors.toSet()));
                        query = query.replace("$entityIds" + i, idsSql);
                    } else {
                        String idsSql = StringUtils.collectionToCommaDelimitedString(Arrays.stream(Arrays.copyOfRange(ids, i * 1000, (i+1)*1000)).boxed().collect(Collectors.toSet()));
                        query = query.replace("$entityIds" + i, idsSql);
                    }
                }
            }
            else
                params.addValue("ENTITY_ID", ids[0]);
        }

        Long creditorId = baseEntity.getRespondentId();

        if (childMetaClass.isDictionary()) {
            //List<Long> products = productService.getProductIdsByMetaClassId(childMetaClass.getId());

            if (childMetaClass.getProductId().equals(9L)) {
                creditorId = NBK_AS_RESPONDENT_ID;
            }
        }


        params.addValue("RESPONDENT_ID", creditorId);
        params.addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(loadingReportDate));

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query, params);

        if (rows.size() < 1) {
            if (!childMetaClass.isOperational())
                throw new UsciException(String.format("Ошибка загрузки комплексного множества %s %s", baseEntity, metaAttribute.getName()));

            return null;
        }

        BaseSet baseSet = new BaseSet(childMetaClass);
        for (Map<String, Object> row : rows) {
            BaseEntity childBaseSetEntity = new BaseEntity(childMetaClass);

            fillEntityAttributes(row, childBaseSetEntity, savingReportDate);

            baseSet.put(new BaseValue(childBaseSetEntity));
        }

        if (baseSet.getValueCount() < 1)
            throw new UsciException(String.format("Ошибка заполнения множества %s %s", baseEntity, metaAttribute.getName()));

        return baseSet;
    }

    private LocalDate getBaseEntityDate(BaseEntity baseEntity, LocalDate reportDateSaving) {
        // получение максимальной отчетной даты из прошедших периодов
        LocalDate maxReportDate = entityDateService.getMaxReportDate(baseEntity, reportDateSaving);
        if (maxReportDate != null)
            return maxReportDate;

        // получение минимальной отчетной даты из будущих периодов
        // данная опция необходима если идет загрузка задней датой
        // допустим когда кредит в первый* раз был загружен в марте и потом его загружают в феврале
        LocalDate minReportDate = entityDateService.getMinReportDate(baseEntity, reportDateSaving);
        if (minReportDate == null)
            throw new UsciException(String.format("Сущность %s не содержит записей в БД", baseEntity));

        return minReportDate;
    }


    private Map<String, Object> loadBaseEntityIfFail(BaseEntity baseEntity, LocalDate existingReportDate, LocalDate savingReportDate, boolean queryOnDate) {
        Objects.requireNonNull(baseEntity.getId(), String.format("Отсутствует ID у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getRespondentId(), String.format("Отсутствует ID кредитора у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getMetaClass(), String.format("Отсутствует мета класс у сущности %s", baseEntity));
        Objects.requireNonNull(savingReportDate, "Отчетная дата не задана");

        MetaClass metaClass = baseEntity.getMetaClass();

        // получаем отчетную дату для operational=false
        LocalDate loadingReportDate = null;
        if (metaClass.isOperational())
            loadingReportDate = savingReportDate;
        else if (queryOnDate)
            loadingReportDate = existingReportDate;
        else
            loadingReportDate = existingReportDate == null ? savingReportDate: savingReportDate.compareTo(existingReportDate) >= 0 ? savingReportDate : existingReportDate;

        BaseEntity baseEntityLoad = new BaseEntity(baseEntity);

        String tableName = String.join(".", metaClass.getSchemaData(), metaClass.getTableName());

        StringBuilder query = new StringBuilder();
        query.append("select ENTITY_ID,\n");
        query.append("REPORT_DATE,\n");
        query.append("CREDITOR_ID,\n");
        query.append("BATCH_ID,\n");
        query.append("OPERATION_ID,\n");
        query.append("X_RECEIVED\n");

        metaClass.getAttributes().forEach(attribute -> {
            query.append(",\n");
            query.append(attribute.getColumnName());
        });

        query.append("\n");
        query.append("from ");
        query.append(tableName).append(" t1\n");
        query.append("where ENTITY_ID = :ENTITY_ID\n");
        query.append("  and CREDITOR_ID = :RESPONDENT_ID\n");

        if (metaClass.isOperational() || queryOnDate)
            query.append("  and REPORT_DATE = :REPORT_DATE\n");
        else {
            String subQuery = "select max(t2.REPORT_DATE)\n" +
                    "  from " + tableName + " t2\n" +
                    " where t2.ENTITY_ID = :ENTITY_ID\n" +
                    "   and t2.CREDITOR_ID = :RESPONDENT_ID\n" +
                    "   and t2.REPORT_DATE <= :REPORT_DATE\n";
            query.append("  and REPORT_DATE = (").append(subQuery).append(")\n");
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ENTITY_ID", baseEntity.getId())
                .addValue("RESPONDENT_ID", baseEntity.getRespondentId())
                .addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(loadingReportDate));

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query.toString(), params);

        if (rows.size() > 1)
            throw new UsciException(String.format("Найдено более одной записи по сущности %s", baseEntity));

        if (rows.size() < 1) {
            if (!metaClass.isOperational())
                throw new UsciException(String.format("Запись не была найдена по сущности %s", baseEntity));

            return null;
        }

        Map<String, Object> values = rows.get(0);
       // fillEntityAttributes(values, baseEntityLoad, savingReportDate);

        return values;
    }

    private Map<String, Object>  loadBaseEntityFail(BaseEntity baseEntity, LocalDate savingReportDate) {
        // очень важный момент сущность может создаваться после savingReportDate
        // то есть мы не сможем прямо получить ее через max date
        // так что проверяем ее сперва до и потом после даты savingReportDate
        LocalDate reportDate = getBaseEntityDate(baseEntity, savingReportDate);

        return loadBaseEntityIfFail(baseEntity, reportDate, savingReportDate, true);
    }

}

