package kz.bsbnb.usci.eav.controller;

import kz.bsbnb.usci.eav.model.base.BaseEntityJson;
import kz.bsbnb.usci.eav.model.core.BaseEntityStatus;
import kz.bsbnb.usci.eav.model.meta.json.EntityExtJsTreeJson;
import kz.bsbnb.usci.eav.service.BaseEntityApprovalService;
import kz.bsbnb.usci.eav.service.BaseEntityLoadXmlService;
import kz.bsbnb.usci.eav.service.EntityExtJsServiceImpl;
import kz.bsbnb.usci.eav.service.EntityService;
import kz.bsbnb.usci.util.json.ext.ExtJsList;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * @author Jandos Iskakov
 */

@RestController
@RequestMapping(value = "/eav")
public class EntityController {
    private final EntityService entityService;
    private final EntityExtJsServiceImpl entityExtJsService;
    private final BaseEntityApprovalService approvalService;
    private final BaseEntityLoadXmlService baseEntityLoadXmlService;


    public EntityController(EntityService entityService,
                            EntityExtJsServiceImpl entityExtJsService,
                            BaseEntityApprovalService approvalService,
                            BaseEntityLoadXmlService baseEntityLoadXmlService) {
        this.entityService = entityService;
        this.entityExtJsService = entityExtJsService;
        this.approvalService = approvalService;
        this.baseEntityLoadXmlService = baseEntityLoadXmlService;
    }

    @PutMapping(value = "/entity/addEntityStatus")
    public Long addEntityStatus(@RequestBody BaseEntityStatus entityStatus) {
        return entityService.addEntityStatus(entityStatus);
    }

    @GetMapping(value = "getEntityData")
    public EntityExtJsTreeJson getEntityData(@RequestParam(name = "entityId") Long entityId,
                                             @RequestParam(name = "metaClassId") Long metaClassId,
                                             @RequestParam(name = "respondentId") Long respondentId,
                                             @RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                             @RequestParam(name = "asRoot") boolean asRoot) {
        return entityExtJsService.getEntityData(entityId, metaClassId, respondentId, date, asRoot);
    }

    @GetMapping(value = "getEntityDataMaintenance")
    public EntityExtJsTreeJson getEntityDataMaintenance(@RequestParam(name = "id") Long id) {
        return entityExtJsService.getEntityDataMaintenance(id);
    }

    @GetMapping(value = "getEntityListMaintenance")
    public ExtJsList getEntityListMaintenance(@RequestParam(name = "batchId") Long batchId) {
        return entityExtJsService.getEntityListMaintenance(batchId);
    }

    @PostMapping(value = "searchEntity")
    public EntityExtJsTreeJson searchEntity(@RequestParam(name = "respondentId") Long respondentId,
                                            @RequestParam(name = "metaClassId") Long metaClassId,
                                            @RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                            @RequestBody EntityExtJsTreeJson rootNode) {
        return entityExtJsService.searchEntity(respondentId, metaClassId, date, rootNode);
    }

    @PostMapping(value = "approveEntityMaintenance")
    public void approveEntityMaintenance(@RequestParam(name = "batchId") Long batchId,
                                         @RequestBody List<BaseEntityJson> baseEntityJsonList) {
        approvalService.approveEntityMaintenance(baseEntityJsonList, batchId);
    }

    @PostMapping(value = "updateBaseEntityState")
    public void updateBaseEntityState(@RequestBody BaseEntityJson baseEntityJson) {
        baseEntityLoadXmlService.updateBaseEntityState(baseEntityJson);
    }

}
