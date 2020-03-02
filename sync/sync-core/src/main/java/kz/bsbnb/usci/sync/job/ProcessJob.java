package kz.bsbnb.usci.sync.job;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.sync.service.EntityService;

/**
 * @author Kanat Tulbassiev
 */

public class ProcessJob extends Thread {
    private final BaseEntity baseEntity;
    private final EntityService entityService;
    private int clearJobsIndex;

    private long timeSpent = 0;

    ProcessJob(EntityService entityService, BaseEntity baseEntity) {
        this.entityService = entityService;
        this.baseEntity = baseEntity;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        entityService.process(baseEntity);
        timeSpent = System.currentTimeMillis() - startTime;
    }

    BaseEntity getBaseEntity() {
        return baseEntity;
    }

    long getTimeSpent() {
        return timeSpent;
    }

    int getClearJobsIndex() {
        return clearJobsIndex;
    }

    void setClearJobsIndex(int cleanJobsIndex) {
        this.clearJobsIndex = cleanJobsIndex;
    }

}
