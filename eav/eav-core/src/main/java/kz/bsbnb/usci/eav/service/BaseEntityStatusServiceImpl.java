package kz.bsbnb.usci.eav.service;

import kz.bsbnb.usci.eav.dao.BaseEntityStatusDao;
import kz.bsbnb.usci.eav.model.core.BaseEntityStatus;
import org.springframework.stereotype.Service;

@Service
public class BaseEntityStatusServiceImpl implements  BaseEntityStatusService {

    private  final BaseEntityStatusDao baseEntityStatusDao;

    BaseEntityStatusServiceImpl(BaseEntityStatusDao baseEntityStatusDao) {
        this.baseEntityStatusDao = baseEntityStatusDao;
    }

    @Override
    public BaseEntityStatus insert(BaseEntityStatus baseEntityStatus) {
        return baseEntityStatusDao.insertBatchMethod(baseEntityStatus);
    }

    @Override
    public void update(BaseEntityStatus baseEntityStatus) {
        baseEntityStatusDao.update(baseEntityStatus);
    }
}
