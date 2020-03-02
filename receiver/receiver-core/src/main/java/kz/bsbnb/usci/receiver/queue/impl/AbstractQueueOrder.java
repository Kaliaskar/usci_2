package kz.bsbnb.usci.receiver.queue.impl;

import kz.bsbnb.usci.receiver.model.Batch;
import kz.bsbnb.usci.receiver.queue.BatchQueueOrder;

import java.util.List;

/**
 * @author Baurzhan Makhanbetov
 */

public abstract class AbstractQueueOrder implements BatchQueueOrder {

    @Override
    public Batch getNextBatch(List<Batch> batches) {
        Batch result = null;
        for (Batch file : batches) {
            if (result == null || compare(result, file) > 0)
                result = file;
        }

        return result;
    }

}
