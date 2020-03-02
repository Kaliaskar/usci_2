package kz.bsbnb.usci.util.dao.impl;

import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.util.Config;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import kz.bsbnb.usci.util.dao.ConfigDao;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Jandos Iskakov
 */

@Repository
public class ConfigDaoImpl implements ConfigDao {
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private SimpleJdbcInsert configInsert;

    public ConfigDaoImpl(NamedParameterJdbcTemplate npJdbcTemplate) {
        this.npJdbcTemplate = npJdbcTemplate;

        this.configInsert = new SimpleJdbcInsert(npJdbcTemplate.getJdbcTemplate())
                .withSchemaName("USCI_UTIL")
                .withTableName("CONFIG")
                .usingGeneratedKeyColumns("ID");
    }

    @Override
    public Long insert(Config config) {
        Number id = configInsert.executeAndReturnKey(new MapSqlParameterSource()
                .addValue("MODULE", config.getModule())
                .addValue("CODE", config.getCode())
                .addValue("VALUE", config.getValue())
                .addValue("DESCRIPTION", config.getDescription()));

        config.setId(id.longValue());

        return id.longValue();
    }

    @Override
    public void update(Config config) {
        int count = npJdbcTemplate.update("update USCI_UTIL.CONFIG\n" +
                        "   set MODULE = :module, CODE = :code, VALUE = :value, DESCRIPTION = :description\n" +
                        " where ID = :id",
                new MapSqlParameterSource("id", config.getId())
                        .addValue("module", config.getModule())
                        .addValue("code", config.getCode())
                        .addValue("value", config.getValue())
                        .addValue("description", config.getDescription()));

        if (count != 1)
            throw new UsciException("Ошибка update записи в таблице USCI_UTIL.CONFIG");
    }

    @Override
    public void delete(Long id) {
        int count = npJdbcTemplate.update("delete from USCI_UTIL.CONFIG where ID = :id",
                new MapSqlParameterSource("id", id));

        if (count != 1)
            throw new UsciException("Ошибка delete записи из таблицы USCI_UTIL.CONFIG");
    }

    @Override
    public Config get(String module, String code) {
        return npJdbcTemplate.queryForObject("select * from USCI_UTIL.CONFIG where MODULE = :module and CODE = :code",
                new MapSqlParameterSource("module", module)
                        .addValue("code", code),
                (rs, rowNum) -> new Config(SqlJdbcConverter.convertToLong(rs.getObject("ID")),
                        rs.getString("MODULE"),
                        rs.getString("CODE"),
                        rs.getString("VALUE"),
                        rs.getString("DESCRIPTION")));
    }

    @Override
    public Config get(Long id) {
        return npJdbcTemplate.queryForObject("select * from USCI_UTIL.CONFIG where ID = :id",
                new MapSqlParameterSource("id", id),
                (rs, rowNum) -> new Config(SqlJdbcConverter.convertToLong(rs.getObject("ID")),
                        rs.getString("MODULE"),
                        rs.getString("CODE"),
                        rs.getString("VALUE"),
                        rs.getString("DESCRIPTION")));
    }

    @Override
    public List<Config> getList() {
        return npJdbcTemplate.query("select * from USCI_UTIL.CONFIG", new UsciConfigMapper());
    }

    private class UsciConfigMapper implements RowMapper<Config> {

        UsciConfigMapper() {
        }

        public Config mapRow(ResultSet rs, int rowNum) throws SQLException {
            Config config = new Config();

            config.setId(rs.getLong("ID"));
            config.setModule(rs.getString("MODULE"));
            config.setCode(rs.getString("CODE"));
            config.setValue(rs.getString("VALUE"));
            config.setDescription(rs.getString("DESCRIPTION"));

            return config;
        }
    }

}
