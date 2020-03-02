package kz.bsbnb.usci.core.config;

import kz.bsbnb.usci.eav.service.EntityRmiService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.remoting.rmi.RmiServiceExporter;

@Configuration
public class RmiConfig {

    @Bean
    public RmiServiceExporter exporter(EntityRmiService impl) {
        RmiServiceExporter exporter = new RmiServiceExporter();
        exporter.setServiceInterface(EntityRmiService.class);
        exporter.setService(impl);
        exporter.setServiceName("entityRmiService");
        exporter.setRegistryPort(1092);
        return exporter;
    }

}
