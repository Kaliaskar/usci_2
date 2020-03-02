package kz.bsbnb.usci.receiver.dao;

import kz.bsbnb.usci.receiver.model.Batch;
import kz.bsbnb.usci.receiver.model.BatchStatusType;

import java.util.List;
import java.util.Map;

public interface BatchDao {

    Batch load(long id);

    long save(Batch batch);

    List<Batch> getPendingBatchList();

    void updateBatchStatus(long batchId, BatchStatusType status);

    void approveMaintenanceBatchList(List<Long> batchIds);

    void declineMaintenanceBatchList(List<Long> batchIds);

    void clearActualCount(long batchId);

    void incrementActualCount(long batchId, long count);

    void setBatchTotalCount(long batchId, long totalCount);

    List<Batch> getBatchFromJdbcMap(List<Map<String, Object>> rows);

}
