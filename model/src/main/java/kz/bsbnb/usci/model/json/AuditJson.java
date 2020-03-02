package kz.bsbnb.usci.model.json;

import kz.bsbnb.usci.model.adm.User;

import java.time.LocalDateTime;

public class AuditJson {
    private static final long serialVersionUID = 1L;
    private long record_id;
    private String schema_name;
    private long userId;
    private LocalDateTime auditTime;
    private long id;
    private String tableName;
    private String content;
    private String eventName;
    private String userName;


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setRecord_id(long record_id) {
        this.record_id = record_id;
    }

    public void setSchema_name(String schema_name) {
        this.schema_name = schema_name;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void setAuditTime(LocalDateTime auditTime) {
        this.auditTime = auditTime;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public long getRecord_id() {
        return record_id;
    }

    public String getSchema_name() {
        return schema_name;
    }

    public long getUserId() {
        return userId;
    }

    public LocalDateTime getAuditTime() {
        return auditTime;
    }

    public long getId() {
        return id;
    }

    public String getTableName() {
        return tableName;
    }

    public String getContent() {
        return content;
    }

    public String getEventName() {
        return eventName;
    }
}
