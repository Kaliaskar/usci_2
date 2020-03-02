package kz.bsbnb.usci.eav.service;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.core.EavHub;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Jandos Iskakov
 */

public interface BaseEntityHubService {

    Map<Short, List<String>> getKeys(final BaseEntity baseEntity);

    void insert(List<BaseEntity> baseEntity);

    void add(BaseEntity baseEntity, List<String> oldKeys);

    Optional<Long> find(BaseEntity baseEntity);

    void deleteAll(EavHub eavHub);

    void update(EavHub eavHub);

}
