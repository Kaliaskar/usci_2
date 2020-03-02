package kz.bsbnb.usci.eav.service;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseEntityJson;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.eav.model.meta.json.EntityExtJsTreeJson;

import java.time.LocalDate;
import java.util.List;


public interface BaseEntityLoadXmlService {

    List<BaseEntityJson> loadEntityForApproval(long batchId);

    EntityExtJsTreeJson loadBaseEntity(Long id);

    void updateBaseEntity(BaseEntityJson baseEntityJson, boolean approve);

    void updateBaseEntityState(BaseEntityJson baseEntityJson);

    void deleteBaseEntity(Long id);

    BaseEntity getBaseEntityFromJsonTree(Long respondentId, LocalDate reportDate, EntityExtJsTreeJson node, MetaClass metaClass, Long batchId);
}
