package kz.bsbnb.usci.sync.service;

import kz.bsbnb.usci.eav.model.base.BaseEntity;

/**
 * @author Kanat Tulbassiev
 */

public interface EntityService {

    boolean process(BaseEntity entity);

    Long find(BaseEntity baseEntity);


}
