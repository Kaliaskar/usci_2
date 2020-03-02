package kz.bsbnb.usci.eav.dao;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseEntityRegistry;

import java.util.List;
import java.util.Optional;

/**
 * @author Jandos Iskakov
 */

public interface BaseEntityRegistryDao {

    void insert(List<BaseEntityRegistry> baseEntityRegistries);

    Optional<BaseEntityRegistry> find(BaseEntity baseEntity);

    List<BaseEntityRegistry> getBaseEntitiesByMetaClass(Long metaClassId);

}
