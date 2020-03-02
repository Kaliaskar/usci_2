package kz.bsbnb.usci.sync;

import kz.bsbnb.usci.eav.service.EntityRmiService;
import kz.bsbnb.usci.model.util.Application;
import kz.bsbnb.usci.sync.service.ServiceRepository;
import kz.bsbnb.usci.util.client.ApplicationClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RmiRegister implements CommandLineRunner {
    private final ApplicationClient applicationClient;
    private final ServiceRepository serviceRepository;

    public RmiRegister(ApplicationClient applicationClient, ServiceRepository serviceRepository) {
        this.applicationClient = applicationClient;
        this.serviceRepository = serviceRepository;
    }

    @Override
    public void run(String... args) {
        List<Application> applications = applicationClient.getApplicationList();

        List<RmiProxyFactoryBean> entityRmiServiceList = new ArrayList<>();

        List<Application> cores = applications.stream().filter(app -> app.getModule().equals("CORE")).collect(Collectors.toList());

        cores.forEach(app -> {
            RmiProxyFactoryBean entityRmiService = new RmiProxyFactoryBean();
            entityRmiService.setServiceUrl("rmi://" + app.getHost() + ":" + app.getRmiPort() + "/entityRmiService");
            entityRmiService.setServiceInterface(EntityRmiService.class);
            entityRmiService.setRefreshStubOnConnectFailure(true);
            entityRmiService.afterPropertiesSet();

            entityRmiServiceList.add(entityRmiService);
        });

        serviceRepository.setEntityRmiServices(entityRmiServiceList);
    }

}
