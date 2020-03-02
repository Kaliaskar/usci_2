package kz.bsbnb.usci.brms.model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * @author Aibek Bukabayev
 */

public class PackageVersion implements Serializable {
    private static final long serialVersionUID = 1L;

    private LocalDate reportDate;
    private RulePackage rulePackage;

    public PackageVersion() {
        super();
    }

    public PackageVersion(RulePackage rulePackage, LocalDate date) {
        this.reportDate = date;
        this.rulePackage = rulePackage;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public RulePackage getRulePackage() {
        return rulePackage;
    }

    public void setRulePackage(RulePackage rulePackage) {
        this.rulePackage = rulePackage;
    }
}
