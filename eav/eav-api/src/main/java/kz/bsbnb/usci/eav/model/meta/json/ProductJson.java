package kz.bsbnb.usci.eav.model.meta.json;

import kz.bsbnb.usci.model.batch.Product;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Olzhas Kaliaskar
 * @author Jandos Iskakov
 */

public class ProductJson {
    private Long id;
    private String code;
    private String name;
    private Boolean confirmWithApproval = false;
    private Boolean confirmWithSignature = false;
    private Set<Long> confirmPositionIds = new HashSet<>();
    private String crosscheckPackageName;

    public ProductJson() {
        super();
    }

    public ProductJson(Product product) {
        this.id = product.getId();
        this.code = product.getCode();
        this.name = product.getName();
        this.crosscheckPackageName = product.getCrosscheckPackageName();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getConfirmWithApproval() {
        return confirmWithApproval;
    }

    public void setConfirmWithApproval(boolean confirmWithApproval) {
        this.confirmWithApproval = confirmWithApproval;
    }

    public boolean getConfirmWithSignature() {
        return confirmWithSignature;
    }

    public void setConfirmWithSignature(boolean confirmWithSignature) {
        this.confirmWithSignature = confirmWithSignature;
    }

    public Set<Long> getConfirmPositionIds() {
        return confirmPositionIds;
    }

    public void setConfirmPositionIds(Set<Long> confirmPositionIds) {
        this.confirmPositionIds = confirmPositionIds;
    }

    public String getCrosscheckPackageName() {
        return crosscheckPackageName;
    }

    public void setCrosscheckPackageName(String crosscheckPackageName) {
        this.crosscheckPackageName = crosscheckPackageName;
    }
}
