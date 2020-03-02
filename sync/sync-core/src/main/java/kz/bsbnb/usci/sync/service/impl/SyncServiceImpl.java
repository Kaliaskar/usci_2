package kz.bsbnb.usci.sync.service.impl;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.sync.SyncStatus;
import kz.bsbnb.usci.sync.bean.SyncStatusSingleton;
import kz.bsbnb.usci.sync.job.DataJob;
import kz.bsbnb.usci.sync.service.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * @author Kanat Tulbassiev
 */

@Service
public class SyncServiceImpl implements SyncService {
    private static final Logger logger = LoggerFactory.getLogger(DataJob.class);

    private final DataJob dataJob;
    private final SyncStatusSingleton syncStatusSingleton;

    public SyncServiceImpl(DataJob dataJob, SyncStatusSingleton syncStatusSingleton) {
        this.dataJob = dataJob;
        this.syncStatusSingleton = syncStatusSingleton;
    }

    @Override
    public int getQueueSize() {
        return dataJob.getQueueSize();
    }

    @Override
    public Set<Long> getFinishedBatches() {
        return dataJob.getFinishedBatches();
    }

    @Override
    public SyncStatus getStatus() {
        return syncStatusSingleton.getStatus();
    }

    @Override
    public void batchFinishedInReader(Long batchId) {
        logger.info("Батч id = {} получен из receiver как обработанный", batchId);

        dataJob.batchFinishedInReader(batchId);
    }

    @Override
    public void process(List<BaseEntity> entities) {
        logger.info("Сущности получены из receiver на обработку {}", entities);

        dataJob.addAll(entities);
    }

}
