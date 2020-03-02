package kz.bsbnb.usci.wsclient.model.currency;

import java.time.LocalDate;

public class NSIEntitySystem {
    private Long entityNum;
    private String operType;
    private LocalDate operDate;
    private Long entityID;
    private LocalDate beginDate;
    private LocalDate endDate;

    public NSIEntitySystem() {
    }

    public Long getEntityNum() {
        return entityNum;
    }

    public void setEntityNum(Long entityNum) {
        this.entityNum = entityNum;
    }

    public String getOperType() {
        return operType;
    }

    public void setOperType(String operType) {
        this.operType = operType;
    }

    public LocalDate getOperDate() {
        return operDate;
    }

    public void setOperDate(LocalDate operDate) {
        this.operDate = operDate;
    }

    public Long getEntityID() {
        return entityID;
    }

    public void setEntityID(Long entityID) {
        this.entityID = entityID;
    }

    public LocalDate getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(LocalDate beginDate) {
        this.beginDate = beginDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
