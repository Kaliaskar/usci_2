package kz.bsbnb.usci.eav.meta.dao;

import com.google.gson.Gson;
import kz.bsbnb.usci.eav.dao.MetaClassDao;
import kz.bsbnb.usci.eav.model.meta.*;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Artur Tkachenko
 * @author Alexandr Motov
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

@Repository
public class MetaClassDaoImpl implements MetaClassDao {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final SimpleJdbcInsert metaClassInsert;
    private final SimpleJdbcInsert complexSetInsert;
    private final SimpleJdbcInsert simpleSetInsert;
    private final SimpleJdbcInsert complexAttributeInsert;
    private final SimpleJdbcInsert simpleAttributeInsert;
    private static final Gson gson = new Gson();

    @Autowired
    public MetaClassDaoImpl(JdbcTemplate jdbcTemplate,
                            NamedParameterJdbcTemplate npJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.npJdbcTemplate = npJdbcTemplate;

        this.metaClassInsert = new SimpleJdbcInsert(jdbcTemplate)
            .usingColumns("REPORT_DATE", "NAME", "TITLE",
                    "SCHEMA_XML", "SCHEMA_DATA", "TABLE_NAME", "HASH_SIZE",
                    "SEQUENCE_NAME", "HISTORY_TYPE_ID", "UI_CONFIG", "IS_DICTIONARY",
                    "IS_OPERATIONAL", "IS_NEED_APPROVAL", "IS_DELETED", "IS_SYNC",
                    "PRODUCT_ID")
            .withSchemaName("EAV_CORE")
                .withTableName("EAV_M_CLASSES")
            .usingGeneratedKeyColumns("ID");

        this.complexSetInsert = new SimpleJdbcInsert(jdbcTemplate)
                .usingColumns("REPORT_DATE",
                        "CLASS_ID", "REF_CLASS_ID",
                        "NAME", "TITLE",
                        "COLUMN_NAME", "COLUMN_TYPE", "NESTED_TABLE",
                        "IS_KEY", "KEY_CODE", "PARENT_IS_KEY", "SET_KEY_TYPE",
                        "IS_NULLIFY", "IS_REFERENCE", "IS_CUMULATIVE",
                        "IS_DELETED", "IS_SYNC", "IS_REQUIRED")
                .withSchemaName("EAV_CORE")
                .withTableName("EAV_M_COMPLEX_SET")
                .usingGeneratedKeyColumns("ID");

        this.simpleSetInsert = new SimpleJdbcInsert(jdbcTemplate)
                .usingColumns("REPORT_DATE", "CLASS_ID",
                        "NAME", "TITLE", "TYPE_CODE",
                        "COLUMN_NAME", "COLUMN_TYPE", "NESTED_TABLE",
                        "IS_KEY", "KEY_CODE", "SET_KEY_TYPE",
                        "IS_NULLIFY", "IS_CUMULATIVE",
                        "IS_DELETED", "IS_SYNC", "IS_REQUIRED")
                .withSchemaName("EAV_CORE")
                .withTableName("EAV_M_SIMPLE_SET")
                .usingGeneratedKeyColumns("ID");

        this.complexAttributeInsert = new SimpleJdbcInsert(jdbcTemplate)
                .usingColumns("REPORT_DATE",
                        "CLASS_ID", "REF_CLASS_ID",
                        "NAME", "TITLE",
                        "COLUMN_NAME", "COLUMN_TYPE",
                        "IS_KEY", "KEY_CODE", "PARENT_IS_KEY",
                        "IS_NULLIFY", "IS_REFERENCE",
                        "IS_DELETED", "IS_SYNC", "IS_REQUIRED")
                .withSchemaName("EAV_CORE")
                .withTableName("EAV_M_COMPLEX_ATTRIBUTES")
                .usingGeneratedKeyColumns("ID");

        this.simpleAttributeInsert = new SimpleJdbcInsert(jdbcTemplate)
                .usingColumns("REPORT_DATE",
                        "CLASS_ID", "NAME", "TITLE",
                        "TYPE_CODE", "COLUMN_NAME", "COLUMN_TYPE",
                        "IS_KEY", "KEY_CODE",
                        "IS_NULLIFY", "IS_DELETED", "IS_SYNC", "IS_REQUIRED")
                .withSchemaName("EAV_CORE")
                .withTableName("EAV_M_SIMPLE_ATTRIBUTES")
                .usingGeneratedKeyColumns("ID");
    }

    private long createMetaClass(MetaClass metaClass) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(metaClass.getReportDate()))
                .addValue("NAME", metaClass.getClassName())
                .addValue("TITLE", metaClass.getClassTitle())
                .addValue("HISTORY_TYPE_ID", metaClass.getPeriodType().getId())
                .addValue("UI_CONFIG", gson.toJson(metaClass.getUiConfig()))
                .addValue("SCHEMA_XML", metaClass.getSchemaXml())
                .addValue("SCHEMA_DATA", metaClass.getSchemaData())
                .addValue("TABLE_NAME",  metaClass.getTableName())
                .addValue("HASH_SIZE",  metaClass.getHashSize())
                .addValue("SEQUENCE_NAME", metaClass.getSequenceName())
                .addValue("IS_DICTIONARY", metaClass.isDictionary()? "1": "0")
                .addValue("IS_OPERATIONAL", metaClass.isOperational()? "1": "0")
                .addValue("IS_NEED_APPROVAL", "0")
                .addValue("IS_DELETED", metaClass.isDeleted()? "1": "0")
                .addValue("IS_SYNC", "0")
                .addValue("PRODUCT_ID", metaClass.getProductId());

        Number metaClassId = metaClassInsert.executeAndReturnKey(params);

        metaClass.setId(metaClassId.longValue());

        return metaClass.getId();
    }

    private void updateClass(MetaClass metaClass) {
        int count = npJdbcTemplate.update("update EAV_CORE.EAV_M_CLASSES\n" +
                "   set REPORT_DATE       = :REPORT_DATE,\n" +
                "       NAME              = :NAME,\n" +
                "       TITLE             = :TITLE,\n" +
                "       SCHEMA_XML        = :SCHEMA_XML,\n" +
                "       SCHEMA_DATA       = :SCHEMA_DATA,\n" +
                "       TABLE_NAME        = :TABLE_NAME,\n" +
                "       HASH_SIZE         = :HASH_SIZE,\n" +
                "       SEQUENCE_NAME     = :SEQUENCE_NAME,\n" +
                "       HISTORY_TYPE_ID   = :HISTORY_TYPE_ID,\n" +
                        "       UI_CONFIG   = :UI_CONFIG,\n" +
                "       IS_DICTIONARY     = :IS_DICTIONARY,\n" +
                "       IS_OPERATIONAL    = :IS_OPERATIONAL,\n" +
                "       IS_NEED_APPROVAL  = :IS_NEED_APPROVAL,\n" +
                "       IS_DELETED        = :IS_DELETED,\n" +
                "       PRODUCT_ID        = :PRODUCT_ID\n" +
                " where ID = :ID",
                new MapSqlParameterSource("REPORT_DATE", metaClass.getReportDate())
                        .addValue("NAME", metaClass.getClassName())
                        .addValue("TITLE", metaClass.getClassTitle())
                        .addValue("HISTORY_TYPE_ID", metaClass.getPeriodType().getId())
                        .addValue("UI_CONFIG", gson.toJson(metaClass.getUiConfig()))
                        .addValue("SCHEMA_XML", metaClass.getSchemaXml())
                        .addValue("SCHEMA_DATA", metaClass.getSchemaData())
                        .addValue("TABLE_NAME", metaClass.getTableName())
                        .addValue("HASH_SIZE", metaClass.getHashSize())
                        .addValue("SEQUENCE_NAME", metaClass.getSequenceName())
                        .addValue("IS_DICTIONARY", metaClass.isDictionary()? "1": "0")
                        .addValue("IS_OPERATIONAL", metaClass.isOperational()? "1": "0")
                        .addValue("IS_NEED_APPROVAL", "0")
                        .addValue("IS_DELETED", metaClass.isDeleted()? "1": "0")
                        .addValue("PRODUCT_ID", metaClass.getProductId())
                        .addValue("ID", metaClass.getId()));

        if (count != 1)
            throw new UsciException("Ошибка update записи в таблице EAV_CORE.EAV_M_CLASSES");
    }

    /**
     * Сохранение мета класса и его атрибутов в БД
     */
    @Transactional
    @Override
    public long save(MetaClass saveMeta) {
        // если создаем новый мета класс
        if (saveMeta.getId() == null) {
            createMetaClass(saveMeta);

            // создаем мета атрибуты, делаем инсерт потому что их нет было в БД
            insertAttributes(saveMeta, new ArrayList<>(saveMeta.getAttributes()));
        } else {
            // подгружаем старый класс чтобы делать сверку атрибутов
            // то есть выявить новых, удаленных атрибутов
            MetaClass dbMeta = new MetaClass(saveMeta);

            loadClass(dbMeta);
            loadAttributes(dbMeta);

            // чтобы в холостую не теребить БД проверим есть ли в действительности изменения
            // то есть сравним два класса, один из БД, другой из фронтенда
            if (!Objects.equals(saveMeta.getClassName(), dbMeta.getClassName()) ||
                !Objects.equals(saveMeta.getClassTitle(), dbMeta.getClassTitle()) ||
                !Objects.equals(saveMeta.getReportDate(), dbMeta.getReportDate()) ||
                !Objects.equals(saveMeta.getSchemaData(), dbMeta.getSchemaData()) ||
                !Objects.equals(saveMeta.getSchemaXml(), dbMeta.getSchemaXml()) ||
                !Objects.equals(saveMeta.getSequenceName(), dbMeta.getSequenceName()) ||
                !Objects.equals(saveMeta.getTableName(), dbMeta.getTableName()) ||
                !Objects.equals(saveMeta.getHashSize(), dbMeta.getHashSize()) ||
                !Objects.equals(saveMeta.isDeleted(), dbMeta.isDeleted()) ||
                !Objects.equals(saveMeta.isDictionary(), dbMeta.isDictionary()) ||
                !Objects.equals(saveMeta.isOperational(), dbMeta.isOperational()) ||
                    !Objects.equals(saveMeta.getUiConfig(), dbMeta.getUiConfig()) ||
                !Objects.equals(saveMeta.getProductId(), dbMeta.getProductId())) {
                updateClass(saveMeta);
            }

            Map<Long, MetaAttribute> dbAttributes = dbMeta.getAttributes().stream()
                    .collect(Collectors.toMap(MetaAttribute::getId, attr -> attr));

            List<MetaAttribute> insertAttributes = new LinkedList<>();
            List<MetaAttribute> updateAttributes = new LinkedList<>();

            for (MetaAttribute saveAttribute : saveMeta.getAttributes()) {
                if (saveAttribute.getId() == null) {
                    insertAttributes.add(saveAttribute);
                } else {
                    MetaAttribute dbAttribute = dbAttributes.get(saveAttribute.getId());

                    // существующий атрибут; делаем сверку чтобы понять есть ли изменения какие либо
                    // это нужно делать чтобы попросту не посылать update запросы в БД
                    if (!saveAttribute.equals(dbAttribute))
                        updateAttributes.add(saveAttribute);

                    dbAttributes.remove(dbAttribute.getId());
                }
            }

            // то что осталось в мэпе и есть удаленные так как их нет в новом мета классе
            List<MetaAttribute> deleteAttributes = new ArrayList<>(dbAttributes.values());
            if (!deleteAttributes.isEmpty())
                deleteAttributes(deleteAttributes);

            if (!insertAttributes.isEmpty())
                insertAttributes(saveMeta, insertAttributes);

            if (!updateAttributes.isEmpty())
                updateAttributes(updateAttributes);
        }

        return saveMeta.getId();
    }

    private void insertAttributes(MetaClass metaClass, List<MetaAttribute> attributes) {
        for (MetaAttribute metaAttribute : attributes) {
            MetaType metaType = metaAttribute.getMetaType();

            if (metaType.isSet()) {
                createSet(metaClass.getId(), metaAttribute);
            } else {
                createScalarAttribute(metaClass.getId(), metaAttribute);
            }
        }
    }

    private void updateAttributes(List<MetaAttribute> attributes) {
        for (MetaAttribute metaAttribute : attributes) {
            MetaType metaType = metaAttribute.getMetaType();

            MapSqlParameterSource params = new MapSqlParameterSource("ID", metaAttribute.getId())
                    .addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(metaAttribute.getReportDate()))
                    .addValue("NAME", metaAttribute.getName())
                    .addValue("TITLE", metaAttribute.getTitle())
                    .addValue("COLUMN_NAME", metaAttribute.getColumnName())
                    .addValue("COLUMN_TYPE", metaAttribute.getColumnType())
                    .addValue("IS_KEY", metaAttribute.getKeyType())
                    .addValue("KEY_CODE", metaAttribute.getKeySet())
                    .addValue("IS_REQUIRED", metaAttribute.isRequired()? "1": "0")
                    .addValue("IS_NULLIFY", metaAttribute.isNullify()? "1": "0")
                    .addValue("IS_DELETED", metaAttribute.isDeleted()? "1": "0");

            String tableName;

            int count = 0;
            if (metaType.isSet()) {
                MetaSet metaSet = (MetaSet) metaAttribute.getMetaType();

                params.addValue("NESTED_TABLE", metaSet.getNestedTable())
                      .addValue("IS_CUMULATIVE", metaAttribute.isCumulative()? "1": "0")
                      .addValue("SET_KEY_TYPE", metaSet.getKeyType().name());

                if (metaType.isComplex()) {
                    tableName = "EAV_M_COMPLEX_SET";
                    long refClassId = ((MetaClass) metaSet.getMetaType()).getId();

                    params.addValue("PARENT_IS_KEY", metaAttribute.isParentIsKey()? "1": "0")
                          .addValue("IS_REFERENCE", metaAttribute.isReference()? "1": "0")
                          .addValue("REF_CLASS_ID", refClassId);

                    count = npJdbcTemplate.update("update EAV_CORE.EAV_M_COMPLEX_SET\n" +
                            "  set REPORT_DATE     = :REPORT_DATE,\n" +
                            "      REF_CLASS_ID    = :REF_CLASS_ID,\n" +
                            "      NAME            = :NAME,\n" +
                            "      TITLE           = :TITLE,\n" +
                            "      COLUMN_NAME     = :COLUMN_NAME,\n" +
                            "      COLUMN_TYPE     = :COLUMN_TYPE,\n" +
                            "      NESTED_TABLE    = :NESTED_TABLE,\n" +
                            "      IS_KEY          = :IS_KEY,\n" +
                            "      KEY_CODE        = :KEY_CODE,\n" +
                            "      PARENT_IS_KEY   = :PARENT_IS_KEY,\n" +
                            "      SET_KEY_TYPE    = :SET_KEY_TYPE,\n" +
                            "      IS_NULLIFY      = :IS_NULLIFY,\n" +
                            "      IS_REQUIRED     = :IS_REQUIRED,\n" +
                            "      IS_REFERENCE    = :IS_REFERENCE,\n" +
                            "      IS_CUMULATIVE   = :IS_CUMULATIVE,\n" +
                            "      IS_DELETED      = :IS_DELETED\n" +
                            "where ID = :ID", params);
                } else {
                    tableName = "EAV_M_SIMPLE_SET";
                    params.addValue("TYPE_CODE", metaAttribute.isParentIsKey()? "1": "0");

                    count = npJdbcTemplate.update("update EAV_CORE.EAV_M_SIMPLE_SET\n" +
                                    "  set REPORT_DATE     = :REPORT_DATE,\n" +
                                    "      NAME            = :NAME,\n" +
                                    "      TITLE           = :TITLE,\n" +
                                    "      TYPE_CODE       = :TYPE_CODE,\n" +
                                    "      COLUMN_NAME     = :COLUMN_NAME,\n" +
                                    "      COLUMN_TYPE     = :COLUMN_TYPE,\n" +
                                    "      NESTED_TABLE    = :NESTED_TABLE,\n" +
                                    "      IS_KEY          = :IS_KEY,\n" +
                                    "      KEY_CODE        = :KEY_CODE,\n" +
                                    "      SET_KEY_TYPE    = :SET_KEY_TYPE,\n" +
                                    "      IS_REQUIRED     = :IS_REQUIRED,\n" +
                                    "      IS_NULLIFY      = :IS_NULLIFY,\n" +
                                    "      IS_CUMULATIVE   = :IS_CUMULATIVE,\n" +
                                    "      IS_DELETED      = :IS_DELETED\n" +
                                    "where ID = :ID", params);
                }
            } else {
                if (metaType.isComplex()) {
                    tableName = "EAV_M_COMPLEX_ATTRIBUTES";
                    long refClassId = ((MetaClass) metaType).getId();

                    params.addValue("PARENT_IS_KEY", metaAttribute.isParentIsKey()? "1": "0")
                          .addValue("IS_REFERENCE", metaAttribute.isReference()? "1": "0")
                          .addValue("REF_CLASS_ID", refClassId);

                    count = npJdbcTemplate.update("update EAV_CORE.EAV_M_COMPLEX_ATTRIBUTES\n" +
                                    "  set REPORT_DATE     = :REPORT_DATE,\n" +
                                    "      REF_CLASS_ID    = :REF_CLASS_ID,\n" +
                                    "      NAME            = :NAME,\n" +
                                    "      TITLE           = :TITLE,\n" +
                                    "      COLUMN_NAME     = :COLUMN_NAME,\n" +
                                    "      COLUMN_TYPE     = :COLUMN_TYPE,\n" +
                                    "      IS_KEY          = :IS_KEY,\n" +
                                    "      KEY_CODE        = :KEY_CODE,\n" +
                                    "      PARENT_IS_KEY   = :PARENT_IS_KEY,\n" +
                                    "      IS_REQUIRED     = :IS_REQUIRED,\n" +
                                    "      IS_NULLIFY      = :IS_NULLIFY,\n" +
                                    "      IS_REFERENCE    = :IS_REFERENCE,\n" +
                                    "      IS_DELETED      = :IS_DELETED\n" +
                                    "where ID = :ID", params);
                } else {
                    tableName = "EAV_M_SIMPLE_ATTRIBUTES";
                    params.addValue("TYPE_CODE", ((MetaValue) metaType).getMetaDataType().name());

                    count = npJdbcTemplate.update("update EAV_CORE.EAV_M_SIMPLE_ATTRIBUTES\n" +
                                    "  set REPORT_DATE     = :REPORT_DATE,\n" +
                                    "      NAME            = :NAME,\n" +
                                    "      TITLE           = :TITLE,\n" +
                                    "      TYPE_CODE       = :TYPE_CODE,\n" +
                                    "      COLUMN_NAME     = :COLUMN_NAME,\n" +
                                    "      COLUMN_TYPE     = :COLUMN_TYPE,\n" +
                                    "      IS_KEY          = :IS_KEY,\n" +
                                    "      KEY_CODE        = :KEY_CODE,\n" +
                                    "      IS_REQUIRED     = :IS_REQUIRED,\n" +
                                    "      IS_NULLIFY      = :IS_NULLIFY,\n" +
                                    "      IS_DELETED      = :IS_DELETED\n" +
                                    "where ID = :ID", params);
                }
            }

            if (count == 0)
                throw new UsciException("Ошибка update записи в таблице EAV_CORE." + tableName);
        }
    }

    private void deleteAttributes(List<MetaAttribute> attributes) {
        for (MetaAttribute metaAttribute : attributes) {
            MetaType metaType = metaAttribute.getMetaType();

            String tableName;

            if (metaType.isComplex()) {
                if (!metaType.isSet()) {
                    tableName = "EAV_M_COMPLEX_ATTRIBUTES";
                } else {
                    tableName = "EAV_M_COMPLEX_SET";
                }
            } else {
                if (!metaType.isSet()) {
                    tableName = "EAV_M_SIMPLE_ATTRIBUTES";
                } else {
                    tableName = "EAV_M_SIMPLE_SET";
                }
            }

            int count = jdbcTemplate.update(String.format("delete from EAV_CORE.%s where ID = ?", tableName), metaAttribute.getId());
            if (count != 1)
                throw new UsciException(String.format("Ошибка delete записи из таблицы %s атрибутов", tableName));
        }
    }

    private long createSet(long classId, MetaAttribute metaAttribute) {
        MetaType metaType = metaAttribute.getMetaType();
        if (!metaType.isSet())
            throw new IllegalStateException("Аттрибут не является массивом");

        Number id = null;
        MetaSet metaSet = (MetaSet) metaAttribute.getMetaType();
        if (metaAttribute.getKeySet() == 1)
            metaSet.setKeyType(SetKeyType.ANY);


        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(metaAttribute.getReportDate()))
                .addValue("CLASS_ID", classId)
                .addValue("NAME", metaAttribute.getName())
                .addValue("TITLE", metaAttribute.getTitle())
                .addValue("COLUMN_NAME", metaAttribute.getColumnName())
                .addValue("COLUMN_TYPE", metaAttribute.getColumnType())
                .addValue("NESTED_TABLE", metaSet.getNestedTable())
                .addValue("IS_KEY", metaAttribute.getKeyType())
                .addValue("KEY_CODE", metaAttribute.getKeySet())
                .addValue("SET_KEY_TYPE", metaSet.getKeyType().name())
                .addValue("IS_REQUIRED", metaAttribute.isRequired()? "1": "0")
                .addValue("IS_NULLIFY", metaAttribute.isNullify()? "1": "0")
                .addValue("IS_CUMULATIVE", metaAttribute.isCumulative()? "1": "0")
                .addValue("IS_DELETED", metaAttribute.isDeleted()? "1": "0")
                .addValue("IS_SYNC", "0");

        if (metaSet.isComplex()) {
            long refClassId = ((MetaClass) metaSet.getMetaType()).getId();

            params.addValue("REF_CLASS_ID", refClassId)
                    .addValue("PARENT_IS_KEY", metaAttribute.isParentIsKey()? "1": "0")
                    .addValue("IS_REFERENCE", metaAttribute.isReference()? "1": "0");

            id = complexSetInsert.executeAndReturnKey(params);
        } else {
            params.addValue("TYPE_CODE", metaSet.getMetaDataType().name());

            id = simpleSetInsert.executeAndReturnKey(params);
        }

        metaSet.setId(id.longValue());

        return metaSet.getId();
    }

    private long createScalarAttribute(Long classId, MetaAttribute metaAttribute) {
        MetaType metaType = metaAttribute.getMetaType();
        if (metaType.isSet())
            throw new IllegalStateException("Аттрибут является массивом");

        Number id = null;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(metaAttribute.getReportDate()))
                .addValue("CLASS_ID", classId)
                .addValue("NAME", metaAttribute.getName())
                .addValue("TITLE", metaAttribute.getTitle())
                .addValue("COLUMN_NAME", metaAttribute.getColumnName())
                .addValue("COLUMN_TYPE", metaAttribute.getColumnType())
                .addValue("IS_KEY", metaAttribute.getKeyType())
                .addValue("KEY_CODE", metaAttribute.getKeySet())
                .addValue("IS_REQUIRED", metaAttribute.isRequired()? "1": "0")
                .addValue("IS_NULLIFY", metaAttribute.isNullify()? "1": "0")
                .addValue("IS_DELETED", metaAttribute.isDeleted()? "1": "0")
                .addValue("IS_SYNC", "0");

        if (metaType.isComplex()) {
            long refClassId = ((MetaClass) metaType).getId();

            params.addValue("REF_CLASS_ID", refClassId)
                    .addValue("PARENT_IS_KEY", metaAttribute.isParentIsKey()? "1": "0")
                    .addValue("IS_REFERENCE", metaAttribute.isReference()? "1": "0");

            id = complexAttributeInsert.executeAndReturnKey(params);
        } else {
            params.addValue("TYPE_CODE", ((MetaValue) metaType).getMetaDataType().name());

            id = simpleAttributeInsert.executeAndReturnKey(params);
        }

        metaAttribute.setId(id.longValue());

        return metaAttribute.getId();
    }

    @Override
    public MetaClass load(String className) {
        MetaClass metaClass = new MetaClass(className);

        loadClass(metaClass);
        loadAttributes(metaClass);

        return metaClass;
    }

    @Override
    public List<MetaClass> loadAll() {
        List<MetaClass> metaClasses = npJdbcTemplate.query("select * from EAV_CORE.EAV_M_CLASSES", new MetaClassMapper());

        metaClasses.forEach(this::loadAttributes);

        return metaClasses;
    }

    @Override
    public MetaClass load(Long id) {
        if (id == null)
            return null;

        MetaClass meta = new MetaClass();
        meta.setId(id);

        loadClass(meta);
        loadAttributes(meta);

        return meta;
    }

    private void loadClass(MetaClass metaClass) {
        Objects.requireNonNull(metaClass, "Мета класс равен null");

        String query;
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (metaClass.getId() == null) {
            if (metaClass.getClassName() == null)
                throw new UsciException("Мета класс не имеет имя или идентификатор. Не удается загрузить");

            query = "select * from EAV_CORE.EAV_M_CLASSES where NAME = :className";
            params.addValue("className", metaClass.getClassName());
        } else {
            query = "select * from EAV_CORE.EAV_M_CLASSES where ID = :id";
            params.addValue("id", metaClass.getId());
        }

        metaClass = npJdbcTemplate.queryForObject(query, params, new MetaClassMapper(metaClass));

        loadAttributes(metaClass);
    }

    private void loadAttributes(MetaClass meta) {
        if (meta.getId() == null)
            throw new UsciException("Невозможно загрузить аттрибуты метакласса без ID");

        meta.removeAttributes();

        loadSimpleAttributes(meta);
        loadSimpleArrays(meta);
        loadComplexAttributes(meta);
        loadComplexArrays(meta);
    }

    private void loadSimpleAttributes(MetaClass metaClass) {
        List<MetaAttribute> attributes = npJdbcTemplate.query("select * from EAV_CORE.EAV_M_SIMPLE_ATTRIBUTES where CLASS_ID = :classId",
            new MapSqlParameterSource("classId", metaClass.getId()), (rs, i) -> {
                MetaAttribute metaAttribute = new MetaAttribute(rs.getLong("id"));

                metaAttribute.setTitle(rs.getString("title"));
                metaAttribute.setName(rs.getString("name"));
                metaAttribute.setReportDate(SqlJdbcConverter.convertToLocalDate(rs.getDate("report_date")));
                metaAttribute.setColumnName(rs.getString("column_name"));
                metaAttribute.setColumnType(rs.getString("column_type"));
                metaAttribute.setMetaType(new MetaValue(MetaDataType.valueOf(rs.getString("type_code"))));
                metaAttribute.setKeyType(Byte.valueOf(rs.getString("is_key")));
                metaAttribute.setKeySet(SqlJdbcConverter.convertToShort(rs.getShort("key_code")));
                metaAttribute.setNullify(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("is_nullify")));
                metaAttribute.setRequired(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("is_required")));
                metaAttribute.setSync(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("is_sync")));

                return metaAttribute;
            });

        for (MetaAttribute row : attributes)
            metaClass.setMetaAttribute(row.getName(), row);
    }

    private void loadSimpleArrays(MetaClass metaClass) {
        List<MetaAttribute> attributes = npJdbcTemplate.query("select * from EAV_CORE.EAV_M_SIMPLE_SET where CLASS_ID = :classId",
            new MapSqlParameterSource("classId", metaClass.getId()), (rs, i) -> {
                MetaAttribute metaAttribute = new MetaAttribute(rs.getLong("id"));

                metaAttribute.setName(rs.getString("name"));
                metaAttribute.setTitle(rs.getString("title"));
                metaAttribute.setReportDate(SqlJdbcConverter.convertToLocalDate(rs.getDate("report_date")));
                metaAttribute.setColumnName(rs.getString("column_name"));
                metaAttribute.setColumnType(rs.getString("column_type"));
                metaAttribute.setMetaType(new MetaValue(MetaDataType.valueOf(rs.getString("type_code"))));
                metaAttribute.setKeyType(Byte.valueOf(rs.getString("is_key")));
                metaAttribute.setCumulative(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("is_cumulative")));
                metaAttribute.setKeySet(SqlJdbcConverter.convertToShort(rs.getShort("key_code")));
                metaAttribute.setRequired(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("is_required")));
                metaAttribute.setNullify(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("is_nullify")));
                metaAttribute.setSync(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("is_sync")));

                MetaSet metaSet = new MetaSet(new MetaValue(MetaDataType.valueOf(rs.getString("type_code"))));
                metaSet.setId(SqlJdbcConverter.convertToLong(rs.getLong("id")));
                metaSet.setKeyType(SetKeyType.valueOf(rs.getString("set_key_type")));
                metaSet.setNestedTable(rs.getString("nested_table"));

                metaAttribute.setMetaType(metaSet);

                return metaAttribute;
            });

        for (MetaAttribute row : attributes)
            metaClass.setMetaAttribute(row.getName(), row);
    }

    private void loadComplexAttributes(MetaClass metaClass) {
        List<MetaAttribute> attributes = npJdbcTemplate.query("select * from EAV_CORE.EAV_M_COMPLEX_ATTRIBUTES where CLASS_ID = :classId",
            new MapSqlParameterSource("classId", metaClass.getId()), (rs, i) -> {
                MetaAttribute metaAttribute = new MetaAttribute(rs.getLong("id"));

                metaAttribute.setTitle(rs.getString("title"));
                metaAttribute.setName(rs.getString("name"));
                metaAttribute.setReportDate(SqlJdbcConverter.convertToLocalDate(rs.getDate("report_date")));
                metaAttribute.setColumnName(rs.getString("column_name"));
                metaAttribute.setColumnType(rs.getString("column_type"));
                metaAttribute.setMetaType(load(rs.getLong("ref_class_id")));
                metaAttribute.setKeyType(Byte.valueOf(rs.getString("is_key")));

                metaAttribute.setRequired(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("is_required")));
                metaAttribute.setNullify(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("is_nullify")));
                metaAttribute.setReference(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("is_reference")));
                metaAttribute.setParentIsKey(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("parent_is_key")));
                metaAttribute.setSync(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("is_sync")));

                return metaAttribute;
            });

        for (MetaAttribute row : attributes)
            metaClass.setMetaAttribute(row.getName(), row);
    }

    private void loadComplexArrays(MetaClass metaClass) {
        List<MetaAttribute> attributes = npJdbcTemplate.query("select * from EAV_CORE.EAV_M_COMPLEX_SET where CLASS_ID = :classId",
            new MapSqlParameterSource("classId", metaClass.getId()), (rs, i) -> {
                MetaAttribute metaAttribute = new MetaAttribute(rs.getLong("id"));

                metaAttribute.setName(rs.getString("name"));
                metaAttribute.setTitle(rs.getString("title"));
                metaAttribute.setReportDate(SqlJdbcConverter.convertToLocalDate(rs.getDate("report_date")));
                metaAttribute.setColumnName(rs.getString("column_name"));
                metaAttribute.setColumnType(rs.getString("column_type"));
                metaAttribute.setKeyType(Byte.valueOf(rs.getString("is_key")));
                metaAttribute.setKeySet(SqlJdbcConverter.convertToShort(rs.getShort("key_code")));

                metaAttribute.setRequired(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("is_required")));
                metaAttribute.setCumulative(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("is_cumulative")));
                metaAttribute.setNullify(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("is_nullify")));
                metaAttribute.setReference(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("is_reference")));
                metaAttribute.setParentIsKey(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("parent_is_key")));
                metaAttribute.setSync(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("is_sync")));

                MetaSet metaSet = new MetaSet(load(rs.getLong("ref_class_id")));
                metaSet.setId(SqlJdbcConverter.convertToLong(rs.getLong("id")));
                metaSet.setKeyType(SetKeyType.valueOf(rs.getString("set_key_type")));
                metaSet.setNestedTable(rs.getString("nested_table"));

                metaAttribute.setMetaType(metaSet);

                return metaAttribute;
            });

        for (MetaAttribute row : attributes)
            metaClass.setMetaAttribute(row.getName(), row);
    }

    @Override
    @Transactional
    public void remove(MetaClass metaClass) {
        int count = jdbcTemplate.update("update EAV_CORE.EAV_M_CLASSES\n" +
                "set IS_DELETED = 1,\n" +
                "    IS_SYNC = 0\n" +
                "where ID = ?", metaClass.getId());

        if (count != 0)
            throw new UsciException("Ошибка update записи в таблице EAV_CORE.EAV_M_CLASSES");
    }

    @Override
    public List<Long> loadClassesWithReference(Long id) {
        return npJdbcTemplate.queryForList("select CLASS_ID\n" +
                "  from EAV_CORE.EAV_M_COMPLEX_SET t\n" +
                " where REF_CLASS_ID = :classId\n" +
                "union\n" +
                "select CLASS_ID\n" +
                "  from EAV_CORE.EAV_M_COMPLEX_ATTRIBUTES t\n" +
                " where REF_CLASS_ID = :classId\n",
                new MapSqlParameterSource("classId", id),
                Long.class);
    }

    private class MetaClassMapper implements RowMapper<MetaClass> {
        private MetaClass initMetaClass;

        MetaClassMapper() {
            /*Пустой конструктор*/
        }

        MetaClassMapper(MetaClass initMetaClass) {
            this.initMetaClass = initMetaClass;
        }

        public MetaClass mapRow(ResultSet rs, int rowNum) throws SQLException {
            MetaClass metaClass = initMetaClass == null? new MetaClass(): initMetaClass;
            metaClass.setId(rs.getLong("id"));
            metaClass.setReportDate(SqlJdbcConverter.convertToLocalDate(rs.getDate("report_date")));
            metaClass.setProductId(rs.getLong("product_id"));
            metaClass.setClassName(rs.getString("name"));
            metaClass.setClassTitle(rs.getString("title"));
            metaClass.setSchemaXml(rs.getString("schema_xml"));
            metaClass.setSchemaData(rs.getString("schema_data"));
            metaClass.setSequenceName(rs.getString("sequence_name"));
            metaClass.setTableName(rs.getString("table_name"));
            metaClass.setHashSize(rs.getShort("hash_size"));
            metaClass.setDictionary(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("is_dictionary")));

            metaClass.setOperational(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("is_operational")));
            metaClass.setPeriodType(PeriodType.getPeriodType(rs.getLong("history_type_id")));
            metaClass.setSync(SqlJdbcConverter.convertVarchar2ToBoolean(rs.getString("is_sync")));
            return metaClass;
        }
    }

}
