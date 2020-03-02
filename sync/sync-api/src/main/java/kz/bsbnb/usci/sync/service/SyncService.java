package kz.bsbnb.usci.sync.service;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.sync.SyncStatus;

import java.util.List;
import java.util.Set;

public interface SyncService {

    int getQueueSize();

    Set<Long> getFinishedBatches();

    SyncStatus getStatus();

    void batchFinishedInReader(Long batchId);

    void process(List<BaseEntity> entities);

}
