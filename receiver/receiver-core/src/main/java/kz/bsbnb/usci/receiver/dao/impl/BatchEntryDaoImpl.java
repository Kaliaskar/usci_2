package kz.bsbnb.usci.receiver.dao.impl;

import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.receiver.dao.BatchEntryDao;
import kz.bsbnb.usci.receiver.model.BatchEntry;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Artur Tkachenko
 * @author Jandos Iskakov
 */

@Repository
public class BatchEntryDaoImpl implements BatchEntryDao {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final SimpleJdbcInsert batchEntryInsert;

    public BatchEntryDaoImpl(JdbcTemplate jdbcTemplate,
                             NamedParameterJdbcTemplate npJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.npJdbcTemplate = npJdbcTemplate;

        this.batchEntryInsert = new SimpleJdbcInsert(jdbcTemplate)
            .withSchemaName("USCI_BATCH")
            .withTableName("BATCH_ENTRY")
            .usingColumns("USER_ID", "VALUE", "REPORT_DATE", "META_CLASS_ID", "IS_DELETED",
                    "UPDATED_DATE", "ENTITY_ID", "IS_MAINTENANCE", "IS_PROCESSED")
            .usingGeneratedKeyColumns("ID");
    }

    @Override
    public BatchEntry load(Long id) {
        List<Map<String, Object>> list = npJdbcTemplate.queryForList("select * from USCI_BATCH.BATCH_ENTRY t where ID = :ID",
                new MapSqlParameterSource("ID", id));

        if (list.size() != 1)
            throw new UsciException("Ошибка нахождения записи в таблице USCI_BATCH.BATCH");

        return getBatchEntryFromJdbcMap(list).get(0);
    }

    @Override
    public Long save(BatchEntry batchEntry) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("USER_ID", batchEntry.getUserId())
                .addValue("VALUE", batchEntry.getValue())
                .addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(batchEntry.getRepDate()))
                .addValue("UPDATED_DATE", SqlJdbcConverter.convertToSqlTimestamp(batchEntry.getUpdateDate()))
                .addValue("ENTITY_ID",  batchEntry.getEntityId())
                .addValue("IS_MAINTENANCE", batchEntry.isMaintenance())
                .addValue("META_CLASS_ID", batchEntry.getMetaClassId())
                .addValue("IS_DELETED", 0)
                .addValue("IS_PROCESSED", batchEntry.getProcessed()? 1: 0);

        Number batchId = batchEntryInsert.executeAndReturnKey(params);

        batchEntry.setId(batchId.longValue());

        return batchEntry.getId();
    }

    @Override
    public List<BatchEntry> getBatchEntriesByUserId(Long userId) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList("select *\n" +
                        "from USCI_BATCH.BATCH_ENTRY t\n" +
                        "where USER_ID = :USER_ID\n" +
                        "  and IS_PROCESSED = 0\n" +
                        "  and IS_DELETED = 0", userId);

        return getBatchEntryFromJdbcMap(list);
    }

    @Override
    public void remove(Long id) {
        int count = jdbcTemplate.update("update USCI_BATCH.BATCH_ENTRY set IS_DELETED = 1 where ID = ?", id);
        if (count != 1)
            throw new UsciException("Ошибка update записи в таблице USCI_BATCH.BATCH_ENTRY");
    }

    @Override
    public void setProcessed(List<Long> batchEntryIds) {
        int count = npJdbcTemplate.update("update USCI_BATCH.BATCH_ENTRY set IS_PROCESSED = 1 where ID in (:ids)",
                new MapSqlParameterSource("ids", batchEntryIds));

        if (count != batchEntryIds.size())
            throw new UsciException("Ошибка update записей из таблицы USCI_BATCH.BATCH_ENTRY");
    }

    private static List<BatchEntry> getBatchEntryFromJdbcMap(List<Map<String, Object>> rows) {
        List<BatchEntry> list = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            BatchEntry batch = new BatchEntry();

            batch.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
            batch.setUserId(SqlJdbcConverter.convertToLong(row.get("USER_ID")));
            batch.setEntityId(SqlJdbcConverter.convertToLong(row.get("ENTITY_ID")));
            batch.setMetaClassId(SqlJdbcConverter.convertToLong(row.get("META_CLASS_ID")));
            batch.setValue(String.valueOf(row.get("VALUE")));
            batch.setUpdateDate(SqlJdbcConverter.convertToLocalDateTime(row.get("UPDATED_DATE")));
            batch.setRepDate(SqlJdbcConverter.convertToLocalDate(row.get("REPORT_DATE")));
            batch.setMaintenance(SqlJdbcConverter.convertToBoolean(row.get("IS_MAINTENANCE")));

            list.add(batch);
        }

        return list;
    }

}
