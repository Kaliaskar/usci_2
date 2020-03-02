package kz.bsbnb.usci.receiver.model.json;

import java.util.ArrayList;
import java.util.List;

public class BatchJsonList {
    private List<String> columnList = new ArrayList<>();
    private List<BatchJson> batchList = new ArrayList<>();

    public List<BatchJson> getBatchList() {
        return batchList;
    }

    public void setBatchList(List<BatchJson> batchList) {
        this.batchList = batchList;
    }

    public List<String> getColumnList() {
        return columnList;
    }

    public void setColumnList(List<String> columnList) {
        this.columnList = columnList;
    }
}
