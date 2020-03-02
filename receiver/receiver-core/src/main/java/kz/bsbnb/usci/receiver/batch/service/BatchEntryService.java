package kz.bsbnb.usci.receiver.batch.service;

import kz.bsbnb.usci.receiver.model.BatchEntry;

import java.util.List;

public interface BatchEntryService {

    Long save(BatchEntry batchEntry);

    BatchEntry load(Long batchEntryId);

    List<BatchEntry> getBatchEntriesByUserId(Long userId);

    void delete(Long batchEntryId);

    void confirmBatchEntries(Long userId, boolean isNb);

}
