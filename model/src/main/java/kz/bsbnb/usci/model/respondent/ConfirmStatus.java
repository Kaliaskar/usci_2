package kz.bsbnb.usci.model.respondent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jandos Iskakov
 */

public enum ConfirmStatus {
    CONFIRMED(300),
    CONFIRMING(301),
    NOT_CONFIRMED(302);

    private long id;

    ConfirmStatus(int id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String type() {
        return "CONFIRM_STATUS";
    }

    public String code() {
        return name();
    }

    private static final Map<Long, ConfirmStatus> map = new ConcurrentHashMap<>();

    static {
        for (ConfirmStatus confirmStatus : ConfirmStatus.values()) {
            map.put(confirmStatus.getId(), confirmStatus);
        }
    }

    public static ConfirmStatus getApprovalStatus(long id) {
        return map.get(id);
    }

}
