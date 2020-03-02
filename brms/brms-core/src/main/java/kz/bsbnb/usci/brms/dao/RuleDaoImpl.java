package kz.bsbnb.usci.brms.dao;

import kz.bsbnb.usci.brms.model.PackageVersion;
import kz.bsbnb.usci.brms.model.Rule;
import kz.bsbnb.usci.brms.model.RulePackage;
import kz.bsbnb.usci.brms.model.SimpleTrack;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

/**
 * @author Artur Tkachenko
 * @author Baurzhan Makhanbetov
 */

@Repository
public class RuleDaoImpl implements RuleDao {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate npJdbcTemplate;

    public RuleDaoImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate npJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.npJdbcTemplate = npJdbcTemplate;
    }

    public List<Rule> load(PackageVersion packageVersion) {
        String query = "SELECT rules.ID, rules.RULE, rules.TITLE, rules.OPEN_DATE, rules.CLOSE_DATE FROM USCI_RULE.RULE_ rules,\n" +
                "                USCI_RULE.RULE_PACKAGE rulePkg\n" +
                "                WHERE rules.ID = rulePkg.rule_ID\n" +
                "                       AND rules.OPEN_DATE <= :reportDate \n" +
                "                       AND (rules.CLOSE_DATE > :reportDate OR rules.CLOSE_DATE is null)\n" +
                "                       AND rulePkg.PACKAGE_ID = :packageId ";
        List<Rule> ruleList = new ArrayList<>();

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("reportDate", SqlJdbcConverter.convertToSqlDate(packageVersion.getReportDate()));
        params.addValue("packageId", packageVersion.getRulePackage().getId());

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query, params);

        for(Map<String,Object> row : rows) {
            Rule rule = getRule(row);
            ruleList.add(rule);
        }

        return ruleList;
    }

    @Override
    @Transactional
    public long update(Rule rule) {
        if (rule.getId() < 1L){
            throw new UsciException("Правило не имеет id.");
        }

        npJdbcTemplate.update("UPDATE USCI_RULE.RULE_\n" +
                        "   SET TITLE = :title ,\n" +
                        "   RULE = :rule ,\n" +
                        "   OPEN_DATE = :openDate\n" +
                        "   WHERE ID = :id",
                new MapSqlParameterSource("title", rule.getTitle())
                        .addValue("rule", rule.getRule())
                        .addValue("openDate", SqlJdbcConverter.convertToSqlDate(rule.getOpenDate()))
                        .addValue("id", rule.getId()));

        return rule.getId();
    }

    @Override
    public List<Rule> getAllRules() {
        String query = "select * from USCI_RULE.RULE_ order by id";
        return jdbcTemplate.query(query, new BeanPropertyRowMapper(Rule.class));
    }

    @Override
    public List<SimpleTrack> getRuleTitles(Long packageId, LocalDate reportDate) {
        String query = "SELECT rules.ID, rules.RULE, rules.TITLE, rules.OPEN_DATE, rules.CLOSE_DATE FROM USCI_RULE.RULE_ rules,\n" +
                "                 USCI_RULE.RULE_PACKAGE rulePkg\n" +
                "                WHERE rules.ID = rulePkg.rule_ID\n" +
                "                       AND rules.OPEN_DATE <= :reportDate \n" +
                "                       AND (rules.CLOSE_DATE > :reportDate OR rules.CLOSE_DATE is null)\n" +
                "                       AND rulePkg.PACKAGE_ID = :packageId";

        List<SimpleTrack> ret = new ArrayList<>();

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("reportDate", SqlJdbcConverter.convertToSqlDate(reportDate));
        params.addValue("packageId", packageId);

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query, params);

        for(Map<String,Object> row: rows) {
            SimpleTrack s = new SimpleTrack();
            s.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
            s.setName((String) row.get("TITLE"));
            s.setIsActive(true);
            ret.add(s);
        }

        return ret;
    }

    @Override
    public List<SimpleTrack> getRuleTitles(Long packageId, LocalDate reportDate, String searchText) {
        searchText = searchText.toLowerCase();
        String query = "SELECT rules.ID, rules.TITLE AS NAME, rules.RULE FROM USCI_RULE.RULE_ rules, USCI_RULE.RULE_PACKAGE rulePkg\n" +
                "                 WHERE rules.ID = rulePkg.rule_ID\n" +
                "                       AND rules.OPEN_DATE <= :reportDate \n" +
                "                       AND (rules.CLOSE_DATE > :reportDate OR rules.CLOSE_DATE is null)\n" +
                "                       AND (LOWER(rules.rule) LIKE :searchText OR LOWER(rules.title) LIKE :searchText)\n" +
                "                       AND rulePkg.PACKAGE_ID = :packageId ";

        List<SimpleTrack> ret = new ArrayList<>();

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("reportDate", SqlJdbcConverter.convertToSqlDate(reportDate));
        params.addValue("packageId", packageId);
        params.addValue("searchText","%"+searchText+"%");

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query, params);

        for(Map<String,Object> row: rows) {
            SimpleTrack s = new SimpleTrack();
            s.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
            s.setName((String) row.get("NAME"));
            s.setIsActive(true);
            ret.add(s);
        }

        return ret;
    }

    @Override
    @Transactional
    public long save(Rule rule, PackageVersion packageVersion){
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName("USCI_RULE")
                .withTableName("RULE_")
                .usingGeneratedKeyColumns("ID");

        Number id = simpleJdbcInsert.executeAndReturnKey(new MapSqlParameterSource()
                .addValue("TITLE", rule.getTitle())
                .addValue("RULE", rule.getRule())
                .addValue("OPEN_DATE", SqlJdbcConverter.convertToSqlDate(packageVersion.getReportDate())));

        rule.setId(id.longValue());

        return id.longValue();
    }

    @Override
    public Rule getRule(Rule rule) {
        if(rule.getId()< 1L || rule.getOpenDate() == null)
            throw new RuntimeException("Ид или отчетная дата указаны неверно");

        String query = "SELECT ID, RULE, TITLE, OPEN_DATE, CLOSE_DATE FROM USCI_RULE.RULE_ rules\n" +
                " WHERE rules.ID = :ruleId" +
                "       AND rules.OPEN_DATE <= :reportDate \n" +
                "       AND (rules.CLOSE_DATE > :reportDate OR rules.CLOSE_DATE is null)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ruleId", rule.getId());
        params.addValue("reportDate", SqlJdbcConverter.convertToSqlDate(rule.getOpenDate()));

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query, params);

        return getRule(rows.iterator().next());
    }

    @Override
    @Transactional
    public void deleteRule(long ruleId, RulePackage rulePackage) {
        int count = npJdbcTemplate.update("delete from USCI_RULE.RULE_PACKAGE " +
                        " where PACKAGE_ID = :packageId\n " +
                        "   and RULE_ID = :ruleId",
                new MapSqlParameterSource("packageId", rulePackage.getId())
                                            .addValue("ruleId",ruleId));

        if (count != 1)
            throw new UsciException("Ошибка удаления из таблицы USCI_RULE.RULE_PACKAGE");

        int countR = npJdbcTemplate.update("delete from USCI_RULE.RULE_ " +
                        " where ID = :ruleId ",
                new MapSqlParameterSource("ruleId", ruleId));

        if (countR != 1)
            throw new UsciException("Ошибка удаления из таблицы USCI_RULE.RULE_");
    }

    @Override
    @Transactional
    public long createEmptyRule(final String title){
        KeyHolder keyHolder = new GeneratedKeyHolder();
        final String defaultRuleBody = "rule \" rule_" + (new Random().nextInt()) + "\"\nwhen\nthen\nend";
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO USCI_RULE.RULE_ (title, rule) " +
                    "VALUES(?, ?)", new String[]{"id"});
            ps.setString(1,title);
            ps.setString(2, defaultRuleBody);
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    @Transactional
    public void saveInPackage(Rule rule, PackageVersion packageVersion) {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName("USCI_RULE")
                .withTableName("RULE_PACKAGE")
                .usingGeneratedKeyColumns("ID");

        simpleJdbcInsert.executeAndReturnKey(new MapSqlParameterSource()
                .addValue("RULE_ID", rule.getId())
                .addValue("PACKAGE_ID", packageVersion.getRulePackage().getId()));
    }

    @Override
    public RulePackage getPackage(String name) {
        String query = "select * from USCI_RULE.PACKAGE_ WHERE NAME = :name";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("name", name);

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query, params);

        if (rows.size() < 1)
            throw new IllegalArgumentException("Отсутствует пакет: " + name);

        Map<String,Object> row = rows.iterator().next();

        RulePackage ret = new RulePackage();
        ret.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
        ret.setName(name);
        return ret;
    }

    @Override
    @Transactional
    public void updateBody(Rule rule) {
        int count = npJdbcTemplate.update("update USCI_RULE.RULE_\n" +
                        "  set RULE = :rule\n" +
                        " where ID = :id " +
                        "   and OPEN_DATE = :openDate",
                new MapSqlParameterSource("rule", rule.getRule())
                        .addValue("id", rule.getId())
                        .addValue("openDate", SqlJdbcConverter.convertToSqlDate(rule.getOpenDate())));

        if (count != 1)
            throw new RuntimeException("Ошибка update записи в таблице USCI_RULE.RULE_");

    }

    @Override
    @Transactional
    public long createRule(Rule rule, PackageVersion packageVersion) {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName("USCI_RULE")
                .withTableName("RULE_")
                .usingGeneratedKeyColumns("ID");

        Number id = simpleJdbcInsert.executeAndReturnKey(new MapSqlParameterSource()
                .addValue("TITLE", rule.getTitle())
                .addValue("RULE", rule.getRule())
                .addValue("OPEN_DATE", SqlJdbcConverter.convertToSqlDate(packageVersion.getReportDate())));

        rule.setId(id.longValue());

        return id.longValue();
    }

    @Override
    @Transactional
    public void renameRule(long ruleId, String title) {
        String sql = "update USCI_RULE.RULE_ set title = ? where id = ?";
        jdbcTemplate.update(sql, title, ruleId);
    }

    @Override
    @Transactional
    public void clearAllRules() {
        jdbcTemplate.update("delete from USCI_RULE.RULE_PACKAGE");

        jdbcTemplate.update("delete from USCI_RULE.RULE_");
    }

    @Override
    public List<PackageVersion> getPackageVersions(RulePackage rulePackage) {

        String query = "SELECT DISTINCT  RULES.OPEN_DATE \n"+
                " FROM (SELECT r.ID, \n"+
                "              r.RULE, \n"+
                "              r.TITLE, \n"+
                "              r.OPEN_DATE, \n"+
                "              r.CLOSE_DATE \n"+
                "         FROM USCI_RULE.RULE_ r,\n"+
                "             USCI_RULE.RULE_PACKAGE rulePkg \n"+
                "       where r.ID = rulePkg.RULE_ID       \n"+
                "         and rulePkg.PACKAGE_ID = :packageId) rules";


        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("packageId", rulePackage.getId());

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query, params);

        Set<LocalDate> dates = new HashSet<>();

        for (Map<String,Object> row: rows) {
            dates.add(SqlJdbcConverter.convertToLocalDate((Timestamp) row.get("OPEN_DATE")));
        }

        query = "SELECT DISTINCT  RULES.CLOSE_DATE \n" +
                "                 FROM (SELECT r.ID, \n" +
                "                              r.RULE, \n" +
                "                              r.TITLE, \n" +
                "                              r.OPEN_DATE, \n" +
                "                              r.CLOSE_DATE \n" +
                "                         FROM USCI_RULE.RULE_ r, \n" +
                "                             USCI_RULE.RULE_PACKAGE rulePkg \n" +
                "                       where r.ID = rulePkg.RULE_ID      \n" +
                "                         and rulePkg.PACKAGE_ID = :packageId and r.CLOSE_DATE is not null) rules";

        rows = npJdbcTemplate.queryForList(query, params);

        for(Map<String,Object> row: rows) {
            dates.add(SqlJdbcConverter.convertToLocalDate((Timestamp) row.get("CLOSE_DATE")));
        }

        List<PackageVersion> ret = new LinkedList<>();

        for(LocalDate date: dates) {
            ret.add(new PackageVersion(rulePackage, date));
        }

        Collections.sort(ret, new Comparator<PackageVersion>() {
            @Override
            public int compare(PackageVersion o1, PackageVersion o2) {
                return o1.getReportDate().compareTo(o2.getReportDate());
            }
        });

        return ret;
    }

    @Override
    public List<Rule> getRuleHistory(long ruleId) {
        String query = getRuleHistoryTable();
        query = query + " WHERE rules.ID = :ruleId";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ruleId",ruleId);

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query,params);
        List<Rule> ruleList = new ArrayList<>();

        long t = 1;
        for(Map<String,Object> row : rows) {
            Rule rule = getRule(row);
            rule.setId(t++ );
            ruleList.add(rule);
        }
        
        return ruleList;
    }

    @Override
    public List<RulePackage> getRulePackages(Rule rule) {
        String query = "select PACKAGE_ID from USCI_RULE.RULE_PACKAGE where RULE_ID = :ruleId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ruleId", rule.getId());

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query,params);

        List<RulePackage> rulePackageList = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            RulePackage rulePackage = new RulePackage();
            rulePackage.setId(SqlJdbcConverter.convertToLong(row.get("PACKAGE_ID")));
            rulePackageList.add(rulePackage);
        }

        return rulePackageList;
    }

    private Rule getRule(Map<String,Object> row){
        Rule rule = new Rule();
        rule.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
        rule.setRule((String) row.get("RULE"));
        rule.setOpenDate(SqlJdbcConverter.convertToLocalDate((Timestamp)  row.get("OPEN_DATE")));
        rule.setTitle((String) row.get("TITLE"));
        rule.setCloseDate(SqlJdbcConverter.convertToLocalDate((Timestamp)  row.get("CLOSE_DATE")));
        return rule;
    }

    private String getRuleHistoryTable(){
        return "SELECT RULES.* FROM (SELECT ID, RULE, TITLE, OPEN_DATE, CLOSE_DATE FROM USCI_RULE.RULE_\n"+
                                        "UNION ALL\n"+
                                        "SELECT RULE_ID as ID, RULE, TITLE, OPEN_DATE, CLOSE_DATE FROM USCI_RULE.RULE_HIS) rules\n";
    }

}
