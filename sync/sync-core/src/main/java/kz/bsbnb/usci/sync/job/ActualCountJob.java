package kz.bsbnb.usci.sync.job;

import kz.bsbnb.usci.sync.service.BatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Потоко-безопасный асинхронный job для обновления actual count для батчей
 * @author Baurzhan Makhambetov
 */

@Component
class ActualCountJob {
    private static final Logger logger = LoggerFactory.getLogger(ActualCountJob.class);
    private static final int PERIOD = 3000;

    private final BatchService batchService;

    /** содержит в качестве ключа batchId, в значениях количество обработанных сущностей */
    private final Map<Long, Long> actualCountMap = new HashMap<>();

    public ActualCountJob(BatchService batchService) {
        this.batchService = batchService;
    }

    @Scheduled(fixedDelay = PERIOD)
    public void run() {
        logger.info("Начало работы по расписанию...");

        synchronized (this) {
            // отправляем изменения в БД и затем очищаем мэп
            if (actualCountMap.size() > 0) {
                logger.info("Отправка в receiver по батчам actualCount {} ", actualCountMap);

                if (batchService.incrementActualCounts(actualCountMap))
                    actualCountMap.clear();
            }
        }

        logger.info("Завершение работы");
    }

    synchronized void insertBatchId(Long batchId) {
        Long count = actualCountMap.get(batchId);
        count = count == null ? 0 : count;
        actualCountMap.put(batchId, count + 1);
    }

}
