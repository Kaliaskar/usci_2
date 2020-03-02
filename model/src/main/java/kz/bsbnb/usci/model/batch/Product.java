package kz.bsbnb.usci.model.batch;

import kz.bsbnb.usci.model.persistence.Persistable;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Jandos Iskakov
 * */

public class Product extends Persistable {
    private String code;
    private String name;
    private byte[] xsd;
    private Set<Long> confirmPositionIds = new HashSet<>();
    private boolean confirmWithApproval = false;
    private boolean confirmWithSignature = false;
    private String crosscheckPackageName;

    public Product() {
        super();
    }

    public Product(Long id) {
        super(id);
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

    public byte[] getXsd() {
        return xsd;
    }

    public void setXsd(byte[] xsd) {
        this.xsd = xsd;
    }

    public Set<Long> getConfirmPositionIds() {
        return confirmPositionIds;
    }

    public void setConfirmPositionIds(Set<Long> confirmPositionIds) {
        this.confirmPositionIds = confirmPositionIds;
    }

    public boolean isConfirmWithApproval() {
        return confirmWithApproval;
    }

    public void setConfirmWithApproval(boolean confirmWithApproval) {
        this.confirmWithApproval = confirmWithApproval;
    }

    public boolean isConfirmWithSignature() {
        return confirmWithSignature;
    }

    public void setConfirmWithSignature(boolean confirmWithSignature) {
        this.confirmWithSignature = confirmWithSignature;
    }

    public String getCrosscheckPackageName() {
        return crosscheckPackageName;
    }

    public void setCrosscheckPackageName(String crosscheckPackageName) {
        this.crosscheckPackageName = crosscheckPackageName;
    }

    @Override
    public String toString() {
        return "Product{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", crosscheckPackageName='" + crosscheckPackageName + '\'' +
                ", confirmPositionIds=" + confirmPositionIds +
                ", confirmWithApproval=" + confirmWithApproval +
                ", confirmWithSignature=" + confirmWithSignature +
                ", id=" + id +
                '}';
    }
}
