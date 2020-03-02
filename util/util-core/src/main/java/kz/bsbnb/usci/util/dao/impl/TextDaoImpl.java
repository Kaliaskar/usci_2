package kz.bsbnb.usci.util.dao.impl;

import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.util.Text;
import kz.bsbnb.usci.util.dao.TextDao;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Yernur Bakash
 */

@Repository
public class TextDaoImpl implements TextDao {
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private SimpleJdbcInsert textInsert;

    public TextDaoImpl(NamedParameterJdbcTemplate npJdbcTemplate) {
        this.npJdbcTemplate = npJdbcTemplate;

        this.textInsert = new SimpleJdbcInsert(npJdbcTemplate.getJdbcTemplate())
                .withSchemaName("USCI_UTIL")
                .withTableName("CONSTANT_")
                .usingGeneratedKeyColumns("ID");
    }

    @Override
    public Long insert(Text text) {
        Number id = textInsert.executeAndReturnKey(new MapSqlParameterSource()
                .addValue("TYPE", text.getType())
                .addValue("CODE", text.getCode())
                .addValue("NAME_RU", text.getNameRu())
                .addValue("NAME_KZ", text.getNameKz()));

        text.setId(id.longValue());

        return id.longValue();
    }

    @Override
    public void update(Text text) {
        int count = npJdbcTemplate.update("update USCI_UTIL.CONSTANT_\n" +
                        "   set TYPE = :type, CODE = :code, NAME_RU = :nameRu, NAME_KZ = :nameKz\n" +
                        " where ID = :id",
                new MapSqlParameterSource("id", text.getId())
                        .addValue("type", text.getType())
                        .addValue("code", text.getCode())
                        .addValue("nameRu", text.getNameRu())
                        .addValue("nameKz", text.getNameKz()));

        if (count != 1)
            throw new UsciException("Ошибка update записи в таблице USCI_UTIL.CONSTANT_");
    }

    @Override
    public void delete(Long id) {
        int count = npJdbcTemplate.update("delete from USCI_UTIL.CONSTANT_ where ID = :id",
                new MapSqlParameterSource("id", id));

        if (count != 1)
            throw new UsciException("Ошибка delete записи из таблицы USCI_UTIL.CONSTANT_");
    }

    @Override
    public Text get(String type, String code) {
        return npJdbcTemplate.queryForObject("select * from USCI_UTIL.CONSTANT_ where TYPE = :type and CODE = :code",
                new MapSqlParameterSource("type", type)
                        .addValue("code", code),
                new TextMapper());
    }

    @Override
    public Text get(Long id) {
        return npJdbcTemplate.queryForObject("select * from USCI_UTIL.CONSTANT_ where ID = :id",
                new MapSqlParameterSource("id", id), new TextMapper());
    }

    @Override
    public List<Text> getAll() {
        return npJdbcTemplate.query("select * from USCI_UTIL.CONSTANT_", new TextMapper());
    }

    @Override
    public List<Text> getConstantsByType(List<String> types) {
        return npJdbcTemplate.query("select * from USCI_UTIL.CONSTANT_ where TYPE in (:types)",
                new MapSqlParameterSource("types", types),
                new TextMapper());
    }

    private class TextMapper implements RowMapper<Text> {

        TextMapper() {
        }

        public Text mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Text()
                    .setId(rs.getLong("ID"))
                    .setType(rs.getString("TYPE"))
                    .setCode(rs.getString("CODE"))
                    .setNameRu(rs.getString("NAME_RU"))
                    .setNameKz(rs.getString("NAME_KZ"));
        }
    }

}
