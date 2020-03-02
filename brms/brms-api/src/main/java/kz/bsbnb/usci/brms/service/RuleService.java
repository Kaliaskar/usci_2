package kz.bsbnb.usci.brms.service;

import kz.bsbnb.usci.brms.model.PackageVersion;
import kz.bsbnb.usci.brms.model.Pair;
import kz.bsbnb.usci.brms.model.Rule;
import kz.bsbnb.usci.brms.model.RulePackage;
import kz.bsbnb.usci.eav.model.base.BaseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface RuleService {

    /*void checkParserRules(BaseEntity baseEntity, String packageType);*/

    long save(Rule rule, PackageVersion packageVersion);

    List<Rule> load(PackageVersion packageVersion);

    void update(Rule rule);

    RulePackage getPackage(String name);

    List<Pair> getPackageVersions(RulePackage rulePackage);

    /**
     * Select * from rules
     *
     * @return list of rules
     */
    List<Rule> getAllRules();

    /**
     * Retrieves rules(<i>id</i>, <i>title <b>as name</b></i>) and <b>batchVersionId</b> given <b>packageId</b> and <b>date</b>
     * Automatically defines its date (i.e the latest version of batch among all dates less or equal <b>repDate</b>)
     *
     * @return map of : <b>data</b> - array of rules <b>batchVersionId</b> - batchVersionId
     */
    Map getRuleTitles(Long packageId, LocalDate repDate);

    Map getRuleTitles(Long packageId, LocalDate repDate, String searchText);

    /**
     * Retrieves single rule by ruleId
     *
     * @param rule
     * @return
     */
    Rule getRule(Rule rule);


    void deleteRule(long ruleId, RulePackage rulePackage);

    /**
     * Creates new rule with given title and empty body <br/>
     *
     * @param title
     * @param packageVersionId
     * @return generated id of rule
     */


    /**
     * Sets new body to rule with given <b>ruleId</b>
     *
     * @param rule - rule id
     */
    void updateBody(Rule rule);

    /**
     * Create new rule with given title and body into batchVersion
     *
     *
     */
    void createNewRuleInPackage(Rule rule, PackageVersion packageVersion);

    void renameRule(long ruleId, String title);

    /**
     * ============================
     * RuleSingleton interface
     * ============================
     */
    String reloadCache();

    void runRules(BaseEntity entity, String pkgName, LocalDate repDate);

    String getRuleErrors(String rule);

    String getPackageErrorsOnRuleUpdate(Rule rule, PackageVersion packageVersion);

    String getPackageErrorsOnRuleInsert(PackageVersion packageVersion, String title, String ruleBody);

    List<Rule> getRuleHistory(long ruleId);

    String getPackageErrorsOnRuleDelete(Rule rule);

    /**
     * =============================
     * Developer tools
     * =============================
     */
    void clearAllRules();
}
