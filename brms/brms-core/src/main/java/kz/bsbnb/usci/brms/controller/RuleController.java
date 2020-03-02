package kz.bsbnb.usci.brms.controller;

import kz.bsbnb.usci.brms.model.PackageVersion;
import kz.bsbnb.usci.brms.model.Pair;
import kz.bsbnb.usci.brms.model.Rule;
import kz.bsbnb.usci.brms.model.RulePackage;
import kz.bsbnb.usci.brms.service.RuleService;
import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.eav.repository.MetaClassRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/rule")
public class RuleController {

    private final RuleService ruleService;
    private final MetaClassRepository metaClassRepository;

    @Autowired
    public RuleController(RuleService ruleService, MetaClassRepository metaClassRepository) {
        this.ruleService = ruleService;
        this.metaClassRepository = metaClassRepository;
    }

    @GetMapping(value = "getPackageVersions")
    public List<Pair> getPackageVersions(@RequestParam Long packageId){
        RulePackage rulePackage = new RulePackage();
        rulePackage.setId(packageId);
        return ruleService.getPackageVersions(rulePackage);
    }

    @GetMapping(value = "getRuleTitles")
    public Map getRuleTitles(@RequestParam Long packageId,
                             @RequestParam  @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate date,
                             @RequestParam String searchText){
        if (searchText != null && searchText.length() > 0)
            return ruleService.getRuleTitles(packageId, date, searchText);
        else
            return ruleService.getRuleTitles(packageId, date);
    }

    @GetMapping(value = "getRule")
    public Rule getRule(@RequestParam Long ruleId,
                        @RequestParam  @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate date){
        Rule rule = new Rule();
        rule.setId(ruleId);
        rule.setOpenDate(date);
        return ruleService.getRule(rule);
    }

    @PutMapping(value = "updateRule")
    public void updateRule(@RequestParam Long packageId,
                           @RequestParam  @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate date,
                           @RequestParam String pkgName,
                           @RequestParam Long ruleId,
                           @RequestParam String ruleBody){
        PackageVersion packageVersion = new PackageVersion(new RulePackage(packageId, pkgName), date);
        Rule rule = new Rule("", ruleBody, date);
        rule.setId(ruleId);
        String errors = ruleService.getPackageErrorsOnRuleUpdate(rule, packageVersion);
        if(errors != null)
            throw new RuntimeException(errors);
        ruleService.updateBody(rule);
    }

    @PostMapping(value = "deleteRule")
    public void deleteRule (@RequestParam Long ruleId,
                            @RequestParam Long packageId,
                            @RequestParam String pkgName,
                            @RequestParam @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate date) {
        String errors = ruleService.getPackageErrorsOnRuleDelete(new Rule(ruleId, date));
        if(errors != null)
            throw new RuntimeException(errors);
        ruleService.deleteRule(ruleId, new RulePackage(packageId, pkgName));
    }

    @PutMapping(value = "createRule")
    public void createRule(@RequestParam Long packageId,
                           @RequestParam  @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate date,
                           @RequestParam String pkgName,
                           @RequestParam String title,
                           @RequestParam String ruleBody) {
        String errors = ruleService.getPackageErrorsOnRuleInsert(
                new PackageVersion(new RulePackage(packageId, pkgName), date),
                title,
                ruleBody);
        if (errors != null)
            throw new RuntimeException(errors);
        ruleService.createNewRuleInPackage(
                new Rule(title, ruleBody),
                new PackageVersion(new RulePackage(packageId, pkgName), date));

    }

    @PostMapping(value = "reloadCache")
    public String reloadCache() {
        return ruleService.reloadCache();
    }

    @PutMapping(value = "renameRule")
    public void renameRule(@RequestParam Long ruleId, @RequestParam String title){
        ruleService.renameRule(ruleId,title);
    }

    @GetMapping(value = "getRuleHistory")
    public List<Rule> getRuleHistory(@RequestParam Long ruleId) {
        return ruleService.getRuleHistory(ruleId);
    }

    @PostMapping(value = "runRules")
    public void runRules(@RequestParam String packageName,
                         @RequestParam  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate repDate,
                         @RequestParam Long baseEntityId,
                         @RequestParam String metaClassName){
        MetaClass metaClass = metaClassRepository.getMetaClass(metaClassName);
        BaseEntity baseEntity  = new BaseEntity(baseEntityId, metaClass, 0L);
        ruleService.runRules(baseEntity, packageName, repDate);
    }

}
