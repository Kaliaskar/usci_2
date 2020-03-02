package kz.bsbnb.usci.receiver.model;

import kz.bsbnb.usci.model.util.TextJson;

/**
 * @author Jandos Iskakov
 */

public class BatchStatusJson {
    private Long batchId;
    private Long entityId;
    private Long metaClassId;
    private Long statusId;
    private TextJson status;
    private String textCode;
    private TextJson operation;
    private String textRu;
    private String textKz;
    private String comments;
    private String entityText;

    public BatchStatusJson() {
        super();
    }

    public BatchStatusJson(Long batchId) {
        this.batchId = batchId;
    }

    public Long getBatchId() {
        return batchId;
    }

    public BatchStatusJson setBatchId(Long batchId) {
        this.batchId = batchId;
        return this;
    }

    public Long getEntityId() {
        return entityId;
    }

    public BatchStatusJson setEntityId(Long entityId) {
        this.entityId = entityId;
        return this;
    }

    public Long getMetaClassId() {
        return metaClassId;
    }

    public BatchStatusJson setMetaClassId(Long metaClassId) {
        this.metaClassId = metaClassId;
        return this;
    }

    public String getTextCode() {
        return textCode;
    }

    public BatchStatusJson setTextCode(String textCode) {
        this.textCode = textCode;
        return this;
    }

    public String getTextRu() {
        return textRu;
    }

    public BatchStatusJson setTextRu(String textRu) {
        this.textRu = textRu;
        return this;
    }

    public TextJson getOperation() {
        return operation;
    }

    public void setOperation(TextJson operation) {
        this.operation = operation;
    }

    public String getTextKz() {
        return textKz;
    }

    public BatchStatusJson setTextKz(String textKz) {
        this.textKz = textKz;
        return this;
    }

    public String getComments() {
        return comments;
    }

    public BatchStatusJson setComments(String comments) {
        this.comments = comments;
        return this;
    }

    public TextJson getStatus() {
        return status;
    }

    public BatchStatusJson setStatus(TextJson status) {
        this.status = status;
        return this;
    }

    public String getEntityText() {
        return entityText;
    }

    public BatchStatusJson setEntityText(String entityText) {
        this.entityText = entityText;
        return this;
    }

    public Long getStatusId() {
        return statusId;
    }

    public BatchStatusJson setStatusId(Long statusId) {
        this.statusId = statusId;
        return this;
    }

}
