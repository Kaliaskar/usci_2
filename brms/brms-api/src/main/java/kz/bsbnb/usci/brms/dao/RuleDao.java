package kz.bsbnb.usci.brms.dao;

import kz.bsbnb.usci.brms.model.PackageVersion;
import kz.bsbnb.usci.brms.model.Rule;
import kz.bsbnb.usci.brms.model.RulePackage;
import kz.bsbnb.usci.brms.model.SimpleTrack;

import java.time.LocalDate;
import java.util.List;

/**
 * @author Aibek Bukabaev
 */

public interface RuleDao {
    long save(Rule rule, PackageVersion packageVersion);

    List<Rule> load(PackageVersion packageVersion);

    long update(Rule rule);

    List<Rule> getAllRules();

    List<SimpleTrack> getRuleTitles(Long packageId, LocalDate repDate);

    Rule getRule(Rule rule);

    void deleteRule(long ruleId, RulePackage rulePackage);

    long createEmptyRule(String title);

    void updateBody(Rule rule);

    long createRule(Rule rule, PackageVersion packageVersion);

    void renameRule(long ruleId, String title);

    void clearAllRules();

    List<SimpleTrack> getRuleTitles(Long batchVersionId, LocalDate reportDate, String searchText);

    List<PackageVersion> getPackageVersions(RulePackage rulePackage);

    void saveInPackage(Rule rule, PackageVersion packageVersion);

    RulePackage getPackage(String name);

    List<Rule> getRuleHistory(long ruleId);

    List<RulePackage> getRulePackages(Rule rule);
}
