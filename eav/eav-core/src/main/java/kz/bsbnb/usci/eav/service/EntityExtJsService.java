package kz.bsbnb.usci.eav.service;

import kz.bsbnb.usci.eav.model.base.BaseEntityJson;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.eav.model.meta.json.EntityExtJsTreeJson;
import kz.bsbnb.usci.util.json.ext.ExtJsList;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @author Jandos Iskakov
 */
public interface EntityExtJsService {

    EntityExtJsTreeJson searchEntity(Long respondentId, Long metaClassId, LocalDate reportDate, EntityExtJsTreeJson rootNode);

    EntityExtJsTreeJson getEntityData(Long entityId, Long metaClassId, Long respondentId, LocalDate reportDate, boolean asRoot);

    List<Map<String, Object>> loadEntityEntries(MetaClass metaClass, LocalDate reportDate, Long userId, boolean isNb);

    byte[] exportDictionaryToMsExcel(MetaClass metaClass, LocalDate reportDate, Long userId, boolean isNb);

    EntityExtJsTreeJson getEntityDataMaintenance(Long id);

    ExtJsList getEntityListMaintenance(Long batchId);

}
