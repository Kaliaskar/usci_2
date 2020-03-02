package kz.bsbnb.usci.model.respondent;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author Zhanar Akhmetova
 */

public class ConfirmJson {
    private Long id;
    private Long respondentId;
    private String respondentName;
    private Long statusId;
    private String statusName;
    private LocalDate reportDate;
    private Long userId;
    private String userName;
    private LocalDateTime firstBatchLoadTime;
    private LocalDateTime lastBatchLoadTime;
    private Long crossCheckStatusId;
    private String crossCheckStatusName;
    private Long productId;
    private String productName;

    public ConfirmJson() {
    }

    public Long getId() {
        return id;
    }

    public ConfirmJson setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getRespondentId() {
        return respondentId;
    }

    public ConfirmJson setRespondentId(Long respondentId) {
        this.respondentId = respondentId;
        return this;
    }

    public String getRespondentName() {
        return respondentName;
    }

    public ConfirmJson setRespondentName(String respondentName) {
        this.respondentName = respondentName;
        return this;
    }

    public Long getStatusId() {
        return statusId;
    }

    public ConfirmJson setStatusId(Long statusId) {
        this.statusId = statusId;
        return this;
    }

    public String getStatusName() {
        return statusName;
    }

    public ConfirmJson setStatusName(String statusName) {
        this.statusName = statusName;
        return this;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public ConfirmJson setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
        return this;
    }

    public Long getUserId() {
        return userId;
    }

    public ConfirmJson setUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public ConfirmJson setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public LocalDateTime getFirstBatchLoadTime() {
        return firstBatchLoadTime;
    }

    public ConfirmJson setFirstBatchLoadTime(LocalDateTime firstBatchLoadTime) {
        this.firstBatchLoadTime = firstBatchLoadTime;
        return this;
    }

    public LocalDateTime getLastBatchLoadTime() {
        return lastBatchLoadTime;
    }

    public ConfirmJson setLastBatchLoadTime(LocalDateTime lastBatchLoadTime) {
        this.lastBatchLoadTime = lastBatchLoadTime;
        return this;
    }

    public Long getCrossCheckStatusId() {
        return crossCheckStatusId;
    }

    public ConfirmJson setCrossCheckStatusId(Long crossCheckStatusId) {
        this.crossCheckStatusId = crossCheckStatusId;
        return this;
    }

    public String getCrossCheckStatusName() {
        return crossCheckStatusName;
    }

    public ConfirmJson setCrossCheckStatusName(String crossCheckStatusName) {
        this.crossCheckStatusName = crossCheckStatusName;
        return this;
    }

    public Long getProductId() {
        return productId;
    }

    public ConfirmJson setProductId(Long productId) {
        this.productId = productId;
        return this;
    }

    public String getProductName() {
        return productName;
    }

    public ConfirmJson setProductName(String productName) {
        this.productName = productName;
        return this;
    }
}
