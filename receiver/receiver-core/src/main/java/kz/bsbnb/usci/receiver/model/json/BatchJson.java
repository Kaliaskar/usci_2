package kz.bsbnb.usci.receiver.model.json;

import kz.bsbnb.usci.eav.model.meta.json.ProductJson;
import kz.bsbnb.usci.model.respondent.RespondentJson;
import kz.bsbnb.usci.model.util.TextJson;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author Jandos
 */

public class BatchJson {
    private Long id;
    private LocalDate reportDate;
    private LocalDateTime receiverDate;
    private LocalDateTime processBeginDate;
    private LocalDateTime processEndDate;
    private String fileName;
    private Long statusId;
    private TextJson status;
    private Long respondentId;
    private RespondentJson respondent;
    private Long productId;
    private ProductJson product;
    private Long totalEntityCount = 0L;
    private Long actualEntityCount = 0L;
    private Long successEntityCount = 0L;
    private Long errorEntityCount = 0L;
    private Long maintenanceEntityCount = 0L;
    private String hash;
    private String signature;
    private LocalDateTime signTime;
    private String signInfo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public LocalDateTime getReceiverDate() {
        return receiverDate;
    }

    public void setReceiverDate(LocalDateTime receiverDate) {
        this.receiverDate = receiverDate;
    }

    public LocalDateTime getProcessBeginDate() {
        return processBeginDate;
    }

    public void setProcessBeginDate(LocalDateTime processBeginDate) {
        this.processBeginDate = processBeginDate;
    }

    public LocalDateTime getProcessEndDate() {
        return processEndDate;
    }

    public void setProcessEndDate(LocalDateTime processEndDate) {
        this.processEndDate = processEndDate;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public RespondentJson getRespondent() {
        return respondent;
    }

    public void setRespondent(RespondentJson respondent) {
        this.respondent = respondent;
    }

    public Long getRespondentId() {
        return respondentId;
    }

    public void setRespondentId(Long respondentId) {
        this.respondentId = respondentId;
    }

    public void setTotalEntityCount(Long totalEntityCount) {
        this.totalEntityCount = totalEntityCount;
    }

    public Long getTotalEntityCount() {
        return totalEntityCount;
    }

    public Long getActualEntityCount() {
        return actualEntityCount;
    }

    public void setActualEntityCount(Long actualEntityCount) {
        this.actualEntityCount = actualEntityCount;
    }

    public Long getSuccessEntityCount() {
        return successEntityCount;
    }

    public void setSuccessEntityCount(Long successEntityCount) {
        this.successEntityCount = successEntityCount;
    }

    public TextJson getStatus() {
        return status;
    }

    public void setStatus(TextJson status) {
        this.status = status;
    }

    public Long getStatusId() {
        return statusId;
    }

    public void setStatusId(Long statusId) {
        this.statusId = statusId;
    }

    public Long getErrorEntityCount() {
        return errorEntityCount;
    }

    public void setErrorEntityCount(Long errorEntityCount) {
        this.errorEntityCount = errorEntityCount;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public ProductJson getProduct() {
        return product;
    }

    public void setProduct(ProductJson product) {
        this.product = product;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public LocalDateTime getSignTime() {
        return signTime;
    }

    public void setSignTime(LocalDateTime signTime) {
        this.signTime = signTime;
    }

    public String getSignInfo() {
        return signInfo;
    }

    public void setSignInfo(String signInfo) {
        this.signInfo = signInfo;
    }

    public Long getMaintenanceEntityCount() {
        return maintenanceEntityCount;
    }

    public void setMaintenanceEntityCount(Long maintenanceEntityCount) {
        this.maintenanceEntityCount = maintenanceEntityCount;
    }
}
