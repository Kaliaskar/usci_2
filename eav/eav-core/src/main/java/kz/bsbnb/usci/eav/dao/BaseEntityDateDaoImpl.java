package kz.bsbnb.usci.eav.dao;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.core.BaseEntityDate;
import kz.bsbnb.usci.eav.model.core.BaseEntityUtils;
import kz.bsbnb.usci.eav.model.meta.MetaAttribute;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.persistence.Persistable;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class BaseEntityDateDaoImpl implements BaseEntityDateDao {
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private SimpleJdbcInsert baseEntityDateInsert;

    public BaseEntityDateDaoImpl(NamedParameterJdbcTemplate npJdbcTemplate) {
        this.npJdbcTemplate = npJdbcTemplate;

        this.baseEntityDateInsert = new SimpleJdbcInsert(npJdbcTemplate.getJdbcTemplate())
                .withSchemaName("EAV_DATA")
                .withTableName("EAV_ENTITY_DATE")
                .usingColumns("CREDITOR_ID", "ENTITY_ID", "CLASS_ID", "REPORT_DATE",
                        "X_RECEIVED", "BATCH_ID", "SYSTEM_DATE");
    }

    @Override
    public void insert(List<BaseEntityDate> baseEntityDates) {
        List<MapSqlParameterSource> params = new ArrayList<>();

        for (BaseEntityDate baseEntityDate : baseEntityDates)
            params.add(new MapSqlParameterSource()
                .addValue("CREDITOR_ID", baseEntityDate.getBaseEntity().getRespondentId())
                .addValue("ENTITY_ID", baseEntityDate.getBaseEntity().getId())
                .addValue("CLASS_ID", baseEntityDate.getBaseEntity().getMetaClass().getId())
                .addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(baseEntityDate.getReportDate()))
                .addValue("X_RECEIVED", BaseEntityUtils.getReceivedAsString(baseEntityDate.getBaseEntity().getMetaClass(), baseEntityDate.getReceived()))
                .addValue("BATCH_ID", baseEntityDate.getBatchId())
                .addValue("SYSTEM_DATE", SqlJdbcConverter.convertToSqlTimestamp(baseEntityDate.getSystemDate())));

        if (baseEntityDates.size() > 1) {
            int counts[] = baseEntityDateInsert.executeBatch(params.toArray(new SqlParameterSource[0]));
            if (Arrays.stream(counts).anyMatch(value -> value != 1))
                throw new UsciException(String.format("Ошибка insert(batch) записей %s в таблицу EAV_DATA.EAV_ENTITY_DATE", baseEntityDates));
        }
        else {
            int count = baseEntityDateInsert.execute(params.get(0));
            if (count != 1)
                throw new UsciException(String.format("Ошибка insert записи %s в таблицу EAV_DATA.EAV_ENTITY_DATE", baseEntityDates.get(0)));
        }
    }

    @Override
    public void update(BaseEntityDate baseEntityDate) {
        int count = npJdbcTemplate.update("update EAV_DATA.EAV_ENTITY_DATE\n" +
                        "  set X_RECEIVED = :X_RECEIVED,\n" +
                        "      BATCH_ID = :BATCH_ID,\n" +
                        "      SYSTEM_DATE = :SYSTEM_DATE\n" +
                        "where CREDITOR_ID = :RESPONDENT_ID\n" +
                        "  and CLASS_ID = :CLASS_ID\n" +
                        "  and ENTITY_ID = :ENTITY_ID\n" +
                        "  and REPORT_DATE = :REPORT_DATE",
                new MapSqlParameterSource()
                    .addValue("RESPONDENT_ID", baseEntityDate.getBaseEntity().getRespondentId())
                    .addValue("CLASS_ID", baseEntityDate.getBaseEntity().getMetaClass().getId())
                    .addValue("ENTITY_ID", baseEntityDate.getBaseEntity().getId())
                    .addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(baseEntityDate.getReportDate()))
                    .addValue("X_RECEIVED", BaseEntityUtils.getReceivedAsString(baseEntityDate.getBaseEntity().getMetaClass(), baseEntityDate.getReceived()))
                    .addValue("BATCH_ID", baseEntityDate.getBatchId())
                    .addValue("SYSTEM_DATE", SqlJdbcConverter.convertToSqlTimestamp(LocalDateTime.now())));

        if (count == 0)
            throw new UsciException(String.format("Ошибка update записи %s в таблице EAV_DATA.EAV_ENTITY_DATE", baseEntityDate));
    }

    @Override
    public Optional<BaseEntityDate> find(BaseEntity baseEntity) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("RESPONDENT_ID", baseEntity.getRespondentId())
                .addValue("META_CLASS_ID", baseEntity.getMetaClass().getId())
                .addValue("ENTITY_ID", baseEntity.getId())
                .addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(baseEntity.getReportDate()));

            return Optional.ofNullable(npJdbcTemplate.queryForObject("select *\n" +
                            "  from EAV_DATA.EAV_ENTITY_DATE\n" +
                            " where CREDITOR_ID = :RESPONDENT_ID\n" +
                            "   and CLASS_ID = :META_CLASS_ID\n" +
                            "   and ENTITY_ID = :ENTITY_ID\n" +
                            "   and REPORT_DATE = :REPORT_DATE\n", params,
                    new BaseEntityDateMapper(baseEntity, baseEntity.getMetaClass(), null)));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (IncorrectResultSizeDataAccessException e) {
            throw new UsciException(String.format("Найдено более одной записи в таблице EAV_DATA.EAV_ENTITY_DATE по сущности %s", baseEntity));
        }
    }

    @Override
    public List<BaseEntityDate> find(BaseEntity baseEntity, LocalDate reportDate) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("RESPONDENT_ID", baseEntity.getRespondentId())
                .addValue("META_CLASS_ID", baseEntity.getMetaClass().getId())
                .addValue("ENTITY_ID", baseEntity.getId())
                .addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(reportDate));

        return npJdbcTemplate.query("select *\n" +
                "  from EAV_DATA.EAV_ENTITY_DATE\n" +
                " where CREDITOR_ID = :RESPONDENT_ID\n" +
                "   and CLASS_ID = :META_CLASS_ID\n" +
                "   and ENTITY_ID = :ENTITY_ID\n" +
                "   and REPORT_DATE > :REPORT_DATE\n" +
                " order by REPORT_DATE", params,
                new BaseEntityDateMapper(baseEntity, baseEntity.getMetaClass(),
                        baseEntity.getMetaClass().getAttributes().stream()
                                .sorted(Comparator.comparing(Persistable::getId))
                                .collect(Collectors.toList())));
    }

    private class BaseEntityDateMapper implements RowMapper<BaseEntityDate> {
        private BaseEntity baseEntity;
        private MetaClass metaClass;
        private List<MetaAttribute> attributes;

        BaseEntityDateMapper(BaseEntity baseEntity, MetaClass metaClass, List<MetaAttribute> attributes) {
            this.baseEntity = baseEntity;
            this.metaClass = metaClass;
            this.attributes = attributes;
        }

        public BaseEntityDate mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new BaseEntityDate(baseEntity,
                    SqlJdbcConverter.convertToLocalDate(rs.getDate("REPORT_DATE")),
                    attributes != null? BaseEntityUtils.getReceivedAsSet(rs.getString("X_RECEIVED"), attributes):
                            BaseEntityUtils.getReceivedAsSet(metaClass, rs.getString("X_RECEIVED")),
                    rs.getLong("BATCH_ID"),
                    SqlJdbcConverter.convertToLocalDateTime(rs.getTimestamp("SYSTEM_DATE")));
        }
    }

}
