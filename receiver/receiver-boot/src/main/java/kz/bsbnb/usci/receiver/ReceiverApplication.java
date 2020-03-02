package kz.bsbnb.usci.receiver;

import kz.bsbnb.usci.receiver.config.ReceiverConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@Configuration
@ComponentScan(basePackages = {"kz.bsbnb.usci.receiver", "kz.bsbnb.usci.sync", "kz.bsbnb.usci.eav", "kz.bsbnb.usci.brms"})
@EnableConfigurationProperties
@EnableScheduling
@EnableFeignClients(basePackages = {"kz.bsbnb.usci.sync.client", "kz.bsbnb.usci.core.client",
        "kz.bsbnb.usci.eav.client", "kz.bsbnb.usci.util.client", "kz.bsbnb.usci.mail.client"})
@EnableEurekaClient
@RibbonClient(name = "receiver", configuration = ReceiverConfig.class)
public class ReceiverApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReceiverApplication.class, args);
    }

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
