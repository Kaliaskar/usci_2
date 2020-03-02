package kz.bsbnb.usci.util.controller;

import kz.bsbnb.usci.model.util.Application;
import kz.bsbnb.usci.util.service.ApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Jandos Iskakov
 */

@RestController
@RequestMapping(value = "/config")
public class ApplicationController {
    private ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping(value = "/getApplicationList")
    public List<Application> getApplicationList() {
        return applicationService.getAppList();
    }

}
