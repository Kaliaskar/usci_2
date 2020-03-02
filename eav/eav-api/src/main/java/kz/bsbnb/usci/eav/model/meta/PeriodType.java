package kz.bsbnb.usci.eav.model.meta;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum PeriodType {
    CONSTANT(200L),
    DAY(201L),
    WEEK(202L),
    MONTH(203L),
    QUARTER(206L),
    YEAR(204L);

    private long id;

    private static final Map<Long, PeriodType> map = new ConcurrentHashMap<>();

    static {
        for (PeriodType periodType : PeriodType.values()) {
            map.put(periodType.getId(), periodType);
        }
    }

    PeriodType(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public static PeriodType getPeriodType(long value) {
        return map.get(value);
    }

}
