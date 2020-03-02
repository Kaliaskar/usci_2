package kz.bsbnb.usci.model.persistence;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Artur Tkachenko
 */

public class Persistable implements Serializable {
    protected Long id;

    protected Persistable() {
        super();
    }

    protected Persistable(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Persistable setId(Long id) {
        this.id = id;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Persistable)) return false;

        Persistable that = (Persistable) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        if (id == null)
            return 0;

        return (int) (id ^ (id >>> 32));
    }

}