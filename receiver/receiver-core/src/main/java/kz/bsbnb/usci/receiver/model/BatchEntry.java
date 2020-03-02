package kz.bsbnb.usci.receiver.model;

import kz.bsbnb.usci.model.persistence.Persistable;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author Artur Tkachenko
 */

public class BatchEntry extends Persistable {
    /**
     * XML файл
     */
    private String value;
    /**
     * Отчетная дата; заполняется пользователем на фронте
     */
    private LocalDate repDate;
    /**
     * Дата создания сущности
     */
    private LocalDateTime updateDate;
    /**
     * Идентификатор пользователя который создал запись
     */
    private Long userId;
    /**
     * Идентфикатор сущности; инициализируется на фронте скриптом
     */
    private Long entityId;
    /**
     * Идентфикатор мета класса; инициализируется на фронте скриптом
     */
    private Long metaClassId;

    /**
     * Признак батча когда меняется ключевое поле
     */
    private boolean isMaintenance = false;
    /**
     * Признак обработки батча
     */
    private Boolean isProcessed = false;
    /**
     * Признак удаления батча
     */
    private boolean isDeleted = false;

    public BatchEntry() {
        super();
    }

    public BatchEntry(Long id) {
        super(id);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public LocalDate getRepDate() {
        return repDate;
    }

    public void setRepDate(LocalDate repDate) {
        this.repDate = repDate;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public Boolean getProcessed() {
        return isProcessed;
    }

    public void setProcessed(Boolean processed) {
        isProcessed = processed;
    }

    public boolean isMaintenance() {
        return isMaintenance;
    }

    public void setMaintenance(boolean maintenance) {
        this.isMaintenance = maintenance;
    }

    public Long getMetaClassId() {
        return metaClassId;
    }

    public void setMetaClassId(Long metaClassId) {
        this.metaClassId = metaClassId;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }
}
