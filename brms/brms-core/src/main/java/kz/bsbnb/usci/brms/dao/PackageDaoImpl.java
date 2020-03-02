package kz.bsbnb.usci.brms.dao;

import kz.bsbnb.usci.brms.model.RulePackage;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Aibek Bukabaev
 */

@Component
public class PackageDaoImpl implements PackageDao {
    private final JdbcTemplate jdbcTemplate;

    public PackageDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RulePackage loadBatch(long id) {
        if (id < 1)
            throw new UsciException("Не имеет id. Не возможно загружать.");

        String query = "select * from USCI_RULE.PACKAGE_ WHERE ID  = ?";
        RulePackage batch = jdbcTemplate.queryForObject(query, new Object[]{id}, new BatchMapper());
        return batch;
    }

    private class BatchMapper implements RowMapper<RulePackage> {
        @Override
        public RulePackage mapRow(ResultSet resultSet, int i) throws SQLException {
            RulePackage batch = new RulePackage();
            batch.setId(resultSet.getLong("id"));
            batch.setName(resultSet.getString("name"));
            batch.setDescription(resultSet.getString("description"));
            return batch;
        }
    }


    @Override
    @Transactional
    public long savePackage(String rulePackageName) {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName("USCI_RULE")
                .withTableName("PACKAGE_")
                .usingGeneratedKeyColumns("ID");

        Number id = simpleJdbcInsert.executeAndReturnKey(new MapSqlParameterSource()
                .addValue("NAME", rulePackageName));

        return id.longValue();
    }

    @Override
    public List<RulePackage> getAllPackages() {
        String query = "select * from USCI_RULE.PACKAGE_";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);
        List<RulePackage> rulePackageList = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            RulePackage rulePkg = new RulePackage();
            rulePkg.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
            rulePkg.setName((String) row.get("NAME"));
            rulePackageList.add(rulePkg);
        }

        return rulePackageList;
    }

}
