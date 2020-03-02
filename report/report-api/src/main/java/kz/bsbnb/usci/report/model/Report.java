package kz.bsbnb.usci.report.model;
import kz.bsbnb.usci.model.persistence.Persistable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Report extends Persistable {

    private static final long serialVersionUID = 1L;
    private String nameRu;
    private String nameKz;
    private String name;
    private String procedureName;
    private String type;
    private Long orderNumber;
    private Set<ReportInputParameter> inputParameters;
    private List<ExportType> exportTypesList;

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

    public String getName() {
        return name;
    }

    public void setName(String fileName) {
        this.name = fileName;
    }

    public String getProcedureName() {
        return procedureName;
    }

    public void setProcedureName(String procedureName) {
        this.procedureName = procedureName;
    }

    public List<ReportInputParameter> getInputParameters() {
        return new ArrayList<ReportInputParameter>(inputParameters);
    }

    public void setInputParameters(List<ReportInputParameter> inputParameters) {
        this.inputParameters = new HashSet<ReportInputParameter>(inputParameters);
    }

    public  List<String> getParameterNames() {
        List<java.lang.String> result = new ArrayList<java.lang.String>();
        for(ReportInputParameter parameter : inputParameters) {
            result.add(parameter.getParameterName());
        }
        return result;
    }

    public  List<String> getParameterCaptions() {
        List<java.lang.String> result = new ArrayList<java.lang.String>();
        for(ReportInputParameter parameter : inputParameters) {
            result.add(parameter.getNameRu());
        }
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Report) {
            Report otherReport = (Report) other;
            return id == otherReport.getId();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + (int) (this.id ^ (this.id >>> 32));
        return hash;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<ExportType> getExportTypesList() {
        return exportTypesList;
    }


    public void setExportTypeList(List<ExportType> exportTypeList) {
        this.exportTypesList = exportTypeList;
    }

    public Long getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Long orderNumber) {
        this.orderNumber = orderNumber;
    }

}