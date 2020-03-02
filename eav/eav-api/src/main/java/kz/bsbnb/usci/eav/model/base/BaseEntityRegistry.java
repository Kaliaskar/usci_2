package kz.bsbnb.usci.eav.model.base;

import java.io.Serializable;

/**
 * Реестр сущностей
 * Все сущности когда либо созданные хранятся в таблице EAV_ENTITY
 * @author Jandos Iskakov
 */

public class BaseEntityRegistry implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    private Long respondentId;
    private Long metaClassId;
    private Long entityId;
    private Long parentEntityId;
    private Long parentClassId;

    public BaseEntityRegistry() {
        /*Пустой конструктор*/
    }

    public BaseEntityRegistry(BaseEntity baseEntity) {
        this.entityId = baseEntity.getId();
        this.respondentId = baseEntity.getRespondentId();
        this.metaClassId = baseEntity.getMetaClass().getId();
        this.parentEntityId = baseEntity.parentIsKey()? baseEntity.getParentEntity().getId(): 0L;
        this.parentClassId = baseEntity.parentIsKey()? baseEntity.getParentEntity().getMetaClass().getId(): 0L;
    }

    public Long getRespondentId() {
        return respondentId;
    }

    public BaseEntityRegistry setRespondentId(Long respondentId) {
        this.respondentId = respondentId;
        return this;
    }

    public Long getMetaClassId() {
        return metaClassId;
    }

    public BaseEntityRegistry setMetaClassId(Long metaClassId) {
        this.metaClassId = metaClassId;
        return this;
    }

    public Long getEntityId() {
        return entityId;
    }

    public BaseEntityRegistry setEntityId(Long entityId) {
        this.entityId = entityId;
        return this;
    }

    public Long getParentEntityId() {
        return parentEntityId;
    }

    public BaseEntityRegistry setParentEntityId(Long parentEntityId) {
        this.parentEntityId = parentEntityId;
        return this;
    }

    public Long getParentClassId() {
        return parentClassId;
    }

    public BaseEntityRegistry setParentClassId(Long parentClassId) {
        this.parentClassId = parentClassId;
        return this;
    }

    @Override
    public String toString() {
        return "BaseEntityRegistry{" +
                "respondentId=" + respondentId +
                ", metaClassId=" + metaClassId +
                ", entityId=" + entityId +
                ", parentEntityId=" + parentEntityId +
                ", parentClassId=" + parentClassId +
                '}';
    }

}
