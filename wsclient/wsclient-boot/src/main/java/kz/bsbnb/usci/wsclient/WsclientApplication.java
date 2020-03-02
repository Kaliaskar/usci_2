package kz.bsbnb.usci.wsclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Configuration
@ComponentScan(basePackages = "kz.bsbnb.usci" )
@EnableConfigurationProperties
@EnableEurekaClient
//@EnableScheduling
public class WsclientApplication {

    public static void main(String[] args) {
        SpringApplication.run(WsclientApplication.class, args);
    }
}
