package kz.bsbnb.usci.receiver.model;

import java.util.ArrayList;
import java.util.List;

public class BatchStatusJsonList {
    private List<String> columnList = new ArrayList<>();
    private List<BatchStatusJson> protocolList = new ArrayList<>();

    public List<String> getColumnList() {
        return columnList;
    }

    public void setColumnList(List<String> columnList) {
        this.columnList = columnList;
    }

    public List<BatchStatusJson> getProtocolList() {
        return protocolList;
    }

    public void setProtocolList(List<BatchStatusJson> protocolList) {
        this.protocolList = protocolList;
    }
}
