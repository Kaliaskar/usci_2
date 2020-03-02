package kz.bsbnb.usci.eav.service;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class BaseEntityDateServiceImpl implements BaseEntityDateSerivce {
    private final JdbcTemplate jdbcTemplate;

    public BaseEntityDateServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LocalDate getMaxReportDate(BaseEntity baseEntity, LocalDate reportDate) {
        Objects.requireNonNull(baseEntity.getId(), String.format("Отсутствует ID у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getRespondentId(), String.format("Отсутствует ID кредитора у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getMetaClass(), String.format("Отсутствует мета класс у сущности %s", baseEntity));
        Objects.requireNonNull(reportDate, "Отчетная дата не задана");

        MetaClass metaClass = baseEntity.getMetaClass();

        String query = String.format("select max(REPORT_DATE) from %s.%s where REPORT_DATE <= ? and ENTITY_ID = ? and CREDITOR_ID = ?",
                metaClass.getSchemaData(), metaClass.getTableName());

        List<LocalDate> rows = jdbcTemplate.queryForList(query, new Object[] {SqlJdbcConverter.convertToSqlDate(reportDate),
                baseEntity.getId(), baseEntity.getRespondentId()}, LocalDate.class);
        if (rows.size() > 1)
            throw new UsciException(String.format("Найдено более одной записи %s", baseEntity));

        return rows.get(0);
    }

    @Override
    public LocalDate getMinReportDate(BaseEntity baseEntity) {
        Objects.requireNonNull(baseEntity.getId(), String.format("Отсутствует ID у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getRespondentId(), String.format("Отсутствует ID кредитора у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getMetaClass(), String.format("Отсутствует мета класс у сущности %s", baseEntity));

        MetaClass metaClass = baseEntity.getMetaClass();

        String query = String.format("select min(REPORT_DATE) from %s.%s where ENTITY_ID = ? and CREDITOR_ID = ?",
                metaClass.getSchemaData(), metaClass.getTableName());

        List<LocalDate> rows = jdbcTemplate.queryForList(query, new Object[] {baseEntity.getId(), baseEntity.getRespondentId()}, LocalDate.class);
        if (rows.size() > 1)
            throw new UsciException(String.format("Найдено более одной записи %s", baseEntity));

        return rows.get(0);
    }

    @Override
    public LocalDate getMaxReportDate(BaseEntity baseEntity) {
        Objects.requireNonNull(baseEntity.getId(), String.format("Отсутствует ID у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getRespondentId(), String.format("Отсутствует ID кредитора у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getMetaClass(), String.format("Отсутствует мета класс у сущности %s", baseEntity));

        MetaClass metaClass = baseEntity.getMetaClass();

        String query = String.format("select max(REPORT_DATE) from %s.%s where ENTITY_ID = ? and CREDITOR_ID = ?",
                metaClass.getSchemaData(), metaClass.getTableName());

        List<LocalDate> rows = jdbcTemplate.queryForList(query, new Object[] {baseEntity.getId(), baseEntity.getRespondentId()}, LocalDate.class);
        if (rows.size() > 1)
            throw new UsciException(String.format("Найдено более одной записи %s", baseEntity));

        return rows.get(0);
    }

    @Override
    public LocalDate getMinReportDate(BaseEntity baseEntity, LocalDate reportDate) {
        Objects.requireNonNull(baseEntity.getId(), String.format("Отсутствует ID у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getRespondentId(), String.format("Отсутствует ID кредитора у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getMetaClass(), String.format("Отсутствует мета класс у сущности %s", baseEntity));
        Objects.requireNonNull(reportDate, "Отчетная дата не задана");

        MetaClass metaClass = baseEntity.getMetaClass();

        String query = String.format("select min(REPORT_DATE) from %s.%s where REPORT_DATE > ? and ENTITY_ID = ? and CREDITOR_ID = ?",
                metaClass.getSchemaData(), metaClass.getTableName());

        List<LocalDate> rows = jdbcTemplate.queryForList(query, new Object[] {SqlJdbcConverter.convertToSqlDate(reportDate),
                baseEntity.getId(), baseEntity.getRespondentId()}, LocalDate.class);
        if (rows.size() > 1)
            throw new UsciException(String.format("Найдено более одной записи %s", baseEntity));

        return rows.get(0);
    }

}
