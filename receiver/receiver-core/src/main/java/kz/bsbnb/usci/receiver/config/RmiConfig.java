package kz.bsbnb.usci.receiver.config;

import kz.bsbnb.usci.sync.service.SyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;

import static kz.bsbnb.usci.model.Constants.SYNC_RMI_PORT;

/**
 * @author Jandos Iskakov
 */

@Configuration
public class RmiConfig {
    @Value("${sync.host}")
    private String syncHost;

    @Bean
    public RmiProxyFactoryBean syncService() {
        RmiProxyFactoryBean rmiProxyFactory = new RmiProxyFactoryBean();
        rmiProxyFactory.setServiceUrl("rmi://" + syncHost + ":" + SYNC_RMI_PORT +  "/syncService");
        rmiProxyFactory.setServiceInterface(SyncService.class);
        return rmiProxyFactory;
    }

}
