package kz.bsbnb.usci.sync.service.impl;

import kz.bsbnb.usci.receiver.client.BatchClient;
import kz.bsbnb.usci.sync.service.BatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Kanat Tulbassiev
 */

@Service
public class BatchServiceImpl implements BatchService {
    private static final Logger logger = LoggerFactory.getLogger(BatchServiceImpl.class);

    private final BatchClient batchClient;

    public BatchServiceImpl(BatchClient batchClient) {
        this.batchClient = batchClient;
    }

    @Override
    public void endBatch(long batchId) {
        try {
        batchClient.endBatch(batchId);
        } catch(Exception e) {
            logger.error("Ошибка вызова метода feign клиента", e);
        }
    }

    @Override
    public boolean incrementActualCounts(Map<Long, Long> batchesToUpdate) {
        try {
            return batchClient.incrementActualCounts(batchesToUpdate);
        } catch(Exception e) {
            logger.error("Ошибка вызова метода feign клиента", e);
        }
        
        return false;
    }

}
