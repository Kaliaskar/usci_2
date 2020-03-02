package kz.bsbnb.usci.eav.dao;

import kz.bsbnb.usci.eav.model.core.EavHub;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Jandos Iskakov
 */

@Repository
public class BaseEntityHubDaoImpl implements BaseEntityHubDao {
    private static final Logger logger = LoggerFactory.getLogger(BaseEntityHubDaoImpl.class);
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private SimpleJdbcInsert eavHubInsert;

    public BaseEntityHubDaoImpl(NamedParameterJdbcTemplate npJdbcTemplate) {
        this.npJdbcTemplate = npJdbcTemplate;

        this.eavHubInsert = new SimpleJdbcInsert(npJdbcTemplate.getJdbcTemplate())
                .withSchemaName("EAV_DATA")
                .withTableName("EAV_HUB")
                .usingColumns("ENTITY_ID", "PARENT_ENTITY_ID", "PARENT_CLASS_ID", "CREDITOR_ID",
                        "BATCH_ID", "CREATION_DATE", "LAST_UPDATE_DATE", "ENTITY_KEY", "CLASS_ID");
    }

    @Override
    public void insert(List<EavHub> hubs) {
        List<MapSqlParameterSource> params = new ArrayList<>();

        for (EavHub eavHub : hubs)
            params.add(new MapSqlParameterSource()
                    .addValue("ENTITY_ID", eavHub.getEntityId())
                    .addValue("PARENT_ENTITY_ID", eavHub.getParentEntityId())
                    .addValue("PARENT_CLASS_ID", eavHub.getParentClassId())
                    .addValue("CREDITOR_ID", eavHub.getRespondentId())
                    .addValue("BATCH_ID", eavHub.getBatchId())
                    .addValue("CREATION_DATE", SqlJdbcConverter.convertToSqlTimestamp(LocalDateTime.now()))
                    .addValue("LAST_UPDATE_DATE", SqlJdbcConverter.convertToSqlTimestamp(LocalDateTime.now()))
                    .addValue("ENTITY_KEY", eavHub.getEntityKey())
                    .addValue("CLASS_ID", eavHub.getMetaClassId()));

        if (hubs.size() > 1) {
            int counts[] = eavHubInsert.executeBatch(params.toArray(new SqlParameterSource[0]));
            if (Arrays.stream(counts).anyMatch(value -> value != 1))
                throw new UsciException(String.format("Ошибка insert(batch) записей %s в таблицу EAV_DATA.EAV_HUB", hubs));
        }
        else {
            int count = eavHubInsert.execute(params.get(0));
            if (count != 1)
                throw new UsciException(String.format("Ошибка insert записи %s в таблицу EAV_DATA.EAV_HUB", hubs.get(0)));
        }
    }

    @Override
    public Optional<Long> find(EavHub eavHub, List<String> keys) {
        try {
            String query = "select distinct ENTITY_ID\n" +
                    "  from EAV_DATA.EAV_HUB\n" +
                    " where CREDITOR_ID = :RESPONDENT_ID\n" +
                    "   and CLASS_ID = :META_CLASS_ID\n" +
                    "   and PARENT_ENTITY_ID = :PARENT_ENTITY_ID\n" +
                    "   and PARENT_CLASS_ID = :PARENT_CLASS_ID\n";

            MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("RESPONDENT_ID", eavHub.getRespondentId())
                .addValue("META_CLASS_ID", eavHub.getMetaClassId())
                .addValue("PARENT_ENTITY_ID", eavHub.getParentEntityId())
                .addValue("PARENT_CLASS_ID", eavHub.getParentClassId());

            if (keys.size() == 1) {
                query += "   and ENTITY_KEY = :KEY\n";
                params.addValue("KEY", keys.get(0));
            }
            else {
                query += "   and ENTITY_KEY in (:KEY_LIST)\n";
                params.addValue("KEY_LIST", keys);
            }

            return Optional.ofNullable(npJdbcTemplate.queryForObject(query, params, Long.class));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (IncorrectResultSizeDataAccessException e) {
            throw new UsciException(String.format("Найдено более одной записи в таблице EAV_DATA.EAV_HUB по ключам %s и параметрам %s", keys, eavHub));
        }
    }

    @Override
    public void delete(EavHub eavHub) {
        String deleteQuery = "delete from EAV_DATA.EAV_HUB\n" +
                " where CREDITOR_ID = :RESPONDENT_ID\n" +
                "   and CLASS_ID = :CLASS_ID\n" +
                "   and ENTITY_ID = :ENTITY_ID\n" +
                "   and PARENT_ENTITY_ID = :PARENT_ENTITY_ID\n" +
                "   and PARENT_CLASS_ID = :PARENT_CLASS_ID\n";

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("ENTITY_ID", eavHub.getEntityId())
            .addValue("CLASS_ID", eavHub.getMetaClassId())
            .addValue("RESPONDENT_ID", eavHub.getRespondentId())
            .addValue("PARENT_ENTITY_ID", eavHub.getParentEntityId())
            .addValue("PARENT_CLASS_ID", eavHub.getParentClassId());


        int count = npJdbcTemplate.update(deleteQuery, params);
        if (count == 0)
            throw new UsciException(String.format("Ошибка delete записи %s из таблицы EAV_DATA.EAV_HUB", eavHub));
    }

    @Override
    public void update(EavHub eavHub) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("LAST_UPDATE_DATE", SqlJdbcConverter.convertToSqlTimestamp(LocalDateTime.now()))
                .addValue("ENTITY_ID", eavHub.getEntityId())
                .addValue("CLASS_ID", eavHub.getMetaClassId())
                .addValue("RESPONDENT_ID", eavHub.getRespondentId())
                .addValue("PARENT_ENTITY_ID", eavHub.getParentEntityId())
                .addValue("PARENT_CLASS_ID", eavHub.getParentClassId())
                .addValue("BATCH_ID", eavHub.getBatchId())
                .addValue("ENTITY_KEY", eavHub.getEntityKey())
                .addValue("NEW_ENTITY_KEY", eavHub.getNewEntityKey());

        int count = npJdbcTemplate.update("update EAV_DATA.EAV_HUB\n" +
                "  set ENTITY_KEY = :NEW_ENTITY_KEY,\n" +
                "      BATCH_ID = :BATCH_ID,\n" +
                "      LAST_UPDATE_DATE = :LAST_UPDATE_DATE\n" +
                "where CREDITOR_ID = :RESPONDENT_ID\n" +
                "  and CLASS_ID = :CLASS_ID\n" +
                "  and ENTITY_ID = :ENTITY_ID\n" +
                "  and PARENT_ENTITY_ID = :PARENT_ENTITY_ID\n" +
                "  and PARENT_CLASS_ID = :PARENT_CLASS_ID\n" +
                "  and ENTITY_KEY = :ENTITY_KEY", params);

        if (count == 0)
            throw new UsciException(String.format("Ошибка update записи %s в таблице EAV_DATA.EAV_HUB", eavHub));
    }

}
