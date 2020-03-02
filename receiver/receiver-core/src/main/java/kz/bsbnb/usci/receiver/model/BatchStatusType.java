package kz.bsbnb.usci.receiver.model;

import kz.bsbnb.usci.model.util.ConstantType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Maksat Nussipzhan
 */

public enum BatchStatusType implements ConstantType {
    WAITING((short)18),
    PROCESSING((short)19),
    ERROR((short)20),
    COMPLETED((short)21),
    WAITING_FOR_SIGNATURE((short)22),
    CANCELLED((short)23),
    MAINTENANCE_REQUEST((short)24),
    MAINTENANCE_DECLINED((short)55);

    short id;

    BatchStatusType(short id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public String type() {
        return "BATCH_STATUS";
    }

    @Override
    public String code() {
        return name();
    }

    private static final Map<Integer, BatchStatusType> map = new ConcurrentHashMap<>();

    static {
        for (BatchStatusType batchStatusType : BatchStatusType.values()) {
            map.put(batchStatusType.getId(), batchStatusType);
        }
    }

    public static BatchStatusType getBatchStatus(int id) {
        return map.get(id);
    }

}
