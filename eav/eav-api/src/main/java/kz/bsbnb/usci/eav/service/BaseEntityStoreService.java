package kz.bsbnb.usci.eav.service;

import kz.bsbnb.usci.eav.model.base.BaseEntity;

/**
 * @author Artur Tkachenko
 * @author Alexandr Motov
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

public interface BaseEntityStoreService {

    BaseEntity processBaseEntity(final BaseEntity baseEntitySaving, BaseEntity baseEntityLoaded, BaseEntityHolder baseEntityHolder);

    void storeInDb(BaseEntityHolder baseEntityHolder, final BaseEntity baseEntity);

    BaseEntity markBaseEntityAsDeleted(final BaseEntity baseEntity, BaseEntityHolder baseEntityHolder);

    BaseEntity closeBaseEntity(final BaseEntity baseEntity, BaseEntityHolder baseEntityHolder);

    BaseEntity openBaseEntity(final BaseEntity baseEntity, BaseEntityHolder baseEntityHolder);

}
