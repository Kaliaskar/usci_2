package kz.bsbnb.usci.eav.model.base;

import java.time.LocalDate;

public class BaseEntityJson {

    private Long id;
    private Long respondentId;
    private LocalDate reportDate;
    private Long entityId;
    private boolean preApproved;
    private boolean preDeclined;
    private String entityText;
    private String entityKey;
    private Long metaClassId;
    private OperType operType;

    public BaseEntityJson () {

    }

    public BaseEntityJson(Long entityId,
                          boolean approved,
                          String  entityText,
                          String  entityKey) {
        this.entityId = entityId;
        this.preApproved = approved;
        this.entityText = entityText;
        this.entityKey = entityKey;

    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public boolean isPreApproved() {
        return preApproved;
    }

    public void setPreApproved(boolean preApproved) {
        this.preApproved = preApproved;
    }

    public String getEntityText() {
        return entityText;
    }

    public void setEntityText(String entityText) {
        this.entityText = entityText;
    }

    public String getEntityKey() {
        return entityKey;
    }

    public void setEntityKey(String entityKey) {
        this.entityKey = entityKey;
    }

    public Long getRespondentId() {
        return respondentId;
    }

    public void setRespondentId(Long respondentId) {
        this.respondentId = respondentId;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public boolean isPreDeclined() {
        return preDeclined;
    }

    public void setPreDeclined(boolean preDeclined) {
        this.preDeclined = preDeclined;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMetaClassId() {
        return metaClassId;
    }

    public void setMetaClassId(Long metaClassId) {
        this.metaClassId = metaClassId;
    }

    public OperType getOperType() {
        return operType;
    }

    public void setOperType(OperType operType) {
        this.operType = operType;
    }
}

