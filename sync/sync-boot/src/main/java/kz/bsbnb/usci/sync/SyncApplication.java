package kz.bsbnb.usci.sync;

import kz.bsbnb.usci.sync.job.DataJob;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Configuration
@ComponentScan(basePackages = {"kz.bsbnb.usci"})
@EnableScheduling
@EnableConfigurationProperties
@EnableFeignClients(basePackages = {"kz.bsbnb.usci.receiver.client", "kz.bsbnb.usci.util.client"})
@EnableEurekaClient
public class SyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyncApplication.class, args);
    }

    @Bean
    public DataJob dataJob() {
        return new DataJob();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor();
    }

    /**
     * Запускаем DataJob асинхронно после запуска приложения
     */
    @Bean
    public CommandLineRunner runner(TaskExecutor executor) {
        return args -> executor.execute(dataJob());
    }

}
