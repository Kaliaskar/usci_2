package kz.bsbnb.usci.sync.config;

import kz.bsbnb.usci.sync.service.SyncService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.remoting.rmi.RmiServiceExporter;

import static kz.bsbnb.usci.model.Constants.SYNC_RMI_PORT;

/**
 * @author Jandos Iskakov
 */

@Configuration
public class RmiConfig {

    @Bean
    public RmiServiceExporter exporter(SyncService impl) {
        RmiServiceExporter exporter = new RmiServiceExporter();
        exporter.setServiceInterface(SyncService.class);
        exporter.setService(impl);
        exporter.setServiceName("syncService");
        exporter.setRegistryPort(SYNC_RMI_PORT);
        return exporter;
    }

}
