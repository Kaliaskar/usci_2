package kz.bsbnb.usci.sync.job;


import kz.bsbnb.usci.eav.model.base.BaseEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Kanat Tulbassiev
 */

public abstract class AbstractDataJob extends Thread {
    /** Сущности ожидающие обработки */
    final List<BaseEntity> entities = new ArrayList<>();
    /** Джобы в обработке*/
    final List<ProcessJob> processingJobs = new ArrayList<>();
    /** Кол-во сущностей по каждому батчу */
    final Set<Long> batchEntityFlag = new HashSet<>();

    /** Кол-во до гибернаций */
    final int SKIP_TIME_MAX = 10;
    /** Счетчик для подчитывания количества простоя */
    volatile int skipCounter = 0;
    /** Время гибернаций обычной */
    final int SLEEP_TIME_NORMAL = 10;
    /** Время долгой гибернаций */
    final int SLEEP_TIME_LONG = 50;
    /** Количество сущностей для отображения статистики */
    final int STAT_INTERVAL = 1000;

    Set<Long> batches = new HashSet<>();

    Set<Long> finishedBatches = new HashSet<>();

    public final synchronized void addAll(List<BaseEntity> entities) {
        this.entities.addAll(entities);
    }

    public int getQueueSize() {
        return entities.size();
    }

    public Set<Long> getFinishedBatches(){
        synchronized (this) {
            Set<Long> ret = new HashSet<>(finishedBatches);
            finishedBatches.clear();
            return ret;
        }
    }

    public final void batchFinishedInReader(Long batchId) {
        batches.add(batchId);
    }

}
