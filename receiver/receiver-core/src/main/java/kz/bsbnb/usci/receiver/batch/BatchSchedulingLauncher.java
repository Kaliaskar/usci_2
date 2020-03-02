package kz.bsbnb.usci.receiver.batch;

import kz.bsbnb.usci.receiver.model.Batch;
import kz.bsbnb.usci.receiver.model.BatchType;
import kz.bsbnb.usci.receiver.queue.BatchQueueHolder;
import kz.bsbnb.usci.sync.client.SyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static kz.bsbnb.usci.model.Constants.MAX_SYNC_QUEUE_SIZE;

/**
 * Периодический добавляет батчи в очередь для обработки
 * @author Artur Tkachenko
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 * */

@Component
public class BatchSchedulingLauncher {
    private static final Logger logger = LoggerFactory.getLogger(BatchSchedulingLauncher.class);

    private static final int PERIOD = 1000;

    private final JobLauncher jobLauncher;
    private final SyncClient syncClient;
    private final BatchQueueHolder batchQueueHolder;
    private final Job batchJobEav;
    private final Job batchJobCr;
    private final Job batchJobMaintenance;

    private final Map<BatchType, Job> jobs = new HashMap<>();

    @PostConstruct
    private void init() {
        jobs.put(BatchType.CREDIT_REGISTRY, batchJobCr);
        jobs.put(BatchType.USCI_OLD, batchJobEav);
        jobs.put(BatchType.USCI, batchJobEav);
        jobs.put(BatchType.MAINTENANCE, batchJobMaintenance);
    }

    public BatchSchedulingLauncher(JobLauncher jobLauncher,
                                   SyncClient syncClient,
                                   BatchQueueHolder batchQueueHolder,
                                   @Qualifier("batchJobEav")
                                   Job batchJobEav,
                                   @Qualifier("batchJobCr")
                                   Job batchJobCr,
                                   @Qualifier("batchJobMaintenance")
                                   Job batchJobMaintenance) {
        this.jobLauncher = jobLauncher;
        this.syncClient = syncClient;
        this.batchQueueHolder = batchQueueHolder;
        this.batchJobEav = batchJobEav;
        this.batchJobCr = batchJobCr;
        this.batchJobMaintenance = batchJobMaintenance;
    }

    @Scheduled(fixedDelay = PERIOD)
    public void run() {
        if (syncClient.getQueueSize() > MAX_SYNC_QUEUE_SIZE) {
            logger.info("Очеред SYNC заполнена, приостанавливаем добавление батчей в очередь");
            return;
        }

        Set<Long> finishedBatchIds = syncClient.getFinishedBatches();
        if (!finishedBatchIds.isEmpty())
            logger.info("Получены батчи из SYNC как завершенные", finishedBatchIds);

        for (Long batchId : finishedBatchIds) {
            logger.info("Помечание батча из SYNC как завершенным id = {}", batchId);
            batchQueueHolder.batchFinished(batchId);
        }

        Optional<Batch> optNextBatch = batchQueueHolder.getNextBatch();

        if (optNextBatch.isPresent()) {
            Batch nextBatch = optNextBatch.get();

            logger.info("Отправка батча на обработку id = {}", nextBatch.getId());

            JobParametersBuilder jobParams = new JobParametersBuilder()
                .addParameter("respondentId", new JobParameter(nextBatch.getRespondentId()))
                .addParameter("batchId", new JobParameter(nextBatch.getId()))
                .addParameter("userId", new JobParameter(nextBatch.getUserId()))
                .addParameter("reportId", new JobParameter(nextBatch.getConfirmId()))
                .addParameter("actualCount", new JobParameter(nextBatch.getActualEntityCount()))
                .addParameter("totalCount", new JobParameter(0L))
                .addParameter("maintenance", new JobParameter((long)(nextBatch.isMaintenance()? 1: 0)));

            try {
                // по типу батча определяем какой job запускать: КР, EAV

                Job job = jobs.get(nextBatch.getBatchType());

                jobLauncher.run(job, jobParams.toJobParameters());
            } catch (Exception e) {
                logger.error(String.format("Ошибка запуска джоба по батчу %s", nextBatch.getId()), e);
                batchQueueHolder.batchFinished(nextBatch.getId());
            }
        } else
            logger.debug("Нет файлов для отправки");
    }

}
