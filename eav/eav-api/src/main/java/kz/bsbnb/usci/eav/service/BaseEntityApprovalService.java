package kz.bsbnb.usci.eav.service;

import kz.bsbnb.usci.eav.model.base.BaseEntityJson;

import java.util.List;

public interface BaseEntityApprovalService {

    void approveEntityMaintenance(List<BaseEntityJson> baseEntityList, Long batchId);

}
