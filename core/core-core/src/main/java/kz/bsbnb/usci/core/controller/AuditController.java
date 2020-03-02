package kz.bsbnb.usci.core.controller;

import kz.bsbnb.usci.core.service.AuditService;
import kz.bsbnb.usci.core.service.UserService;
import kz.bsbnb.usci.util.json.ext.ExtJsList;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/audit")
public class AuditController {
    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping(value = "/getAudit")
    public ExtJsList getAudit(@RequestParam(name = "userId") Long userId) {
        return new ExtJsList(auditService.getAudit(userId));
    }
    @GetMapping(value = "/getAuditJson")
    public ExtJsList getAuditJson(@RequestParam(name = "userId") Long userId) {
        return new ExtJsList(auditService.getAuditJson(userId));
    }
}
