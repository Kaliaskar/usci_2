package kz.bsbnb.usci.util.service.impl;

import kz.bsbnb.usci.model.util.Application;
import kz.bsbnb.usci.util.dao.ApplicationDao;
import kz.bsbnb.usci.util.service.ApplicationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApplicationServiceImpl implements ApplicationService {
    private final ApplicationDao applicationDao;

    public ApplicationServiceImpl(ApplicationDao applicationDao) {
        this.applicationDao = applicationDao;
    }

    @Override
    public List<Application> getAppList() {
        return applicationDao.getAppList();
    }

}
