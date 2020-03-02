package kz.bsbnb.usci.wsclient.dao;

import kz.bsbnb.usci.util.SqlJdbcConverter;
import kz.bsbnb.usci.wsclient.model.currency.CurrencyEntityCustom;
import kz.bsbnb.usci.wsclient.model.currency.HolidayEntityCustom;
import kz.bsbnb.usci.wsclient.model.currency.NSIEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

@Repository
public class NSIDaoImpl implements NSIDao {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate npJdbcTemplate;

    public NSIDaoImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate npJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.npJdbcTemplate = npJdbcTemplate;
    }

    @Override
    public void saveCurrencyRates(List<NSIEntity> currList) {
        /*HashMap<Long, NSIEntity> currencyFromDB = new HashMap<>();
        HashMap<Long, NSIEntity> currencyFromWS = new HashMap<>();

        String query = "SELECT t.* from USCI_WS.NSI_CURRENCY t";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

        for (Map<String, Object> row : rows) {
            NSIEntity currencyEntity = new NSIEntity();
            NSIEntitySystem currencyEntitySystem = new NSIEntitySystem();
            CurrencyEntityCustom currencyEntityCustom = new CurrencyEntityCustom();
            currencyEntity.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
            currencyEntitySystem.setOperType(SqlJdbcConverter.convertObjectToString(row.get("OPER_TYPE")));
            currencyEntitySystem.setEntityID(SqlJdbcConverter.convertToLong(row.get("ENTITY_ID")));
            currencyEntitySystem.setOperDate(SqlJdbcConverter.convertToLocalDate((Timestamp) row.get("OPER_DATE")));
            currencyEntitySystem.setBeginDate(SqlJdbcConverter.convertToLocalDate((Timestamp) row.get("BEGIN_DATE")));
            currencyEntitySystem.setEndDate(SqlJdbcConverter.convertToLocalDate((Timestamp) row.get("END_DATE")));
            currencyEntity.setNSIEntitySystem(currencyEntitySystem);
            currencyEntityCustom.setCurrId(SqlJdbcConverter.convertToLong(row.get("CURRENCY_ID")));
            currencyEntityCustom.setCurrCode(SqlJdbcConverter.convertObjectToString(row.get("CURRENCY_CODE")));
            currencyEntityCustom.setCourseDate(SqlJdbcConverter.convertToLocalDate((Timestamp) row.get("COURSE_DATE")));
            currencyEntityCustom.setCourseKind(SqlJdbcConverter.convertToLong(row.get("COURSE_KIND")));
            currencyEntityCustom.setCourse(SqlJdbcConverter.convertToDouble(row.get("COURSE")));
            currencyEntityCustom.setCorellation(SqlJdbcConverter.convertToLong(row.get("CORELLATION")));
            currencyEntityCustom.setWdKind(SqlJdbcConverter.convertToLong(row.get("WD_KIND")));
            currencyEntity.setCurrencyEntityCustom(currencyEntityCustom);

            currencyFromDB.put(currencyEntity.getNSIEntitySystem().getEntityID(), currencyEntity);
        }

        for (NSIEntity currencyEntity : currList) {
            currencyFromWS.put(currencyEntity.getNSIEntitySystem().getEntityID(), currencyEntity);
        }


        Set<Long> toAdd = SetUtils.difference(currencyFromWS.keySet(), currencyFromDB.keySet());
        List<NSIEntity> currListToAdd = new ArrayList<>();
        for (Long id : toAdd) {
            NSIEntity currencyEntity = currencyFromWS.get(id);
            currListToAdd.add(currencyEntity);
        }*/

        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName("USCI_WS")
                .withTableName("NSI_CURRENCY")
                .usingGeneratedKeyColumns("ID");

        List<Map<String, Object>> batchValues = new ArrayList<>(currList.size());
        for (NSIEntity NSIEntity : currList) {
            Map<String, Object> map = new HashMap<>();
            map.put("OPER_TYPE", NSIEntity.getNSIEntitySystem().getOperType());
            map.put("ENTITY_ID", NSIEntity.getNSIEntitySystem().getEntityID());
            map.put("OPER_DATE", SqlJdbcConverter.convertToSqlDate(NSIEntity.getNSIEntitySystem().getOperDate()));
            map.put("BEGIN_DATE", SqlJdbcConverter.convertToSqlDate(NSIEntity.getNSIEntitySystem().getBeginDate()));
            map.put("END_DATE", SqlJdbcConverter.convertToSqlDate(NSIEntity.getNSIEntitySystem().getEndDate()));
            map.put("CURRENCY_ID", ((CurrencyEntityCustom) NSIEntity.getNSIEntityCustom()).getCurrId());
            map.put("CURRENCY_CODE", ((CurrencyEntityCustom) NSIEntity.getNSIEntityCustom()).getCurrCode());
            map.put("COURSE_DATE", SqlJdbcConverter.convertToSqlDate(((CurrencyEntityCustom) NSIEntity.getNSIEntityCustom()).getCourseDate()));
            map.put("COURSE_KIND", ((CurrencyEntityCustom) NSIEntity.getNSIEntityCustom()).getCourseKind());
            map.put("COURSE", ((CurrencyEntityCustom) NSIEntity.getNSIEntityCustom()).getCourse());
            map.put("CORELLATION", ((CurrencyEntityCustom) NSIEntity.getNSIEntityCustom()).getCorellation());
            map.put("WD_KIND", ((CurrencyEntityCustom) NSIEntity.getNSIEntityCustom()).getWdKind());

            batchValues.add(map);
        }
        simpleJdbcInsert.executeBatch(batchValues.toArray(new Map[currList.size()]));

    }

    @Override
    public LocalDate getMaxCourseDate() {
        return SqlJdbcConverter.convertToLocalDate(jdbcTemplate.queryForObject("select max(COURSE_DATE) from usci_ws.nsi_currency", Date.class));
    }

    @Override
    public void saveHolidayDates(List<NSIEntity> currList) {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName("USCI_WS")
                .withTableName("NSI_HOLIDAY")
                .usingGeneratedKeyColumns("ID");

        List<Map<String, Object>> batchValues = new ArrayList<>(currList.size());
        for (NSIEntity NSIEntity : currList) {
            Map<String, Object> map = new HashMap<>();
            map.put("OPER_TYPE", NSIEntity.getNSIEntitySystem().getOperType());
            map.put("ENTITY_ID", NSIEntity.getNSIEntitySystem().getEntityID());
            map.put("OPER_DATE", SqlJdbcConverter.convertToSqlDate(NSIEntity.getNSIEntitySystem().getOperDate()));
            map.put("BEGIN_DATE", SqlJdbcConverter.convertToSqlDate(NSIEntity.getNSIEntitySystem().getBeginDate()));
            map.put("END_DATE", SqlJdbcConverter.convertToSqlDate(NSIEntity.getNSIEntitySystem().getEndDate()));
            map.put("HOLIDAY_DATE", SqlJdbcConverter.convertToSqlDate(((HolidayEntityCustom) NSIEntity.getNSIEntityCustom()).getHolidayDate()));
            map.put("HOLIDAY_TYPE", ((HolidayEntityCustom) NSIEntity.getNSIEntityCustom()).getHolidayType());

            batchValues.add(map);
        }
        simpleJdbcInsert.executeBatch(batchValues.toArray(new Map[currList.size()]));
    }
}
