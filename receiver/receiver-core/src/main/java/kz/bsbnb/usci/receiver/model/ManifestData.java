package kz.bsbnb.usci.receiver.model;

import java.time.LocalDate;

/**
 * @author Jandos Iskakov
 */

public class ManifestData {
    private String product;
    private LocalDate reportDate;
    private String respondent;

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public String getRespondent() {
        return respondent;
    }

    public void setRespondent(String respondent) {
        this.respondent = respondent;
    }

}
