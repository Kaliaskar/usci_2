package kz.bsbnb.usci.brms.controller;

import kz.bsbnb.usci.brms.model.RulePackage;
import kz.bsbnb.usci.brms.service.PackageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/package")
public class PackageController {
    private final PackageService packageService;

    public PackageController(PackageService packageService) {
        this.packageService = packageService;
    }

    @GetMapping(value = "getAllPackages")
    public List<RulePackage> getAllPackages() {
        return packageService.getAllPackages();
    }

    @GetMapping(value = "savePackage")
    public Long savePackage(@RequestParam(name = "rulePackageName") String rulePackageName) {
        return packageService.savePackage(rulePackageName);
    }



}
