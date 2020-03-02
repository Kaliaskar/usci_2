package kz.bsbnb.usci.eav.model.core;

import kz.bsbnb.usci.eav.model.base.BaseEntity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * @author Jandos Iskakov
 */

public class BaseEntityDate implements Serializable {
    private BaseEntity baseEntity;
    private LocalDate reportDate;
    private Set<String> received;
    private Long batchId;
    private LocalDateTime systemDate;

    public BaseEntityDate(BaseEntity baseEntity) {
        this.baseEntity = baseEntity;
        this.reportDate = baseEntity.getReportDate();
        this.received = baseEntity.getReceived();
        this.batchId = baseEntity.getBatchId();
        this.systemDate = LocalDateTime.now();
    }

    public BaseEntityDate(BaseEntity baseEntity, LocalDate reportDate, Set<String> received, Long batchId, LocalDateTime systemDate) {
        this.baseEntity = baseEntity;
        this.reportDate = reportDate;
        this.received = received;
        this.batchId = batchId;
        this.systemDate = systemDate;
    }

    public BaseEntity getBaseEntity() {
        return baseEntity;
    }

    public void setBaseEntity(BaseEntity baseEntity) {
        this.baseEntity = baseEntity;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public Set<String> getReceived() {
        return received;
    }

    public void setReceived(Set<String> received) {
        this.received = received;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public LocalDateTime getSystemDate() {
        return systemDate;
    }

    public void setSystemDate(LocalDateTime systemDate) {
        this.systemDate = systemDate;
    }
}
