package kz.bsbnb.usci.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
@ComponentScan(basePackages = "kz.bsbnb.usci")
@EnableConfigurationProperties
@EnableEurekaClient
@EnableFeignClients(basePackages = {"kz.bsbnb.usci.util.client", "kz.bsbnb.usci.mail.client","kz.bsbnb.usci.core.client"})
public class CoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoreApplication.class, args);
	}

}
