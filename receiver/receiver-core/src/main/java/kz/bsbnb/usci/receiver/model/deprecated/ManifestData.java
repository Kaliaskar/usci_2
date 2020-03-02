package kz.bsbnb.usci.receiver.model.deprecated;

import java.time.LocalDate;
import java.util.HashMap;

/**
 * @author Dauletkhan Tulendiev
 */

@Deprecated
public class ManifestData {
    private String type;
    private String product;
    private Long userId;
    private Integer size;
    private LocalDate reportDate;
    private HashMap<String, String> additionalParams = new HashMap<>();
    private boolean maintenance;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public HashMap<String, String> getAdditionalParams() {
        return additionalParams;
    }

    public void setAdditionalParams(HashMap<String, String> additionalParams) {
        this.additionalParams = additionalParams;
    }

    public void setMaintenance(boolean maintenance) {
        this.maintenance = maintenance;
    }

    public boolean isMaintenance() {
        return maintenance;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }
}
