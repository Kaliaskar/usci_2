package kz.bsbnb.usci.mail.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Baurzhan Makhambetov
 */

public enum MailMessageStatus {
    PROCESSING(132),
    SENT(133),
    REJECTED_BY_USER_SETTINGS(134);

    long id;

    MailMessageStatus(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    private static final Map<Long, MailMessageStatus> map = new ConcurrentHashMap<>();

    static {
        for (MailMessageStatus mailMessageStatus : MailMessageStatus.values()) {
            map.put(mailMessageStatus.getId(), mailMessageStatus);
        }
    }

    public static MailMessageStatus getMailMessageStatus(int id) {
        return map.get(id);
    }

}
