package kz.bsbnb.usci.sync.job;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.sync.bean.SyncStatusSingleton;
import kz.bsbnb.usci.sync.service.BatchService;
import kz.bsbnb.usci.sync.service.EntityService;
import kz.bsbnb.usci.util.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Kanat Tulbassiev
 * @author Artur Tkachenko
 * @author Baurzhan Makhambetov
 */

public class DataJob extends AbstractDataJob {
    private static final Logger logger = LoggerFactory.getLogger(DataJob.class);

    @Autowired
    private BatchService batchService;
    @Autowired
    private SyncStatusSingleton syncStatusSingleton;
    @Autowired
    private ActualCountJob actualCountJob;
    @Autowired
    private EntityService entityService;

    /** Максимальное количество потоков для обработки сущностей */
    @Value("${sync.threadLimit}")
    private Short threadLimit;

    private final List<InProcessTester> entitiesInProcess = new ArrayList<>();

    private volatile BaseEntity currentEntity;
    private volatile boolean currentIntersection;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private double avgTimePrev = 0;
    private double avgTimeCur = 0;
    private long entityCounter = 0;
    private long executorServiceCnt = 0;
    private double avgTimeExecutor = 0;

    private int clearJobsIndex = 0;

    private class InProcessTester implements Callable<Boolean> {
        private final BaseEntity inProcessEntity;

        private InProcessTester(BaseEntity inProcessEntity) {
            this.inProcessEntity = inProcessEntity;
        }

        public Boolean call() {
            try {
                for (BaseEntity myEntityKeyElement : inProcessEntity.getKeyElements()) {
                    if (myEntityKeyElement.parentIsKey())
                        continue;
                    for (BaseEntity currentEntityKeyElement : currentEntity.getKeyElements()) {
                        if (Objects.equals(myEntityKeyElement.getMetaClass().getId(), currentEntityKeyElement.getMetaClass().getId()) &&
                                myEntityKeyElement.equalsByKey(currentEntityKeyElement) ) {
                                currentIntersection = true;
                                return true;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Ошибка выявления пересечения в sync {}", inProcessEntity, e.getMessage());
            }

            return false;
        }

        BaseEntity getInProcessEntity() {
            return inProcessEntity;
        }
    }

    @Override
    public void run() {
        logger.info("Начало работы...");

        //noinspection InfiniteLoopStatement
        while (true) {
            try {
                while (entities.size() > 0 && entitiesInProcess.size() < threadLimit) {
                    if (!processNewEntities())
                        break;
                }

                if (processingJobs.size() > 0) {
                    removeDeadJobs();
                }

                setFinishStatusToBatches();

                if (entities.size() == 0 && entitiesInProcess.size() == 0) {
                    Thread.sleep(SLEEP_TIME_NORMAL);
                    skipCounter++;
                }

                if (skipCounter > SKIP_TIME_MAX) {
                    Thread.sleep(SLEEP_TIME_LONG);
                    skipCounter = 0;
                }

                syncStatusSingleton.put(entities.size(), entitiesInProcess.size(), avgTimeCur);
                syncStatusSingleton.setExecutorStat(executorServiceCnt, avgTimeExecutor);
            } catch (Exception e) {
                logger.error("Программная ошибка работы", e);
            }
        }
    }

    private void removeDeadJobs() {
        Iterator<ProcessJob> processJobIterator = processingJobs.iterator();

        while (processJobIterator.hasNext()) {
            ProcessJob processJob = processJobIterator.next();

            if (!processJob.isAlive()) {
                BaseEntity entity = processJob.getBaseEntity();

                entityCounter++;
                if (entityCounter < STAT_INTERVAL) {
                    avgTimeCur = (avgTimeCur * (entityCounter - 1)) / entityCounter +
                            processJob.getTimeSpent() / entityCounter;
                } else {
                    if (avgTimePrev > 0)
                        logger.info("Скорость обработки: {}", avgTimeCur);

                    entityCounter = 0;
                    avgTimePrev = avgTimeCur;
                }

                Iterator<InProcessTester> entityProcessIterator = entitiesInProcess.iterator();

                boolean found = false;

                while (entityProcessIterator.hasNext()) {
                    if (entity.hashCode() == entityProcessIterator.next().getInProcessEntity().hashCode()) {
                        entityProcessIterator.remove();
                        found = true;
                        break;
                    }
                }

                if (!found)
                    throw new UsciException("Запись не найдена");

                if (processJob.getClearJobsIndex() < clearJobsIndex)
                    clearJobsIndex = 0;

                processJobIterator.remove();
            }
        }
    }

    private boolean processNewEntities() {
        final Optional<BaseEntity> optEntity = getClearEntity();

        if (optEntity.isPresent()) {
            BaseEntity entity = optEntity.get();

            final ProcessJob processJob = new ProcessJob(entityService, entity);
            processJob.setClearJobsIndex(clearJobsIndex);

            entitiesInProcess.add(new InProcessTester(entity));
            processingJobs.add(processJob);
            actualCountJob.insertBatchId(entity.getBatchId());

            processJob.start();
            skipCounter = 0;

            return  true;
        }

        return false;
    }

    private synchronized Optional<BaseEntity> getClearEntity() {
        if (clearJobsIndex >= entities.size())
            clearJobsIndex = 0;

        Iterator<BaseEntity> iterator = entities.listIterator(clearJobsIndex);
        while (iterator.hasNext()) {
            BaseEntity entity = iterator.next();

            entity.getKeyElements();

            if (!isInProcessWithThreads(entity)) {
                batchEntityFlag.add(entity.getBatchId());

                iterator.remove();
                return Optional.of(entity);
            }

            clearJobsIndex++;
        }

        return Optional.empty();
    }

    /**
     * Проверяет сущность пересекается ли она с другими сущностями в обработке
     * используются потоки чтобы производить проверку в параллельном и максимально эффективном режиме
     */
    private boolean isInProcessWithThreads(BaseEntity baseEntity) {
        currentEntity = baseEntity;
        currentIntersection = false;

        try {
            long startTime = System.currentTimeMillis();
            executorService.invokeAll(entitiesInProcess);
            long endTime = System.currentTimeMillis();
            executorServiceCnt++;
            avgTimeExecutor = ((executorServiceCnt - 1) * avgTimeExecutor + (endTime - startTime) ) / executorServiceCnt;
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }

        return currentIntersection;
    }

    /**
     * Проставляет статус завершения батчей
     * */
    private synchronized void setFinishStatusToBatches() {
        Set<Long> activeBatches = new HashSet<>();
        Set<Long> difference;

        synchronized (this) {
            for (BaseEntity entity : entities)
                activeBatches.add(entity.getBatchId());

            for (InProcessTester inProcessTester : entitiesInProcess)
                activeBatches.add(inProcessTester.getInProcessEntity().getBatchId());

            difference = SetUtils.difference(batches, activeBatches);
            difference.removeIf(batchId -> !batchEntityFlag.contains(batchId));

            if (difference.size() > 0)
                finishedBatches.addAll(difference);

            for (Long batchId : difference)
                batches.remove(batchId);
        }

        for (Long batchId : difference) {
            logger.info("Обработка батча id = {} завершена", batchId);

            batchEntityFlag.remove(batchId);

            batchService.endBatch(batchId);
        }
    }

}
