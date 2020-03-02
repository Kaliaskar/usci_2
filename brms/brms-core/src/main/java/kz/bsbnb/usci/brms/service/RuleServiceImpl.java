package kz.bsbnb.usci.brms.service;

import kz.bsbnb.usci.brms.RulesSingleton;
import kz.bsbnb.usci.brms.dao.RuleDao;
import kz.bsbnb.usci.brms.model.*;
import kz.bsbnb.usci.eav.model.base.BaseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Artur Tkachenko
 * @author Baurzhan Makhanbetov
 */

@Service
public class RuleServiceImpl implements RuleService {
    private static final Logger logger = LoggerFactory.getLogger(RuleServiceImpl.class);

    private final RuleDao ruleDao;
    private final RulesSingleton rulesSingleton;

    public RuleServiceImpl(RuleDao ruleDao, RulesSingleton rulesSingleton) {
        this.ruleDao = ruleDao;
        this.rulesSingleton = rulesSingleton;
    }

    @Override
    public long save(Rule rule, PackageVersion packageVersion) {
        return ruleDao.save(rule, packageVersion);
    }

    @Override
    public List<Rule> load(PackageVersion packageVersion) {
        return ruleDao.load(packageVersion);
    }

    @Override
    public void update(Rule rule) {
        ruleDao.update(rule);
    }

    @Override
    public RulePackage getPackage(String name) {
        return ruleDao.getPackage(name);
    }

    @Override
    public List<Rule> getAllRules() {
        return ruleDao.getAllRules();
    }

    @Override
    public Map getRuleTitles(Long packageId, LocalDate repDate) {
        Map m = new HashMap();
        m.put("data",ruleDao.getRuleTitles(packageId,repDate));
        return m;
    }

    @Override
    public Map getRuleTitles(Long packageId, LocalDate reportDate, String searchText) {
        if(searchText == null || searchText.length() < 1)
            throw new IllegalArgumentException("Ключ не задан\\поисковой ключ не задан");

        Map m = new HashMap();
        m.put("data",ruleDao.getRuleTitles(packageId, reportDate, searchText));
        return m;
    }

    @Override
    public Rule getRule(Rule rule) {
        return ruleDao.getRule(rule);
    }

    @Override
    public void deleteRule(long ruleId, RulePackage rulePackage) {
        ruleDao.deleteRule(ruleId, rulePackage);
    }

    @Override
    public void updateBody(Rule rule) {
        ruleDao.updateBody(rule);
    }

    @Override
    public void createNewRuleInPackage(Rule rule, PackageVersion packageVersion) {
        long ruleId = ruleDao.createRule(rule, packageVersion);
        rule.setId(ruleId);
        ruleDao.saveInPackage(rule, packageVersion);
    }

    @Override
    public void renameRule(long ruleId, String title) {
        ruleDao.renameRule(ruleId,title);
    }

    @Override
    public String reloadCache() {
        rulesSingleton.fillPackagesCache();
        if (rulesSingleton.rulePackageErrors.size() > 0) {
            String errors = "";
            for (RulePackageError error : rulesSingleton.rulePackageErrors) {
                errors += error.getErrorMsg() + "\n";
            }
            return errors;
        }
        return null;
    }

    @Override
    public void runRules(BaseEntity entity, String pkgName, LocalDate repDate) {
        rulesSingleton.runRules(entity,pkgName,repDate);
    }

    @Override
    public String getRuleErrors(String rule) {
        return rulesSingleton.getRuleErrors(rule);
    }

    @Override
    public String getPackageErrorsOnRuleUpdate(Rule rule, PackageVersion packageVersion) {
        return rulesSingleton.getPackageErrorsOnRuleUpdate(rule, packageVersion);
    }

    @Override
    public void clearAllRules() {
        ruleDao.clearAllRules();
    }

    @Override
    public List<Pair> getPackageVersions(RulePackage rulePackage) {
        List<PackageVersion> versions = ruleDao.getPackageVersions(rulePackage);

        List<Pair> ret = new LinkedList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        long id = 1L;
        for(PackageVersion packageVersion: versions) {
            Pair p = new Pair(id++, packageVersion.getReportDate().format(formatter));
            ret.add(p);
        }

        return ret;
    }

    @Override
    public String getPackageErrorsOnRuleInsert(PackageVersion packageVersion, String title, String ruleBody) {
        return rulesSingleton.getPackageErrorsOnRuleInsert(packageVersion, title, ruleBody);
    }

    @Override
    public List<Rule> getRuleHistory(long ruleId) {
        return ruleDao.getRuleHistory(ruleId);
    }

    @Override
    public String getPackageErrorsOnRuleDelete(Rule rule) {
        return rulesSingleton.getPackageErrorsOnRuleDelete(rule);
    }
}
