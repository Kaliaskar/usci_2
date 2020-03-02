package kz.bsbnb.usci.brms.dao;

import kz.bsbnb.usci.brms.model.RulePackage;

import java.util.List;

/**
 * @author Aibek Bukabaev
 */

public interface PackageDao {
    RulePackage loadBatch(long id);

    long savePackage(String rulePackageName);

    List<RulePackage> getAllPackages();
}
