package kz.bsbnb.usci.receiver.queue.impl;

import kz.bsbnb.usci.receiver.model.Batch;

/**
 * @author Baurzhan Makhanbetov
 */

public class ChronologicalOrder extends AbstractQueueOrder {

    @Override
    public int compare(Batch batch1, Batch batch2) {
        return (int) (batch1.getId() - batch2.getId());
    }

}
