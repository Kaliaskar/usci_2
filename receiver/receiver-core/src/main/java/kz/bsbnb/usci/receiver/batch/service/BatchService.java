package kz.bsbnb.usci.receiver.batch.service;

import kz.bsbnb.usci.receiver.model.Batch;
import kz.bsbnb.usci.receiver.model.BatchStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface BatchService {

    Long save(Batch batch);

    Batch getBatch(long batchId);

    Batch uploadBatch(Batch batch);

    void addBatchStatus(BatchStatus batchStatus);

    void cancelBatch(long batchId);

    void endBatch(long batchId);

    List<Batch> getPendingBatchList();

    void approveMaintenanceBatchList(List<Long> batchIds);

    void declineMaintenanceBatchList(List<Long> batchIds);

    List<BatchStatus> getBatchStatusList(List<Long> batchIds);

    void signBatch(long batchId, String sign, String signInfo, LocalDateTime signTime);

    boolean clearActualCount(long batchId);

    boolean incrementActualCounts(Map<Long, Long> batchesToUpdate);

    void setBatchTotalCount(long batchId, long totalCount);

    int getErrorEntitiesCount(Long batchId);

    int getSuccessEntitiesCount(Long batchId);

}
