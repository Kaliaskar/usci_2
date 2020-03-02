package kz.bsbnb.usci.receiver.queue.impl;

import kz.bsbnb.usci.receiver.model.Batch;

/**
 * @author Baurzhan Makhanbetov
 */

public class MinimumWeightOrder extends AbstractQueueOrder {

    @Override
    public int compare(Batch batch1, Batch batch2) {
        return batch1.getContentSize() - batch2.getContentSize();
    }

}
