package kz.bsbnb.usci.sync.service;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.core.EavHub;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author vitominn
 */

public interface BaseEntityHubService {

    BaseEntity prepareBaseEntity(final BaseEntity baseEntity, Long respondentId);

}
