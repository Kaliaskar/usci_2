package kz.bsbnb.usci.eav.service;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseEntityRegistry;
import kz.bsbnb.usci.eav.model.core.BaseEntityStatus;

import java.time.LocalDate;
import java.util.List;

public interface EntityService {

    boolean existsBaseEntity(BaseEntity baseEntity, LocalDate reportDate);

    long countBaseEntityEntries(BaseEntity baseEntity);

    boolean hasReference(BaseEntity baseEntity);

    void insert(List<BaseEntityRegistry> infos);

    Long addEntityStatus(BaseEntityStatus entityStatus);

}
