package kz.bsbnb.usci.report.model;


import kz.bsbnb.usci.model.persistence.Persistable;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Aidar.Myrzahanov
 */

@XmlRootElement
public class ReportInputParameter extends Persistable {

    private static final long serialVersionUID = 1L;

    private String nameRu;
    private String nameKz;
    private String type;
    private String procedureName;
    private Long minimum;
    private Long maximum;
    private String parameterName;
    private Long orderNumber;
    private Long reportId;
    private String value;

    public ReportInputParameter() {

    }

    public String getNameRu() {
        return nameRu;
    }

    public void setNameRu(String nameRu) {
        this.nameRu = nameRu;
    }

    public String getNameKz() {
        return nameKz;
    }

    public void setNameKz(String nameKz) {
        this.nameKz = nameKz;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProcedureName() {
        return procedureName;
    }

    public void setProcedureName(String procedureName) {
        this.procedureName = procedureName;
    }

    public Long getMinimum() {
        return minimum;
    }

    public void setMinimum(Long minimum) {
        this.minimum = minimum;
    }

    public Long getMaximum() {
        return maximum;
    }

    public void setMaximum(Long maximum) {
        this.maximum = maximum;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    private ParameterType parameterType;

    public ParameterType getParameterType() {
        if (parameterType == null) {
            parameterType = ParameterType.fromString(type);
        }
        return parameterType;
    }

    public void setParameterType(ParameterType parameterType) {
        this.parameterType = parameterType;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public Long getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Long orderNumber) {
        this.orderNumber = orderNumber;
    }

}