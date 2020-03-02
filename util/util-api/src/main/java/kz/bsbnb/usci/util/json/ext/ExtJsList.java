package kz.bsbnb.usci.util.json.ext;

import java.util.List;

public class ExtJsList {
    private int totalCount;
    private List<Object> data;
    private boolean success;

    public ExtJsList(List data) {
        this.data = data;
        this.totalCount = data.size();
        this.success = true;
    }

    public ExtJsList(List data, int totalCount) {
        this.data = data;
        this.totalCount = totalCount;
        this.success = true;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<Object> getData() {
        return data;
    }

    public void setData(List<Object> data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

}
