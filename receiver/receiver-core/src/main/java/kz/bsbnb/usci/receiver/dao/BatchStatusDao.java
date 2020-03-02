package kz.bsbnb.usci.receiver.dao;

import kz.bsbnb.usci.receiver.model.BatchStatus;

import java.util.List;

/**
 * @author Maksat Nussipzhan
 * @author Olzhas Kaliaskar
 */

public interface BatchStatusDao {

    void insert(BatchStatus batchStatus);

    List<BatchStatus> getList(long batchId);

    List<BatchStatus> getList(List<Long> batchIds);

}
