package kz.bsbnb.usci.eav.model.base;

import kz.bsbnb.usci.eav.model.meta.MetaAttribute;
import kz.bsbnb.usci.model.exception.UsciException;

import java.io.Serializable;
import java.util.Objects;

public class BaseEntityInfo implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    private BaseEntity parent;
    private MetaAttribute attribute;

    public BaseEntityInfo() {
        /*Пустой конструктор*/
    }

    public BaseEntityInfo(BaseEntity parent, MetaAttribute attribute) {
        this.parent = parent;
        this.attribute = attribute;
    }

    public BaseEntity getParent() {
        return parent;
    }

    public void setParent(BaseEntity parent) {
        this.parent = parent;
    }

    public MetaAttribute getAttribute() {
        return attribute;
    }

    public void setAttribute(MetaAttribute attribute) {
        this.attribute = attribute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        BaseEntityInfo that = (BaseEntityInfo) o;

        if (parent == null || attribute == null || that.parent == null || that.attribute == null)
            throw new UsciException(String.format("Ошибка сравнения родителей сущностей (аргументы равны null) %s", o.toString()));

        if (!attribute.getId().equals(that.attribute.getId()))
            return false;

        if (parent.getId() != null && that.parent.getId() != null && Objects.equals(parent.getId(), that.parent.getId()))
            return true;

        if (parent.getId() == null && that.parent.getId() == null && parent.equalsByKey(that.parent))
            return true;

        return false;
    }

}
