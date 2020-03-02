package kz.bsbnb.usci.receiver.queue;

import kz.bsbnb.usci.receiver.model.Batch;

import java.util.List;

public interface BatchQueueOrder {

    Batch getNextBatch(List<Batch> files);

    int compare(Batch batch1, Batch batch2);

}
