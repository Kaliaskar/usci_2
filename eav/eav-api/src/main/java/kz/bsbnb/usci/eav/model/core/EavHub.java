package kz.bsbnb.usci.eav.model.core;

import kz.bsbnb.usci.eav.model.base.BaseEntity;

import java.io.Serializable;

/**
 * @author Jandos Iskakov
 */

public class EavHub implements Serializable {
    private Long respondentId;
    private String entityKey;
    private String newEntityKey;
    private Long metaClassId;
    private Long entityId;
    private Long parentEntityId;
    private Long parentClassId;
    private Long batchId;

    public EavHub() {
        /*Пустой конструктор*/
    }

    public EavHub(BaseEntity baseEntity, String key) {
        this.entityId = baseEntity.getId();
        this.parentClassId = baseEntity.parentIsKey()? baseEntity.getParentEntity().getMetaClass().getId(): 0;
        this.parentEntityId = baseEntity.parentIsKey()? baseEntity.getParentEntity().getId(): 0;
        this.entityKey = key;
        this.metaClassId = baseEntity.getMetaClass().getId();
        this.respondentId = baseEntity.getRespondentId();
        this.batchId = baseEntity.getBatchId();
    }

    public EavHub(BaseEntity baseEntity, String oldEntityKey, String newEntityKey) {
        this(baseEntity, oldEntityKey);
        this.newEntityKey = newEntityKey;
    }

    //region Getters and Setters

    public Long getRespondentId() {
        return respondentId;
    }

    public void setRespondentId(Long respondentId) {
        this.respondentId = respondentId;
    }

    public Long getParentClassId() {
        return parentClassId;
    }

    public void setParentClassId(Long parentClassId) {
        this.parentClassId = parentClassId;
    }

    public String getEntityKey() {
        return entityKey;
    }

    public void setEntityKey(String entityKey) {
        this.entityKey = entityKey;
    }

    public String getNewEntityKey() {
        return newEntityKey;
    }

    public void setNewEntityKey(String newEntityKey) {
        this.newEntityKey = newEntityKey;
    }

    public Long getMetaClassId() {
        return metaClassId;
    }

    public void setMetaClassId(Long metaClassId) {
        this.metaClassId = metaClassId;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public Long getParentEntityId() {
        return parentEntityId;
    }

    public void setParentEntityId(Long parentEntityId) {
        this.parentEntityId = parentEntityId;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }


    //endregion

    @Override
    public String toString() {
        return "EavHub{" +
                "respondentId=" + respondentId +
                ", entityKey='" + entityKey + '\'' +
                ", newEntityKey='" + newEntityKey + '\'' +
                ", metaClassId=" + metaClassId +
                ", entityId=" + entityId +
                ", parentEntityId=" + parentEntityId +
                ", parentClassId=" + parentClassId +
                ", batchId=" + batchId +
                '}';
    }

}
