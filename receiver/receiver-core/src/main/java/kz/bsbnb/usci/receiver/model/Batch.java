package kz.bsbnb.usci.receiver.model;

import kz.bsbnb.usci.model.batch.Product;
import kz.bsbnb.usci.model.persistence.Persistable;
import kz.bsbnb.usci.model.respondent.Respondent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Alexandr Motov
 * @author Olzhas Kaliaskar
 * @author Jandos Iskakov
 * @author Ernur Bakash
 * @author Emil Essanov
 */

public class Batch extends Persistable {
    /**
     * тип батча ЕССП старых и нового форматов
     */
    private BatchType batchType;
    private String filePath;
    /**
     * Оригинальное название батча файла загруженное пользователем
     */
    private String batchName;
    /**
     * Время загрузки батча в систему; то есть когда пользователь загрузил батч
     */
    private LocalDateTime receiptDate;
    /**
     * Отчетная дата батча; информация берется из манифест файла
     */
    private LocalDate reportDate;
    /**
     * Пользователь(респондент) который загружил батч
     */
    private Long userId;
    private byte[] content;
    private String hash;
    private String signature;
    private String signInfo;
    private LocalDateTime signTime;
    private Long totalEntityCount;
    private Long actualEntityCount;
    private Long confirmId;
    /**
     * Идентификатор респондента; его следует фиксировать
     * так как пользователь может переходит от одного к другому респонденту
     * то есть уволиться и устроиться к другую организацию
     */
    private Long respondentId;
    /**
     * Идентификатор статуса батча
     */
    private Long statusId;
    private boolean maintenance = false;
    private Boolean maintenanceApproved = false;
    private Boolean maintenanceDeclined = false;
    private Respondent respondent;
    /**
     * Код продукта батча; извлекается из манифест файла
     */
    private String productCode;
    private Product product;
    /**
     * Идентификатор BatchEntryId
     */
    private Long batchEntryId;
    private BatchStatusType status;
    private Map<String, String> addParams = new HashMap<>();

    public Batch() {
        super();
    }

    public Batch(LocalDate reportDate, Long userId) {
        super();
        this.reportDate = reportDate;
        this.receiptDate = LocalDateTime.now();
        this.userId = userId;
    }

    public Batch(LocalDate reportDate) {
        super();
        this.reportDate = reportDate;
        this.receiptDate = LocalDateTime.now();
    }

    public Batch(LocalDateTime receiptDate, LocalDate reportDate) {
        this.reportDate = reportDate;
        this.receiptDate = receiptDate;
    }

    public void setReceiptDate(LocalDateTime receiptDate) {
        if (receiptDate == null) {
            this.receiptDate = null;
            return;
        }
        this.receiptDate = receiptDate;
    }

    public LocalDateTime getReceiptDate() {
        return receiptDate;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getBatchName() {
        return batchName;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public int getContentSize() {
        return content == null? 0: content.length;
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

    public String getSignInfo() {
        return signInfo;
    }

    public void setSignInfo(String signInfo) {
        this.signInfo = signInfo;
    }

    public LocalDateTime getSignTime() {
        return signTime;
    }

    public void setSignTime(LocalDateTime signTime) {
        this.signTime = signTime;
    }

    public BatchType getBatchType() {
        return batchType;
    }

    public void setBatchType(BatchType batchType) {
        this.batchType = batchType;
    }

    public Long getTotalEntityCount() {
        return totalEntityCount;
    }

    public void setTotalEntityCount(Long totalEntityCount) {
        this.totalEntityCount = totalEntityCount;
    }

    public Long getActualEntityCount() {
        return actualEntityCount;
    }

    public void setActualEntityCount(Long actualEntityCount) {
        this.actualEntityCount = actualEntityCount;
    }

    public Long getConfirmId() {
        return confirmId;
    }

    public void setConfirmId(Long confirmId) {
        this.confirmId = confirmId;
    }

    public Long getRespondentId() {
        return respondentId;
    }

    public void setRespondentId(Long respondentId) {
        this.respondentId = respondentId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Long getStatusId() {
        return statusId;
    }

    public void setStatusId(Long statusId) {
        this.statusId = statusId;
    }

    public Boolean getMaintenanceApproved() {
        return maintenanceApproved;
    }

    public Boolean getMaintenanceDeclined() {
        return maintenanceDeclined;
    }

    public void setMaintenance(boolean maintenance) {
        this.maintenance = maintenance;
    }

    public boolean isMaintenance() {
        return maintenance;
    }

    public void setMaintenanceApproved(Boolean maintenanceApproved) {
        this.maintenanceApproved = maintenanceApproved;
    }

    public Boolean isMaintenanceApproved() {
        return maintenanceApproved;
    }

    public void setMaintenanceDeclined(Boolean maintenanceDeclined) {
        this.maintenanceDeclined = maintenanceDeclined;
    }

    public Boolean isMaintenanceDeclined() {
        return maintenanceDeclined;
    }

    public Respondent getRespondent() {
        return respondent;
    }

    public void setRespondent(Respondent respondent) {
        this.respondent = respondent;
    }

    public Long getBatchEntryId() {
        return batchEntryId;
    }

    public void setBatchEntryId(Long batchEntryId) {
        this.batchEntryId = batchEntryId;
    }

    public BatchStatusType getStatus() {
        return status;
    }

    public void setStatus(BatchStatusType status) {
        this.status = status;
    }

    public void setAddParams(HashMap<String, String> addParams) {
        this.addParams = addParams;
    }

    public void addParam(String name, String value) {
        this.addParams.put(name, value);
    }

    public Map<String, String> getAddParams() {
        return addParams;
    }

    public String getFormattedFileName(){
        if (filePath == null)
            return "без имени";

        if (filePath.contains("\\"))
            return filePath.substring(filePath.lastIndexOf('\\') + 1);

        return filePath.substring(filePath.lastIndexOf('/') + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof Batch)) return false;

        Batch that = (Batch) o;

        return Objects.equals(id, that.id) &&
               Objects.equals(filePath, that.filePath) &&
               Objects.equals(receiptDate, that.receiptDate);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + receiptDate.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Batch{" +
                "fileName='" + filePath + '\'' +
                ", repDate=" + reportDate +
                ", respondentId=" + respondentId +
                ", id=" + id +
                '}';
    }

}