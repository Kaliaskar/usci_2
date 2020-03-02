package kz.bsbnb.usci.util.balancer;

import java.util.List;
import java.util.Optional;

public abstract class RoundRobin<T> {
    final int size;
    final List<T> routes;

    public RoundRobin(final List<T> list) {
        this.size = list.size();
        this.routes = list;
    }

    public abstract Optional<T> get();

}
