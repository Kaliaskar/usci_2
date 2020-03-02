package kz.bsbnb.usci.util.balancer;

import java.util.List;
import java.util.Optional;

public class CircleRoundRobin<T> extends RoundRobin<T> {
    private int cursor;

    public CircleRoundRobin(final List<T> list) {
        super(list);
        cursor = 0;
    }

    @Override
    public synchronized Optional<T> get() {
        if (routes.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(routes.get(getCursor()));
    }

    private synchronized int getCursor() {
        try {
            return cursor++;
        } finally {
            if (cursor >= (size)) {
                cursor = 0;
            }
        }
    }


}
