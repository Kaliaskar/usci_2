package kz.bsbnb.usci.eav.service;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.meta.MetaClass;

import java.time.LocalDate;
import java.util.List;

/**
 * @author Jandos Iskakov
 */

public interface BaseEntityLoadService {

    BaseEntity loadByMaxReportDate(BaseEntity baseEntity, LocalDate savingReportDate);

    BaseEntity loadBaseEntity(BaseEntity baseEntity, LocalDate savingReportDate);

    BaseEntity loadBaseEntity(BaseEntity baseEntity);

    List<BaseEntity> loadBaseEntityEntries(BaseEntity baseEntity, LocalDate reportDate);

    List<BaseEntity> loadBaseEntitiesByMetaClass(MetaClass metaClass);

}
