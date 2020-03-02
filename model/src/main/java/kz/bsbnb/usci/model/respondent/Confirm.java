package kz.bsbnb.usci.model.respondent;

import kz.bsbnb.usci.model.persistence.Persistable;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author Artur Tkachenko
 */

public class Confirm extends Persistable {
    private static final long serialVersionUID = 8626348715892412142L;

    private Long respondentId;
    private ConfirmStatus status;
    private LocalDate reportDate;
    private Long productId;
    private LocalDateTime changeDate;
    private Long userId;
    private LocalDateTime firstBatchLoadTime;
    private LocalDateTime lastBatchLoadTime;
    private boolean isReconfirm = false;

    public Confirm() {
        super();
    }

    public Long getRespondentId() {
        return respondentId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Confirm setRespondentId(Long respondentId) {
        this.respondentId = respondentId;
        return this;
    }

    public ConfirmStatus getStatus() {
        return status;
    }

    public Confirm setStatus(ConfirmStatus status) {
        this.status = status;
        return this;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public Confirm setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
        return this;
    }

    public LocalDateTime getFirstBatchLoadTime() {
        return firstBatchLoadTime;
    }

    public Confirm setFirstBatchLoadTime(LocalDateTime firstBatchLoadTime) {
        this.firstBatchLoadTime = firstBatchLoadTime;
        return this;
    }

    public LocalDateTime getLastBatchLoadTime() {
        return lastBatchLoadTime;
    }

    public Confirm setLastBatchLoadTime(LocalDateTime lastBatchLoadTime) {
        this.lastBatchLoadTime = lastBatchLoadTime;
        return this;
    }

    public LocalDateTime getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(LocalDateTime changeDate) {
        this.changeDate = changeDate;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isReconfirm() {
        return isReconfirm;
    }

    public void setReconfirm(boolean reconfirm) {
        isReconfirm = reconfirm;
    }

}
