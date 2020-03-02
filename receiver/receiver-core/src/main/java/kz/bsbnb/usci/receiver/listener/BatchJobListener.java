package kz.bsbnb.usci.receiver.listener;

import kz.bsbnb.usci.receiver.batch.service.BatchService;
import kz.bsbnb.usci.receiver.model.Batch;
import kz.bsbnb.usci.receiver.model.BatchStatus;
import kz.bsbnb.usci.receiver.model.BatchStatusType;
import kz.bsbnb.usci.receiver.model.ReceiverStatusSingleton;
import kz.bsbnb.usci.receiver.queue.BatchQueueHolder;
import kz.bsbnb.usci.receiver.service.MailService;
import kz.bsbnb.usci.sync.client.SyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author Kanat Tulbassiev
 */

@Component
public class BatchJobListener implements JobExecutionListener {
    private static final Logger logger = LoggerFactory.getLogger(BatchJobListener.class);

    private final ReceiverStatusSingleton receiverStatus;
    private final BatchService batchService;
    private final BatchQueueHolder batchQueueHolder;
    private final SyncClient syncClient;
    private final MailService mailService;

    private static long lastTime;

    @Autowired
    public BatchJobListener(ReceiverStatusSingleton receiverStatus,
                            BatchQueueHolder batchQueueHolder,
                            BatchService batchService,
                            SyncClient syncClient,
                            MailService mailService) {
        this.receiverStatus = receiverStatus;
        this.batchQueueHolder = batchQueueHolder;
        this.batchService = batchService;
        this.syncClient = syncClient;
        this.mailService = mailService;
    }

    @BeforeJob
    public void beforeJob(JobExecution jobExecution) {
        // изначально при старте spring boot избегаем авто запуск
        if (jobExecution.getJobParameters().isEmpty())
            return;

        long batchId = jobExecution.getJobParameters().getLong("batchId");

        logger.info("Началась обработка батча: id = {} время начала {}", batchId, LocalDateTime.now());
        lastTime = System.currentTimeMillis();

        receiverStatus.batchStarted();

        batchService.clearActualCount(batchId);
        batchService.addBatchStatus(new BatchStatus(batchId, BatchStatusType.PROCESSING));
    }

    @AfterJob
    public void afterJob(JobExecution jobExecution) {
        Long batchId = jobExecution.getJobParameters().getLong("batchId");

        // код jobExecution.getExitStatus().equals(ExitStatus.FAILED) заменен так как выдает false
        if (jobExecution.getExitStatus().getExitCode().equals(ExitStatus.FAILED.getExitCode()))
            batchQueueHolder.batchFinished(batchId);
        else {
            // отправляем сигнал SYNC что парсинг батча завершен в RECEIVER
            logger.info("Отправление сигнала в SYNC что парсинг батча({}) завершен в RECEIVER", batchId);
            syncClient.batchFinishedInReader(batchId);
        }

        Batch batch = batchService.getBatch(batchId);

        mailService.notifyBatchProcessCompleted(batch);

        receiverStatus.batchEnded();

        double secs = Math.round((System.currentTimeMillis() - lastTime) / 1000);
        double minutes = Math.round(secs / 60);

        logger.info("Чтение батча завершено: id = {} за время ({} мин, {} сек), текущее время: ({});", batch.getId(), minutes, secs, LocalDateTime.now());
    }

}
