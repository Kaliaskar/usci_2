package kz.bsbnb.usci.util.controller;

import kz.bsbnb.usci.util.service.ConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * @author Jandos Iskakov
 */

@RestController
@RequestMapping(value = "/config")
public class ConfigController {
    private ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping(value = "/getDigitalSigningOrgIds")
    public Set<Long> getDigitalSigningOrgIds() {
        return configService.getDigitalSigningOrgIds();
    }

    @GetMapping(value = "/getQueueAlgorithm")
    public String getQueueAlgorithm() {
        return configService.getConfig("RECEIVER", "QUEUE_ALGO").getValue();
    }

    @GetMapping(value = "/getPriorityRespondentIds")
    public Set<Long> getPriorityRespondentIds() {
        return configService.getPriorityRespondentIds();
    }

    @PostMapping(value = "/updateQueueConfig")
    public void setPriorityRespondentIds(@RequestParam(name = "priorityRespondentIds") List<Long> priorityRespondentIds,
                                         @RequestParam(name = "queueAlgorithm") String queueAlgorithm) {
        configService.setPriorityRespondentIds(priorityRespondentIds);
        configService.setQueueAlgorithm(queueAlgorithm);
    }

    @GetMapping(value = "/getManifestXsd")
    public byte[] getManifestXsd() {
        return configService.getManifestXsd();
    }

    @PostMapping(value = "/updateDigitalSigningOrgIds")
    public void updateDigitalSigningOrgIds(@RequestParam(name = "digitalSigningOrgIds") List<Long> digitalSigningOrgIds) {
        configService.setDigitalSigningOrgIds(digitalSigningOrgIds);
    }

    @GetMapping(value = "/getConfirmText")
    public String getConfirmText() {
        return configService.getConfirmText();
    }


}
