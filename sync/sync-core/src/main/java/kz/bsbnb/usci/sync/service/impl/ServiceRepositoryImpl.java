package kz.bsbnb.usci.sync.service.impl;

import kz.bsbnb.usci.model.util.Application;
import kz.bsbnb.usci.sync.service.ServiceRepository;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceRepositoryImpl implements ServiceRepository {
    private List<Application> applicationList;
    private List<RmiProxyFactoryBean> entityRmiServices;

    public List<RmiProxyFactoryBean> getEntityRmiServices() {
        return entityRmiServices;
    }

    public void setEntityRmiServices(List<RmiProxyFactoryBean> entityRmiServices) {
        this.entityRmiServices = entityRmiServices;
    }

    public List<Application> getApplicationList() {
        return applicationList;
    }

    @Override
    public void setApplicationList(List<Application> applications) {
        this.applicationList = applications;
    }

}
