package kz.bsbnb.usci.eav.dao;

import kz.bsbnb.usci.eav.model.core.EavHub;

import java.util.List;
import java.util.Optional;

/**
 * @author Jandos Iskakov
 */

public interface BaseEntityHubDao {

    void insert(List<EavHub> hubs);

    Optional<Long> find(EavHub eavHub, List<String> keys);

    void delete(EavHub eavHub);

    void update(EavHub eavHub);

}
