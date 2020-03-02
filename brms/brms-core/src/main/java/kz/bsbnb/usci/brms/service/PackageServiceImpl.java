package kz.bsbnb.usci.brms.service;

import kz.bsbnb.usci.brms.dao.PackageDao;
import kz.bsbnb.usci.brms.model.RulePackage;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Kanat Tulbassiev
 */

@Service
public class PackageServiceImpl implements PackageService {
    private final PackageDao packageDao;

    public PackageServiceImpl(PackageDao packageDao) {
        super();
        this.packageDao = packageDao;
    }

    @Override
    public long savePackage(String rulePackageName) {
        return packageDao.savePackage(rulePackageName);
    }

    @Override
    public RulePackage load(long id) {
        return packageDao.loadBatch(id);
    }

    @Override
    public List<RulePackage> getAllPackages() {
        return packageDao.getAllPackages();
    }

}
