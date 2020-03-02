package kz.bsbnb.usci.receiver.dao;

import kz.bsbnb.usci.receiver.model.BatchEntry;

import java.util.List;

/**
 * @author Artur Tkachenko
 * @author Jandos Iskakov
 */

public interface BatchEntryDao {

    BatchEntry load(Long id);

    void remove(Long id);

    List<BatchEntry> getBatchEntriesByUserId(Long userId);

    Long save(BatchEntry batchEntry);

    void setProcessed(List<Long> batchEntryIds);

}
