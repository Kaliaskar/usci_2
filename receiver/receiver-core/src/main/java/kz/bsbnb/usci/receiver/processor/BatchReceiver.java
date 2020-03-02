package kz.bsbnb.usci.receiver.processor;

import kz.bsbnb.usci.receiver.model.Batch;
import kz.bsbnb.usci.receiver.model.BatchFile;

public interface BatchReceiver {

    boolean processBatch(long batchId);

    void declineMaintenanceBatch(long batchId);

    void cancelBatch(long batchId);

    void receiveBatch(BatchFile batchFile);

    boolean processBatch(Batch batch);

}
