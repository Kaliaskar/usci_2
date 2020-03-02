package kz.bsbnb.usci.receiver.queue;

import kz.bsbnb.usci.receiver.model.Batch;
import kz.bsbnb.usci.receiver.model.json.BatchJson;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Baurzhan Makhanbetov
 */

public interface BatchQueueHolder {
    Optional<Batch> getNextBatch();

    void batchFinished(Long batchId);

    void addBatch(Batch batch);

    void reloadConfig();

    void removeBatch(long batchId);

    List<BatchJson> getOrderedBatches(Set<Long> respondentsWithPriority, String queueAlgo);
}
