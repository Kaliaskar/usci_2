package kz.bsbnb.usci.eav.dao;

import kz.bsbnb.usci.eav.model.base.BaseEntityJson;
import kz.bsbnb.usci.eav.model.meta.json.EntityExtJsTreeJson;

import java.util.List;

public interface BaseEntityLoadXmlDao {

    List<BaseEntityJson> loadEntityForApproval(long batchId);

    EntityExtJsTreeJson loadBaseEntity(Long id);

    void updateBaseEntityState(BaseEntityJson baseEntityJson);

    void updateBaseEntity(BaseEntityJson baseEntityJson, boolean approve);

    void deleteBaseEntity(Long id);
}
