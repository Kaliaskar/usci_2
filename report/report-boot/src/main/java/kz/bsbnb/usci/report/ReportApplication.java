package kz.bsbnb.usci.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
@ComponentScan(basePackages = "kz.bsbnb.usci" )
@EnableFeignClients(basePackages = {"kz.bsbnb.usci.sync.client", "kz.bsbnb.usci.core.client",
        "kz.bsbnb.usci.eav.client", "kz.bsbnb.usci.util.client", "kz.bsbnb.usci.mail.client", "kz.bsbnb.usci.wsclient.client"})
@EnableConfigurationProperties
@EnableEurekaClient
public class ReportApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportApplication.class, args);
    }

}
