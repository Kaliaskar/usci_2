package kz.bsbnb.usci.receiver.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum BatchType {
    CREDIT_REGISTRY("1"), USCI_OLD("2"), USCI("3"), MAINTENANCE("4");

    private String code;

    BatchType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    private static final Map<String, BatchType> map = new ConcurrentHashMap<>();

    static {
        for (BatchType batchType : BatchType.values()) {
            map.put(batchType.getCode(), batchType);
        }
    }

    public static BatchType getBatchTypeByCode(String code) {
        return map.get(code);
    }

}
