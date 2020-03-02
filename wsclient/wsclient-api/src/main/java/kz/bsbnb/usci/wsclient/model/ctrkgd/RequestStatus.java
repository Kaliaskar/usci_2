package kz.bsbnb.usci.wsclient.model.ctrkgd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum RequestStatus {
    SUCCESS(100),
    ERROR(101);

    private long id;

    RequestStatus(int id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String type() {
        return "REQUEST_STATUS";
    }

    public String code() {
        return name();
    }

    private static final Map<Long, RequestStatus> map = new ConcurrentHashMap<>();

    static {
        for (RequestStatus requestStatus : RequestStatus.values()) {
            map.put(requestStatus.getId(), requestStatus);
        }
    }

    public static RequestStatus getRequestStatus(long id) {
        return map.get(id);
    }
}
