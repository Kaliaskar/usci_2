package kz.bsbnb.usci.receiver.queue.impl;

import kz.bsbnb.usci.receiver.model.Batch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Baurzhan Makhanbetov
 */

public class RespondentCycleOrder extends AbstractQueueOrder {
    private Map<Long, Long> respondentsTime = new HashMap<>();
    private Long time = 0L;

    @Override
    public int compare(Batch batch1, Batch batch2) {
        return (int) (batch1.getId() - batch2.getId());
    }

    @Override
    public Batch getNextBatch(List<Batch> batches) {
        Batch nextBatch = null;
        Long minTime = null;

        for (Batch file : batches) {
            if (nextBatch == null) {
                nextBatch = file;
                minTime = respondentsTime.getOrDefault(file.getRespondentId(), -1L);
            }
            else {
                Long respondentLastTime  = respondentsTime.getOrDefault(file.getRespondentId(), -1L);

                if (minTime > respondentLastTime) {
                    minTime = respondentLastTime;
                    nextBatch = file;
                }
            }
        }

        if (nextBatch != null)
            respondentsTime.put(nextBatch.getRespondentId(), time++);

        return nextBatch;
    }

}
