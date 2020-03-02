package kz.bsbnb.usci.eav.service;

import kz.bsbnb.usci.eav.model.base.BaseEntity;

/**
 * @author Artur Tkachenko
 * @author Alexandr Motov
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

public interface BaseEntityProcessor {

    BaseEntity processBaseEntity(BaseEntity baseEntity);

    BaseEntity prepareBaseEntity(final BaseEntity baseEntity, Long respondentId);

}
