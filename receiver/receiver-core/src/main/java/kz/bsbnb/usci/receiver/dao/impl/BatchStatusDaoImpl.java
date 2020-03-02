package kz.bsbnb.usci.receiver.dao.impl;

import kz.bsbnb.usci.receiver.dao.BatchStatusDao;
import kz.bsbnb.usci.receiver.model.BatchStatus;
import kz.bsbnb.usci.receiver.model.BatchStatusType;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * @author Maksat Nussipzhan
 * @author Olzhas Kaliaskar
 */

@Repository
public class BatchStatusDaoImpl implements BatchStatusDao {
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private SimpleJdbcInsert batchStatusInsert;

    public BatchStatusDaoImpl(NamedParameterJdbcTemplate npJdbcTemplate) {
        this.npJdbcTemplate = npJdbcTemplate;

        this.batchStatusInsert = new SimpleJdbcInsert(npJdbcTemplate.getJdbcTemplate())
                .withSchemaName("USCI_BATCH")
                .withTableName("BATCH_STATUS")
                .usingGeneratedKeyColumns("ID");
    }

    @Override
    public void insert(BatchStatus batchStatus) {
        batchStatusInsert.execute(new MapSqlParameterSource()
                .addValue("BATCH_ID", batchStatus.getBatchId())
                .addValue("STATUS_ID", batchStatus.getStatus().getId())
                .addValue("RECEIPT_DATE", SqlJdbcConverter.convertToSqlTimestamp(batchStatus.getReceiptDate()))
                .addValue("DESCRIPTION", batchStatus.getText())
                .addValue("EXCEPTION_TRACE", batchStatus.getExceptionTrace()));

        //batchStatus.setId(id.longValue());

        //return batchStatus.getId();
    }

    @Override
    public List<BatchStatus> getList(long batchId) {
        return getList(Collections.singletonList(batchId));
    }

    @Override
    public List<BatchStatus> getList(List<Long> batchIds) {
        int orCount = (batchIds.size() -1) / 1000;

        String query = "select *\n" +
                "from USCI_BATCH.BATCH_STATUS\n" +
                "where ";
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (orCount > 0) {
            query+="BATCH_ID in (:BATCH_IDS0)\n";
            params.addValue("BATCH_IDS0", batchIds.subList(0,1000));
            for (int i = 1; i <= orCount; i++) {
                if (i == orCount) {
                    query += "or BATCH_ID in (:BATCH_IDS" + i + ")\n";
                    params.addValue("BATCH_IDS" + i, batchIds.subList(i * 1000, batchIds.size()));
                } else {
                    query += "or BATCH_ID in (:BATCH_IDS" + i + ")\n";
                    params.addValue("BATCH_IDS" + i, batchIds.subList(i * 1000, (i + 1) * 1000));
                }
            }
        } else {
            query+="BATCH_ID in (:BATCH_IDS0)\n";
            params.addValue("BATCH_IDS0", batchIds);
        }

        query+="order by RECEIPT_DATE desc, STATUS_ID";

        return npJdbcTemplate.query(query,params,new BatchStatusMapper());
    }

    private static class BatchStatusMapper implements RowMapper<BatchStatus> {

        public BatchStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
            BatchStatus batchStatus = new BatchStatus();
            batchStatus.setBatchId(rs.getLong("BATCH_ID"));
            batchStatus.setStatus(BatchStatusType.getBatchStatus(rs.getInt("STATUS_ID")));
            batchStatus.setReceiptDate(SqlJdbcConverter.convertToLocalDateTime(rs.getTimestamp("RECEIPT_DATE")));
            batchStatus.setText(rs.getString("DESCRIPTION"));
            return batchStatus;
        }

    }

}
