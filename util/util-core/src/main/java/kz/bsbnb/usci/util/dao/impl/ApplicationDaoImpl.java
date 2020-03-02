package kz.bsbnb.usci.util.dao.impl;

import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.util.Application;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import kz.bsbnb.usci.util.dao.ApplicationDao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Jandos Iskakov
 */

@Repository
public class ApplicationDaoImpl implements ApplicationDao {
    private final JdbcTemplate jdbcTemplate;

    public ApplicationDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Application> getAppList() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from USCI_UTIL.APP");

        if (rows.size() == 0)
            throw new UsciException("Таблица USCI_UTIL.APP не содержит модулей");

        List<Application> applications = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            Application application = new Application();

            application.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
            application.setHost(String.valueOf(row.get("HOST")));
            application.setPort(SqlJdbcConverter.convertToInt(row.get("PORT")));
            application.setModule(String.valueOf(row.get("MODULE")));
            application.setRmiPort(SqlJdbcConverter.convertToInt(row.get("RMI_PORT")));
            application.setProduct(row.get("PRODUCT") != null? String.valueOf(row.get("PRODUCT")): null);

            applications.add(application);
        }

        return applications;
    }

}
