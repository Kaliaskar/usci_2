package kz.bsbnb.usci.receiver.batch;

import kz.bsbnb.usci.model.Constants;
import kz.bsbnb.usci.receiver.batch.service.BatchService;
import kz.bsbnb.usci.receiver.model.Batch;
import kz.bsbnb.usci.receiver.model.BatchStatusType;
import kz.bsbnb.usci.receiver.model.BatchType;
import kz.bsbnb.usci.receiver.model.ReceiverStatusSingleton;
import kz.bsbnb.usci.receiver.queue.BatchQueueHolder;
import kz.bsbnb.usci.util.client.ConfigClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Загружает не завершенные батчи из БД и добавляет в очередь на обработку
 * @author Artur Tkachenko
 * @author Kanat Tulbassiev
 */

@Component
public class PendingBatchLauncher implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(PendingBatchLauncher.class);

    private final BatchService batchService;
    private final BatchQueueHolder batchQueueHolder;
    private final ReceiverStatusSingleton receiverStatus;
    private final ConfigClient configClient;

    public PendingBatchLauncher(BatchService batchService,
                                BatchQueueHolder batchQueueHolder,
                                ReceiverStatusSingleton receiverStatus,
                                ConfigClient configClient) {
        this.batchService = batchService;
        this.batchQueueHolder = batchQueueHolder;
        this.receiverStatus = receiverStatus;
        this.configClient = configClient;
    }

    @Override
    public void run(String... args) {
        logger.info("Проверка не завершенных батчей из БД...");
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }

        List<Batch> pendingBatches = batchService.getPendingBatchList();

        Set<Long> orgIds = configClient.getDigitalSigningOrgIds();

        // удаляем батчи ожидающие подписи
        pendingBatches.removeIf(batch -> batch.getStatusId() == BatchStatusType.WAITING_FOR_SIGNATURE.getId() &&
                batch.getSignature() == null &&
                batch.getRespondentId() != null &&
                !batch.getRespondentId().equals(Constants.NBK_AS_RESPONDENT_ID) &&
                orgIds.contains(batch.getRespondentId()));

        if (pendingBatches.size() > 0) {
            logger.info("Найдены не законченные батчи: {}", pendingBatches.size());

            logger.info("-------------------------------------------------------------------------");

            for (Batch batch : pendingBatches)
                logger.info(batch.getId() + ", " + batch.getFilePath() + ", " + batch.getReportDate().format(Constants.DATE_FORMATTER_ISO));

            logger.info("-------------------------------------------------------------------------");

            for (Batch batch : pendingBatches) {
                logger.info("Перезагрузка батча : id = {}, fileName = {}", batch.getId(), batch.getFilePath());

                try {
                    batchQueueHolder.addBatch(batch);
                    receiverStatus.batchReceived();
                } catch (Exception e) {
                    logger.error(String.format("Ошибка перезагрузки батча id = %s", batch.getId()), e);
                }
            }
        }
    }

}
