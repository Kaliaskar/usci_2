package kz.bsbnb.usci.eav.service;

import kz.bsbnb.usci.eav.model.core.BaseEntityStatus;

public interface BaseEntityStatusService {

    BaseEntityStatus insert(BaseEntityStatus baseEntityStatus);

    void update(BaseEntityStatus baseEntityStatus);
}
