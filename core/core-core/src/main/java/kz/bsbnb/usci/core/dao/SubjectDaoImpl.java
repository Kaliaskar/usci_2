package kz.bsbnb.usci.core.dao;

import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.respondent.SubjectType;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class SubjectDaoImpl implements SubjectDao {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate npJdbcTemplate;

    public SubjectDaoImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate npJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.npJdbcTemplate = npJdbcTemplate;
    }

    @Override
    public Long getSubjectTypeProductPeriod(Long subjectTypeId, Long productId) {
        Long periodId;
        try {
            periodId = jdbcTemplate.queryForObject("select PERIOD_ID\n " +
                    "  from USCI_RSPDENT.SUBJECT_TYPE_PERIOD\n " +
                    "where SUBJECT_TYPE_ID = ?\n" +
                    "  and PRODUCT_ID = ?", new Object[]{subjectTypeId, productId}, Long.class);
        } catch (EmptyResultDataAccessException e) {
            throw new UsciException("Тип субьекта не настроен на периодичность по продукту");
        }
        return periodId;
    }

    @Override
    public List<SubjectType> getSubjectTypeList() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from EAV_DATA.REF_SUBJECT_TYPE");

        List<SubjectType> subjectTypeList = new ArrayList<>();
        for (Map<String, Object> row : rows)
            subjectTypeList.add(getSubjectFromJdbcMap(row));

        return subjectTypeList;
    }

    @Override
    public void updateSubjectTypeProductPeriod(Long subjectTypeId, Long productId, Long periodId) {

        List<Map<String, Object>> rows  = npJdbcTemplate.queryForList("select PERIOD_ID\n " +
                "  from USCI_RSPDENT.SUBJECT_TYPE_PERIOD\n " +
                "where SUBJECT_TYPE_ID = :subjectTypeId \n" +
                "  and PRODUCT_ID = :productId",
                new MapSqlParameterSource("subjectTypeId", subjectTypeId)
                        .addValue("productId", productId));

        if (!rows.isEmpty()) {
            int count = npJdbcTemplate.update("update USCI_RSPDENT.SUBJECT_TYPE_PERIOD set PERIOD_ID = :periodId \n" +
                            "where SUBJECT_TYPE_ID = :subjectTypeId \n" +
                            "and PRODUCT_ID = :productId \n",
                    new MapSqlParameterSource("periodId", periodId)
                            .addValue("subjectTypeId", subjectTypeId)
                            .addValue("productId", productId));

            if (count != 1)
                throw new UsciException("Ошибка update записи из таблицы USCI_RSPDENT.SUBJECT_TYPE_PERIOD");
        } else {
            SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                    .withSchemaName("USCI_RSPDENT")
                    .withTableName("SUBJECT_TYPE_PERIOD")
                    .usingGeneratedKeyColumns("ID")
                    .usingColumns("SUBJECT_TYPE_ID","PERIOD_ID","PRODUCT_ID");

            int count = simpleJdbcInsert.execute(new MapSqlParameterSource()
                    .addValue("SUBJECT_TYPE_ID", subjectTypeId)
                    .addValue("PERIOD_ID", periodId)
                    .addValue("PRODUCT_ID", productId));
            if (count != 1)
                throw new UsciException("Ошибка insert записи в таблицу USCI_RSPDENT.SUBJECT_TYPE_PERIOD");
        }

    }

    private static SubjectType getSubjectFromJdbcMap(Map<String, Object> row) {
        SubjectType subjectType = new SubjectType();
        subjectType.setId(SqlJdbcConverter.convertToLong(row.get("ENTITY_ID")));
        subjectType.setCode(SqlJdbcConverter.convertObjectToString(row.get("CODE")));
        subjectType.setNameRu(SqlJdbcConverter.convertObjectToString(row.get("NAME_RU")));
        subjectType.setNameKz(SqlJdbcConverter.convertObjectToString(row.get("NAME_KZ")));
        return subjectType;
    }
}
