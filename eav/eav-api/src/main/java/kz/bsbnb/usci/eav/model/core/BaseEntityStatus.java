package kz.bsbnb.usci.eav.model.core;

import kz.bsbnb.usci.eav.model.base.OperType;
import kz.bsbnb.usci.model.persistence.Persistable;

import java.time.LocalDateTime;

/**
 * @author Maksat Nussipzhan
 */

public class BaseEntityStatus extends Persistable {
    private Long batchId;
    private Long entityId;
    private Long approvedEntityId;
    private Long index;
    private Long metaClassId;
    private OperType operation;
    private String errorCode;
    private LocalDateTime systemDate;
    private EntityStatusType status;
    private String errorMessage;
    private String entityText;
    private String exceptionTrace;

    public BaseEntityStatus() {
        /*Пустой конструктор*/
    }

    public BaseEntityStatus(Long batchId, EntityStatusType status) {
        this.batchId = batchId;
        this.status = status;
        this.systemDate = LocalDateTime.now();
    }

    public Long getEntityId() {
        return entityId;
    }

    public BaseEntityStatus setEntityId(Long entityId) {
        this.entityId = entityId;
        return this;
    }

    public Long getApprovedEntityId() {
        return approvedEntityId;
    }

    public BaseEntityStatus setApprovedEntityId(Long approvedEntityId) {
        this.approvedEntityId = approvedEntityId;
        return this;
    }

    public Long getBatchId() {
        return batchId;
    }

    public BaseEntityStatus setBatchId(Long batchId) {
        this.batchId = batchId;
        return this;
    }

    public OperType getOperation() {
        return operation;
    }

    public BaseEntityStatus setOperation(OperType operation) {
        this.operation = operation;
        return this;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public BaseEntityStatus setErrorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public BaseEntityStatus setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public LocalDateTime getSystemDate() {
        return systemDate;
    }

    public BaseEntityStatus setSystemDate(LocalDateTime systemDate) {
        this.systemDate = systemDate;
        return this;
    }

    public Long getIndex() {
        return index;
    }

    public BaseEntityStatus setIndex(Long index) {
        this.index = index;
        return this;
    }

    public EntityStatusType getStatus() {
        return status;
    }

    public BaseEntityStatus setStatus(EntityStatusType status) {
        this.status = status;
        return this;
    }

    public String getEntityText() {
        return entityText;
    }

    public BaseEntityStatus setEntityText(String entityText) {
        this.entityText = entityText;
        return this;
    }

    public Long getMetaClassId() {
        return metaClassId;
    }

    public BaseEntityStatus setMetaClassId(Long metaClassId) {
        this.metaClassId = metaClassId;
        return this;
    }

    public String getExceptionTrace() {
        return exceptionTrace;
    }

    public BaseEntityStatus setExceptionTrace(String exceptionTrace) {
        this.exceptionTrace = exceptionTrace;
        return this;
    }

}
