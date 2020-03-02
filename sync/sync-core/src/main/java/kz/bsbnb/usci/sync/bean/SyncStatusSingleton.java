package kz.bsbnb.usci.sync.bean;

import kz.bsbnb.usci.sync.SyncStatus;
import org.springframework.stereotype.Component;

/**
 * @author Artur Tkachenko
 */

@Component
public class SyncStatusSingleton {
    private SyncStatus status = new SyncStatus();

    public void put(long queueSize, long threadsCount, double avgTime) {
        status.setQueueSize(queueSize);
        status.setThreadsCount(threadsCount);
        status.setAvgTime(avgTime);
    }

    public void setExecutorStat(long executorCnt, double avgExecutor){
        status.setExectuorStat(executorCnt, avgExecutor);
    }

    public SyncStatus getStatus() {
        return status;
    }

}
