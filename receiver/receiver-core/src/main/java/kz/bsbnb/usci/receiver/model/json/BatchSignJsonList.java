package kz.bsbnb.usci.receiver.model.json;

import java.util.ArrayList;
import java.util.List;

public class BatchSignJsonList {
    private List<BatchSignJson> batchList = new ArrayList<>();

    public List<BatchSignJson> getBatchList() {
        return batchList;
    }

    public void setBatchList(List<BatchSignJson> batchList) {
        this.batchList = batchList;
    }
}
