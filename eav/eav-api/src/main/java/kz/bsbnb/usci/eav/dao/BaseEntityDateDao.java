package kz.bsbnb.usci.eav.dao;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.core.BaseEntityDate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * @author Jandos Iskakov
 */

public interface BaseEntityDateDao {

    void insert(List<BaseEntityDate> baseEntityDates);

    void update(BaseEntityDate baseEntityDate);

    Optional<BaseEntityDate> find(BaseEntity baseEntity);

    List<BaseEntityDate> find(BaseEntity baseEntity, LocalDate reportDate);

}
