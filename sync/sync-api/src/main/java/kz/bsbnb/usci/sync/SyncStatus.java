package kz.bsbnb.usci.sync;

import java.io.Serializable;

/**
 * @author Artur Tkachenko
 */

public class SyncStatus implements Serializable {
    private long queueSize;
    private long threadsCount;
    private double avgTime;
    private long executorCnt;
    private double avgExecutor;

    public long getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(long queueSize) {
        this.queueSize = queueSize;
    }

    public long getThreadsCount() {
        return threadsCount;
    }

    public void setThreadsCount(long threadsCount) {
        this.threadsCount = threadsCount;
    }

    public double getAvgTime() {
        return avgTime;
    }

    public void setAvgTime(double avgTime) {
        this.avgTime = avgTime;
    }

    public void setExectuorStat(long executorCnt, double avgTime) {
        this.executorCnt = executorCnt;
        this.avgExecutor = avgTime;
    }

    @Override
    public String toString() {
        return "SyncStatus{" +
                "queueSize=" + queueSize +
                ", threadsCount=" + threadsCount +
                ", avgTime=" + avgTime +
                ", executorCnt=" + executorCnt +
                ", avgExecutor=" + avgExecutor +
                '}';
    }

}
