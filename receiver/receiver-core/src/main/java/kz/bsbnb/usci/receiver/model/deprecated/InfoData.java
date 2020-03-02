package kz.bsbnb.usci.receiver.model.deprecated;

import java.time.LocalDate;

/**
 * @author Dauletkhan Tulendiev
 */

@Deprecated
public class InfoData {
    private LocalDate reportDate;
    private LocalDate accountDate;
    private Long actualCreditCount;
    private String code;
    private String docType;
    private String docValue;
    private boolean maintenance;

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public Long getActualCreditCount() {
        return actualCreditCount;
    }

    public void setActualCreditCount(Long actualCreditCount) {
        this.actualCreditCount = actualCreditCount;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getDocValue() {
        return docValue;
    }

    public void setDocValue(String docValue) {
        this.docValue = docValue;
    }

    public LocalDate getAccountDate() {
        return accountDate;
    }

    public void setAccountDate(LocalDate accountDate) {
        this.accountDate = accountDate;
    }

    public void setMaintenance(boolean maintenance) {
        this.maintenance = maintenance;
    }

    public boolean isMaintenance() {
        return maintenance;
    }
}
