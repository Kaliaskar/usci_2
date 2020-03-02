package kz.bsbnb.usci.receiver.dao.impl;

import kz.bsbnb.usci.model.batch.Product;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.receiver.dao.BatchDao;
import kz.bsbnb.usci.receiver.model.Batch;
import kz.bsbnb.usci.receiver.model.BatchStatusType;
import kz.bsbnb.usci.receiver.model.BatchType;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Maksat Nussipzhan
 * @author Baurzhan Makhambetov
 * @author Olzhas Kaliaskar
 * @author Jandos Iskakov
 */

@Repository
public class BatchDaoImpl implements BatchDao {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private SimpleJdbcInsert batchInsert;

    @Autowired
    public BatchDaoImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate npJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.npJdbcTemplate = npJdbcTemplate;

        this.batchInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName("USCI_BATCH")
                .withTableName("BATCH")
                .usingGeneratedKeyColumns("ID");
    }

    @Override
    public Batch load(long id) {
        List<Map<String, Object>> list = npJdbcTemplate.queryForList("select * from USCI_BATCH.BATCH t where ID = :ID",
                new MapSqlParameterSource("ID", id));

        if (list.size() != 1)
            throw new UsciException("Ошибка нахождения записи в таблице USCI_BATCH.BATCH");

        return getBatchFromJdbcMap(list).get(0);
    }

    @Override
    public long save(Batch batch) {
        long batchId;

        if (batch.getId() == null) {
            batchId = insertBatch(batch);
            batch.setId(batchId);
        } else {
            updateBatch(batch);
            batchId = batch.getId();
        }

        return batchId;
    }

    private long insertBatch(Batch batch) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("USER_ID", batch.getUserId())
                .addValue("CREDITOR_ID", batch.getRespondentId())
                .addValue("FILE_NAME",batch.getFilePath())
                .addValue("HASH", batch.getHash())
                .addValue("SIGN",  batch.getSignature())
                .addValue("SIGN_INFO", batch.getSignInfo())
                .addValue("SIGN_TIME", SqlJdbcConverter.convertToSqlTimestamp(batch.getSignTime()))
                .addValue("REP_DATE", SqlJdbcConverter.convertToSqlDate(batch.getReportDate()))
                .addValue("RECEIPT_DATE", SqlJdbcConverter.convertToSqlTimestamp(batch.getReceiptDate()))
                .addValue("BATCH_ENTRY_ID", batch.getBatchEntryId())
                .addValue("TOTAL_COUNT", batch.getTotalEntityCount())
                .addValue("ACTUAL_COUNT", batch.getActualEntityCount())
                .addValue("REPORT_ID",  batch.getConfirmId())
                .addValue("PRODUCT_ID", batch.getProduct() != null? batch.getProduct().getId(): null)
                .addValue("IS_DISABLED", 0)
                .addValue("IS_MAINTENANCE", batch.isMaintenance()? 1: 0)
                .addValue("IS_MAINTENANCE_APPROVED", batch.isMaintenanceApproved()? 1: 0)
                .addValue("IS_MAINTENANCE_DECLINED", batch.isMaintenanceDeclined()? 1: 0);

        if (batch.getBatchType() != null)
            params.addValue("BATCH_TYPE", batch.getBatchType());

        Number batchId = batchInsert.executeAndReturnKey(params);

        batch.setId(batchId.longValue());

        return batch.getId();
    }

    private void updateBatch(Batch batch) {
        int count = npJdbcTemplate.update("update USCI_BATCH.BATCH\n" +
                        "   set USER_ID     = :USER_ID,\n" +
                        "   CREDITOR_ID     = :CREDITOR_ID,\n" +
                        "   PRODUCT_ID      = :PRODUCT_ID,\n" +
                        "   FILE_NAME       = :FILE_NAME,\n" +
                        "   HASH            = :HASH,\n" +
                        "   SIGN            = :SIGN,\n" +
                        "   SIGN_INFO       = :SIGN_INFO,\n" +
                        "   SIGN_TIME       = :SIGN_TIME,\n" +
                        "   REP_DATE        = :REP_DATE,\n" +
                        "   RECEIPT_DATE    = :RECEIPT_DATE,\n" +
                        "   BATCH_TYPE      = :BATCH_TYPE,\n" +
                        "   TOTAL_COUNT     = :TOTAL_COUNT,\n" +
                        "   ACTUAL_COUNT    = :ACTUAL_COUNT,\n" +
                        "   REPORT_ID       = :REPORT_ID,\n" +
                        "   IS_MAINTENANCE  = :IS_MAINTENANCE\n" +
                        " where ID = :ID",
                new MapSqlParameterSource("USER_ID", batch.getUserId())
                        .addValue("CREDITOR_ID", batch.getRespondentId())
                        .addValue("FILE_NAME", batch.getFilePath())
                        .addValue("HASH", batch.getHash())
                        .addValue("SIGN", batch.getSignature())
                        .addValue("SIGN_INFO", batch.getSignInfo())
                        .addValue("SIGN_TIME", SqlJdbcConverter.convertToSqlTimestamp(batch.getSignTime()))
                        .addValue("REP_DATE", SqlJdbcConverter.convertToSqlDate(batch.getReportDate()))
                        .addValue("RECEIPT_DATE", SqlJdbcConverter.convertToSqlTimestamp(batch.getReceiptDate()))
                        .addValue("BATCH_TYPE", batch.getBatchType() != null? batch.getBatchType().getCode(): null)
                        .addValue("TOTAL_COUNT", batch.getTotalEntityCount())
                        .addValue("ACTUAL_COUNT", batch.getActualEntityCount())
                        .addValue("REPORT_ID", batch.getConfirmId())
                        .addValue("IS_MAINTENANCE", batch.isMaintenance())
                        .addValue("PRODUCT_ID", batch.getProduct() != null? batch.getProduct().getId(): null)
                        .addValue("ID", batch.getId()));

        if (count != 1)
            throw new UsciException("Ошибка update записи в таблице USCI_BATCH.BATCH");
    }

    @Override
    public void updateBatchStatus(long batchId, BatchStatusType status) {
        int count = jdbcTemplate.update("update USCI_BATCH.BATCH set STATUS_ID = ? where ID = ?", status.getId(), batchId);
        if (count != 1)
            throw new UsciException("Ошибка update записи в таблице USCI_BATCH.BATCH");
    }

    @Override
    //TODO: рефакторинг
    public List<Batch> getPendingBatchList() {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("statusCompleted", BatchStatusType.COMPLETED.getId())
                .addValue("statusError", BatchStatusType.ERROR.getId())
                .addValue("statusCancelled", BatchStatusType.CANCELLED.getId())
                .addValue("statusDeclined", BatchStatusType.MAINTENANCE_DECLINED.getId());

        String query = "select *" +
                       "  from USCI_BATCH.BATCH b\n" +
                       " where b.IS_DISABLED = 0\n" +
                       "   and b.CREDITOR_ID not in (46) \n"+
                      // "   and b.PRODUCT_ID <> 10 \n"+
                       //"   and b.id in (142132)\n"+
                       "   and b.STATUS_ID not in (:statusError, :statusCancelled, :statusCompleted, :statusDeclined)\n" +
                       "   and b.BATCH_TYPE <> 4 order by id\n";
                       //"and (b.BATCH_TYPE <> 4 or (b.BATCH_TYPE = 4 and b.is_maintenance = 1 and b.is_maintenance_approved = 1 and b.receipt_date > to_date('10.01.2020','dd.mm.yyyy')))order by id\n ";

        return getBatchFromJdbcMap(npJdbcTemplate.queryForList(query, params));
    }

    @Override
    public void approveMaintenanceBatchList(List<Long> batchIds) {
        int count = npJdbcTemplate.update("update USCI_BATCH.BATCH set IS_MAINTENANCE_APPROVED = 1 where ID in (:batchIds)",
                new MapSqlParameterSource("batchIds", batchIds));

        if (count != batchIds.size())
            throw new UsciException("Ошибка update записей из таблицы USCI_BATCH.BATCH");
    }

    @Override
    public void declineMaintenanceBatchList(List<Long> batchIds) {
        int count = npJdbcTemplate.update("update USCI_BATCH.BATCH set IS_MAINTENANCE_DECLINED = 1 where ID in (:batchIds)",
                new MapSqlParameterSource("batchIds", batchIds));

        if (count != batchIds.size())
            throw new UsciException("Ошибка update записей из таблицы USCI_BATCH.BATCH");
    }

    @Override
    public void clearActualCount(long batchId) {
        int count = jdbcTemplate.update("update USCI_BATCH.BATCH set ACTUAL_COUNT = 0 where ID = ?", batchId);
        if (count != 1)
            throw new UsciException("Ошибка update записи в таблице USCI_BATCH.BATCH");
    }

    @Override
    public void setBatchTotalCount(long batchId, long totalCount) {
        int count = jdbcTemplate.update("update USCI_BATCH.BATCH set TOTAL_COUNT = ? where ID = ?", totalCount, batchId);
        if (count != 1)
            throw new UsciException("Ошибка update записи в таблице USCI_BATCH.BATCH");
    }

    @Override
    public void incrementActualCount(long batchId, long count) {
        int updates = jdbcTemplate.update("update USCI_BATCH.BATCH set ACTUAL_COUNT = ACTUAL_COUNT + :counter where ID = ?", count, batchId);
        if (updates != 1)
            throw new UsciException("Ошибка update записи в таблице USCI_BATCH.BATCH");
    }

    @Override
    public List<Batch> getBatchFromJdbcMap(List<Map<String, Object>> rows) {
        List<Batch> list = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            Batch batch = new Batch();
            batch.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
            batch.setUserId(SqlJdbcConverter.convertToLong(row.get("USER_ID")));
            batch.setStatus(row.get("STATUS_ID") != null? BatchStatusType.getBatchStatus(SqlJdbcConverter.convertToInt(row.get("STATUS_ID"))): null);
            batch.setBatchEntryId(SqlJdbcConverter.convertToLong(row.get("BATCH_ENTRY_ID")));
            batch.setRespondentId(SqlJdbcConverter.convertToLong(row.get("CREDITOR_ID")));
            batch.setFilePath(SqlJdbcConverter.convertObjectToString((row.get("FILE_NAME"))));
            batch.setProduct(row.get("PRODUCT_ID") != null? new Product(SqlJdbcConverter.convertToLong(row.get("PRODUCT_ID"))): null);
            batch.setHash(SqlJdbcConverter.convertObjectToString(row.get("HASH")));
            batch.setSignature(SqlJdbcConverter.convertObjectToString((row.get("SIGN"))));
            batch.setStatusId(SqlJdbcConverter.convertToLong(row.get("STATUS_ID")));
            batch.setReportDate(SqlJdbcConverter.convertToLocalDate(row.get("REP_DATE")));
            batch.setReceiptDate(SqlJdbcConverter.convertToLocalDateTime(row.get("RECEIPT_DATE")));
            batch.setBatchType(row.get("BATCH_TYPE") != null? BatchType.getBatchTypeByCode(SqlJdbcConverter.convertObjectToString(row.get("BATCH_TYPE"))): null);
            batch.setTotalEntityCount(SqlJdbcConverter.convertToLong(row.get("TOTAL_COUNT")));
            batch.setActualEntityCount(SqlJdbcConverter.convertToLong(row.get("ACTUAL_COUNT")));
            batch.setConfirmId(SqlJdbcConverter.convertToLong(row.get("REPORT_ID")));
            batch.setMaintenance(SqlJdbcConverter.convertToBoolean(row.get("IS_MAINTENANCE")));
            batch.setMaintenanceApproved(SqlJdbcConverter.convertToBoolean(row.get("IS_MAINTENANCE_APPROVED")));
            batch.setMaintenanceDeclined(SqlJdbcConverter.convertToBoolean(row.get("IS_MAINTENANCE_DECLINED")));
            list.add(batch);
        }

        return list;
    }



}
