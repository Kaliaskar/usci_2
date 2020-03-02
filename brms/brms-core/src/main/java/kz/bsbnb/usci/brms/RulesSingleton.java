package kz.bsbnb.usci.brms;

import kz.bsbnb.usci.brms.dao.PackageDao;
import kz.bsbnb.usci.brms.dao.RuleDao;
import kz.bsbnb.usci.brms.exception.BrmsException;
import kz.bsbnb.usci.brms.model.PackageVersion;
import kz.bsbnb.usci.brms.model.Rule;
import kz.bsbnb.usci.brms.model.RulePackage;
import kz.bsbnb.usci.brms.model.RulePackageError;
import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.repository.MetaClassRepository;
import kz.bsbnb.usci.eav.service.BaseEntityLoadService;
import kz.bsbnb.usci.eav.service.BaseEntityProcessor;
import kz.bsbnb.usci.model.exception.UsciException;
import org.drools.core.command.runtime.rule.FireAllRulesCommand;
import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.impl.KnowledgeBaseFactory;
import org.kie.api.KieBase;
import org.kie.api.command.Command;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.api.runtime.rule.AgendaFilter;
import org.kie.api.runtime.rule.Match;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.command.CommandFactory;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Artur Tkachenko
 * @author Baurzhan Makhanbetov
 */

@Component
public class RulesSingleton {
    private final Logger logger = LoggerFactory.getLogger(RulesSingleton.class);

    private KieBase kieBase;
    private InternalKnowledgeBase kbase;

    public static final DateTimeFormatter ruleDateFormat = DateTimeFormatter.ofPattern("dd_MM_yyyy");

    private final PackageDao packageDao;

    private BaseEntityProcessor baseEntityProcessor;

    private BaseEntityLoadService baseEntityLoadService;

    private MetaClassRepository metaClassRepository;

    protected Map<String, BaseEntity> creditorCache;

    private class RuleCacheEntry implements Comparable {
        private LocalDate repDate;
        private String rules;

        private RuleCacheEntry(LocalDate repDate, String rules) {
            this.repDate = repDate;
            this.rules = rules;
        }

        @Override
        public int compareTo(Object obj) {
            if (obj == null)
                return 0;
            if (!(getClass().equals(obj.getClass())))
                return 0;

            return (repDate.compareTo(((RuleCacheEntry) obj).getRepDate()));
        }

        private LocalDate getRepDate() {
            return repDate;
        }

        private void setRepDate(LocalDate repDate) {
            this.repDate = repDate;
        }

        private String getRules() {
            return rules;
        }

        private void setRules(String rules) {
            this.rules = rules;
        }
    }

    private HashMap<String, ArrayList<RuleCacheEntry>> ruleCache = new HashMap<>();

    public ArrayList<RulePackageError> rulePackageErrors = new ArrayList<>();

    private RuleDao ruleDao;

    public StatelessKieSession getSession() {
        return kieBase.newStatelessKieSession();
    }

    public RulesSingleton(PackageDao packageDao,
                          BaseEntityProcessor baseEntityProcessor,
                          BaseEntityLoadService baseEntityLoadService,
                          MetaClassRepository metaClassRepository,
                          RuleDao ruleDao) {
        this.packageDao = packageDao;
        this.baseEntityProcessor = baseEntityProcessor;
        this.baseEntityLoadService = baseEntityLoadService;
        this.metaClassRepository = metaClassRepository;
        this.ruleDao = ruleDao;
        creditorCache = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {
        reloadCache();
    }

    public void reloadCache() {
        fillPackagesCache();
    }

    public void setRules(String rules) {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add(ResourceFactory.newInputStreamResource(new ByteArrayInputStream(rules.getBytes())), ResourceType.DRL);

        if (kbuilder.hasErrors()) {
            throw new IllegalArgumentException(kbuilder.getErrors().toString());
        }

        kbase.addPackages(kbuilder.getKnowledgePackages());
    }

    public String getRuleErrors(String rule) {
        String packages = "";
        packages += "package test \n";
        packages += "dialect \"mvel\"\n";
        packages += "import kz.bsbnb.usci.eav.model.base.BaseEntity;\n";
        packages += "import kz.bsbnb.usci.brms.BRMSHelper;\n";

        rule = packages + rule;

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add(ResourceFactory.newInputStreamResource(new ByteArrayInputStream(rule.getBytes())), ResourceType.DRL);

        if (kbuilder.hasErrors()) {
            return kbuilder.getErrors().toString();
        }

        return null;
    }

    public String getPackageErrorsOnRuleUpdate(Rule rule, PackageVersion packageVersion) {
        List<Rule> rules = ruleDao.load(packageVersion);

        String packages = "";

        packages += "package test\n";
        packages += "dialect \"mvel\"\n";
        packages += "import kz.bsbnb.usci.eav.model.base.BaseEntity;\n";
        packages += "import kz.bsbnb.usci.brms.BRMSHelper;\n";

        for (Rule r : rules) {
            if (!r.getId().equals(rule.getId()))
                packages += r.getRule() + "\n";
            else {
                packages += rule.getRule() + "\n";
            }
        }

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add(ResourceFactory.newInputStreamResource(new ByteArrayInputStream(packages.getBytes())), ResourceType.DRL);

        if (kbuilder.hasErrors()) {
            return kbuilder.getErrors().toString();
        }

        return null;
    }

    private class PackageAgendaFilter implements AgendaFilter {
        private String pkgName = "";

        public PackageAgendaFilter(String pkgName) {
            this.pkgName = pkgName.trim();
        }

        @Override
        public boolean accept(Match match) {
            return pkgName.equals(match.getRule().getPackageName());
        }
    }

    public void runRules(BaseEntity entity, String pkgName) {
        runRules(entity, pkgName, LocalDate.now());
    }

    synchronized public void fillPackagesCache() {
        kbase = KnowledgeBaseFactory.newKnowledgeBase();
        List<RulePackage> packages = packageDao.getAllPackages();

        rulePackageErrors.clear();
        ruleCache.clear();
        creditorCache.clear();

        for (RulePackage curPackage : packages) {
            List<PackageVersion> versions = ruleDao.getPackageVersions(curPackage);

            ArrayList<RuleCacheEntry> ruleCacheEntries = new ArrayList<>();

            for (PackageVersion version : versions) {
                List<Rule> rules = ruleDao.load(version);

                StringBuilder droolPackage = new StringBuilder();

                droolPackage.append("package ").append(curPackage.getName()).append("_")
                        .append(version.getReportDate().format(ruleDateFormat)).append("\n");

                droolPackage.append("dialect \"mvel\"\n");
                droolPackage.append("import kz.bsbnb.usci.brms.BRMSHelper;\n");

                for (Rule r : rules) {
                    if (r.isActive() || true)
                        droolPackage.append(r.getRule()).append("\n");
                }

                logger.debug(droolPackage.toString());
                try {
                    setRules(droolPackage.toString());
                } catch (Exception e) {
                    rulePackageErrors.add(new RulePackageError(curPackage.getName() + "_" + version,
                            e.getMessage()));
                    logger.error("Ошибка компиляции пакета бизнес-правил : " + curPackage.getName() + "\n" + e.getMessage() );
                    //throw new UsciException("Ошибка компиляции пакета бизнес-правил : " + curPackage.getName(), e);
                }

                ruleCacheEntries.add(new RuleCacheEntry(version.getReportDate(),
                        curPackage.getName() + "_" + version));
            }

            Collections.sort(ruleCacheEntries);
            ruleCache.put(curPackage.getName(), ruleCacheEntries);
        }
    }

    public void runRules(BaseEntity entity, String pkgName, LocalDate repDate) {
        kieBase = kbase;
        StatelessKieSession kSession = getSession();

        kSession.setGlobal("baseEntityProcessor", baseEntityProcessor);
        kSession.setGlobal("baseEntityLoadService", baseEntityLoadService);
        kSession.setGlobal("metaClassRepository", metaClassRepository);
        kSession.setGlobal("creditorCache", creditorCache);

        ArrayList<RuleCacheEntry> versions = ruleCache.get(pkgName);
        String packageName = null;
        if (versions == null) {
            try {
                logger.info("В данный момент идет обновление кэша бизнес - правил");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
        }
        versions = ruleCache.get(pkgName);
        for (RuleCacheEntry version : versions) {
            if (version.getRepDate().compareTo(repDate) <= 0) {
                packageName = pkgName + "_" + version.getRepDate().format(ruleDateFormat);
            } else {
                break;
            }
        }

        if (packageName == null)
            return;

        @SuppressWarnings("rawtypes")
        List<Command> commands = new ArrayList<>();
        commands.add(CommandFactory.newInsert(entity));
        commands.add(new FireAllRulesCommand(new PackageAgendaFilter(packageName)));
        kSession.execute(CommandFactory.newBatchExecution(commands));

    }

    public void runRules(BaseEntity entity) {
        StatelessKieSession kSession = getSession();

        kSession.execute(entity);
    }

    public String getPackageErrorsOnRuleInsert(PackageVersion packageVersion, String title, String ruleBody) {
        List<Rule> rules = ruleDao.load(packageVersion);

        String packages = "";

        packages += "package test\n";
        packages += "dialect \"mvel\"\n";
        packages += "import kz.bsbnb.usci.eav.model.base.BaseEntity;\n";
        packages += "import kz.bsbnb.usci.brms.BRMSHelper;\n";

        for (Rule r : rules)
            packages += r.getRule() + "\n";

        packages += ruleBody + "\n";


        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add(ResourceFactory.newInputStreamResource(new ByteArrayInputStream(packages.getBytes())), ResourceType.DRL);

        if (kbuilder.hasErrors()) {
            return kbuilder.getErrors().toString();
        }

        return null;
    }

    public String getPackageErrors(List<Rule> rules) {
        StringBuilder packages = new StringBuilder();

        packages.append("package test\n");
        packages.append("dialect \"mvel\"\n");
        packages.append("import kz.bsbnb.usci.eav.model.base.BaseEntity;\n");
        packages.append("import kz.bsbnb.usci.brms.BRMSHelper;\n");

        for (Rule r : rules)
            packages.append(r.getRule()).append("\n");


        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add(ResourceFactory.newInputStreamResource(new ByteArrayInputStream(packages.toString().getBytes())), ResourceType.DRL);

        if (kbuilder.hasErrors()) {
            return kbuilder.getErrors().toString();
        }

        return null;
    }


    public String getPackageErrorsOnRuleDelete(Rule rule) {
        List<RulePackage> rulePackages = ruleDao.getRulePackages(rule);

        for (RulePackage rulePackage : rulePackages) {
            List<PackageVersion> packageVersions = ruleDao.getPackageVersions(rulePackage);

            for (PackageVersion packageVersion : packageVersions) {
                if (packageVersion.getReportDate().compareTo(rule.getOpenDate()) > 0)
                    continue;

                List<Rule> rules = ruleDao.load(packageVersion);

                Iterator<Rule> iterator = rules.iterator();

                while(iterator.hasNext()) {
                    if (iterator.next().getId().equals(rule.getId())) {
                        iterator.remove();
                        break;
                    }
                }

                String curResult = getPackageErrors(rules);

                if (curResult != null)
                    return curResult;
            }
        }

        return null;
    }
}
