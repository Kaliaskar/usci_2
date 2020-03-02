package kz.bsbnb.usci.core.dao;


import kz.bsbnb.usci.core.service.UserService;
import kz.bsbnb.usci.model.creditor.AuditEvent;

import kz.bsbnb.usci.model.json.AuditJson;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Repository
public class AuditDaoImpl implements AuditDao {
    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private SimpleJdbcInsert auditInsert;
    public AuditDaoImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate npJdbcTemplate,UserService userService) {
        this.jdbcTemplate = jdbcTemplate;
        this.npJdbcTemplate = npJdbcTemplate;
        this.userService = userService;

        this.auditInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName("USCI_AUDIT")
                .withTableName("AUDIT_LOG")
                .usingGeneratedKeyColumns("ID");

    }
    @Override
    public void insertAuditEvent(AuditEvent auditEvent) {
        Number id = auditInsert.executeAndReturnKey(new MapSqlParameterSource()
                .addValue("USER_ID", auditEvent.getUserId())
                .addValue("RECORD_ID", auditEvent.getRecord_id())
                .addValue("SCHEMA_NAME", auditEvent.getSchema_name())
                .addValue("AUDIT_TIME", SqlJdbcConverter.convertToSqlDate(auditEvent.getAuditTime()))
                .addValue("TABLE_NAME", auditEvent.getTableName())
                .addValue("CONTENT", auditEvent.getContent())
                .addValue("EVENT_NAME", auditEvent.getEventName()));
        auditEvent.setId(id.longValue());
    }
    @Override
    public List<AuditEvent> getAudit(long userId) {
        ArrayList<AuditEvent> auditEvents = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId);

        String query  = "select * from USCI_AUDIT.AUDIT_LOG t where t.user_id = :userId";
        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query, params);

        for (Map<String, Object> row : rows) {
            AuditEvent auditEvent = new AuditEvent();
            auditEvent.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
            auditEvent.setRecord_id(SqlJdbcConverter.convertToLong(row.get("RECORD_ID")));
            auditEvent.setUserId(SqlJdbcConverter.convertToLong(row.get("USER_ID")));
            auditEvent.setAuditTime(SqlJdbcConverter.convertToSqlDate((Date) row.get("AUDIT_TIME")));
            auditEvent.setSchema_name((String)row.get("SCHEMA_NAME"));
            auditEvent.setTableName((String)row.get("TABLE_NAME"));
            auditEvent.setContent((String)row.get("CONTENT"));
            auditEvent.setEventName((String)row.get("EVENT_NAME"));
            auditEvents.add(auditEvent);
        }

        return auditEvents;
    }
    @Override
    public List<AuditJson> getAuditJson(long userId) {
        ArrayList<AuditJson> auditEvents = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId);

        String query  = "select * from USCI_AUDIT.AUDIT_LOG t where t.user_id = :userId";
        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query, params);

        for (Map<String, Object> row : rows) {
            AuditJson auditEvent = new AuditJson();
            auditEvent.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
            //auditEvent.setUserName(userService.getUser(userId).getFullName());
            auditEvent.setRecord_id(SqlJdbcConverter.convertToLong(row.get("RECORD_ID")));
            auditEvent.setUserId(SqlJdbcConverter.convertToLong(row.get("USER_ID")));
            auditEvent.setAuditTime(SqlJdbcConverter.convertToLocalDateTime(row.get("AUDIT_TIME")));
            auditEvent.setSchema_name((String)row.get("SCHEMA_NAME"));
            auditEvent.setTableName((String)row.get("TABLE_NAME"));
            auditEvent.setContent((String)row.get("CONTENT"));
            auditEvent.setEventName((String)row.get("EVENT_NAME"));
            auditEvents.add(auditEvent);
        }

        return auditEvents;
    }
}
