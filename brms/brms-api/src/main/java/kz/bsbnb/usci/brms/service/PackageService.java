package kz.bsbnb.usci.brms.service;

import kz.bsbnb.usci.brms.model.RulePackage;

import java.util.List;

public interface PackageService {
    long savePackage(String rulePackageName);

    RulePackage load(long id);

    List<RulePackage> getAllPackages();
}
