package kz.bsbnb.usci.sync.service;

import kz.bsbnb.usci.model.util.Application;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;

import java.util.List;

public interface ServiceRepository {

    void setApplicationList(List<Application> applications);

    void setEntityRmiServices(List<RmiProxyFactoryBean> entityRmiServices);

    List<RmiProxyFactoryBean> getEntityRmiServices();

}
