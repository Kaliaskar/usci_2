package kz.bsbnb.usci.util;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Configuration
@EnableConfigurationProperties
@EnableScheduling
@EnableEurekaClient
public class UtilApplication {

    public static void main(String[] args) {
        SpringApplication.run(UtilApplication.class, args);
    }

}
