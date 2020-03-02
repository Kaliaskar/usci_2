package kz.bsbnb.usci.sync.service;

import java.util.Map;

/**
 * @author Kanat Tulbassiev
 */

public interface BatchService {

    void endBatch(long batchId);

    boolean incrementActualCounts(Map<Long, Long> batchesToUpdate);

}
