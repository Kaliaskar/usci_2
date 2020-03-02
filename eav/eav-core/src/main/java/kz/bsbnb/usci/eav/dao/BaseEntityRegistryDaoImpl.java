package kz.bsbnb.usci.eav.dao;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseEntityRegistry;
import kz.bsbnb.usci.model.exception.UsciException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Jandos Iskakov
 */

@Repository
public class BaseEntityRegistryDaoImpl implements BaseEntityRegistryDao {
    private static final BaseEntityRegistryMapper BASE_ENTITY_REGISTRY_MAPPER = new BaseEntityRegistryMapper();

    private final JdbcTemplate jdbcTemplate;
    private SimpleJdbcInsert baseEntityRegistryInsert;

    public BaseEntityRegistryDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

        this.baseEntityRegistryInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName("EAV_DATA")
                .withTableName("EAV_ENTITY")
                .usingColumns("ENTITY_ID", "PARENT_ENTITY_ID", "PARENT_CLASS_ID", "CREDITOR_ID", "CLASS_ID");
    }

    @Override
    public void insert(List<BaseEntityRegistry> baseEntityRegistries) {
        List<MapSqlParameterSource> params = new ArrayList<>();

        for (BaseEntityRegistry info : baseEntityRegistries)
            params.add(new MapSqlParameterSource()
                .addValue("ENTITY_ID", info.getEntityId())
                .addValue("PARENT_ENTITY_ID", info.getParentEntityId())
                .addValue("PARENT_CLASS_ID", info.getParentClassId())
                .addValue("CREDITOR_ID", info.getRespondentId())
                .addValue("CLASS_ID", info.getMetaClassId()));

        if (baseEntityRegistries.size() > 1) {
            int counts[] = baseEntityRegistryInsert.executeBatch(params.toArray(new SqlParameterSource[0]));
            if (Arrays.stream(counts).anyMatch(value -> value != 1))
                throw new UsciException(String.format("Ошибка insert(batch) записей %s в таблицу EAV_DATA.EAV_ENTITY", baseEntityRegistries));
        }
        else {
            int count = baseEntityRegistryInsert.execute(params.get(0));
            if (count != 1)
                throw new UsciException(String.format("Ошибка insert записи %s в таблицу EAV_DATA.EAV_ENTITY", baseEntityRegistries.get(0)));
        }
    }

    @Override
    public Optional<BaseEntityRegistry> find(BaseEntity baseEntity) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("select *\n" +
                    "  from EAV_DATA.EAV_ENTITY\n" +
                    " where CREDITOR_ID = ?\n" +
                    "   and CLASS_ID = ?\n" +
                    "   and ENTITY_ID = ?\n",
                    BASE_ENTITY_REGISTRY_MAPPER,
                    baseEntity.getRespondentId(), baseEntity.getMetaClass().getId(), baseEntity.getId()));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (IncorrectResultSizeDataAccessException e) {
            throw new UsciException(String.format("Найдено более одной записи в таблице EAV_DATA.EAV_ENTITY по сущности %s", baseEntity));
        }
    }

    @Override
    public List<BaseEntityRegistry> getBaseEntitiesByMetaClass(Long metaClassId) {
        return jdbcTemplate.query("select * from EAV_DATA.EAV_ENTITY where CLASS_ID = ?", new Object[]{metaClassId}, BASE_ENTITY_REGISTRY_MAPPER);
    }

    private static class BaseEntityRegistryMapper implements RowMapper<BaseEntityRegistry> {

        @Override
        public BaseEntityRegistry mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new BaseEntityRegistry()
                    .setRespondentId(rs.getLong("CREDITOR_ID"))
                    .setMetaClassId(rs.getLong("CLASS_ID"))
                    .setEntityId(rs.getLong("ENTITY_ID"))
                    .setParentEntityId(rs.getLong("PARENT_ENTITY_ID"))
                    .setParentClassId(rs.getLong("PARENT_CLASS_ID"));
        }
    }

}
