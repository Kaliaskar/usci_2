package kz.bsbnb.usci.eav.model.base;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Artur Tkachenko
 * @author Alexandr Motov
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 */

public enum OperType {
    INSERT((short)1),
    UPDATE((short)2),
    DELETE((short)3),
    OPEN((short)4),
    CLOSE((short)5),
    NEW((short)6);

    private short id;

    private static final Map<Short, OperType> map = new ConcurrentHashMap<>();

    static {
        for (OperType operType : OperType.values()) {
            map.put(operType.getId(), operType);
        }
    }

    OperType(short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    public static OperType getOperType(Short value) {
        return map.get(value);
    }

}
