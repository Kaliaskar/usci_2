package kz.bsbnb.usci.eav.model.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Maksat Nussipzhan
 */

public enum EntityStatusType {
    ERROR(8),
    COMPLETED(15),
    MAINTENANCE(9);

    int id;

    EntityStatusType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    private static final Map<Integer, EntityStatusType> map = new ConcurrentHashMap<>();

    static {
        for (EntityStatusType entityStatusType : EntityStatusType.values()) {
            map.put(entityStatusType.getId(), entityStatusType);
        }
    }

    public static EntityStatusType getEntityStatus(int id) {
        return map.get(id);
    }

}
