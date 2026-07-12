package dev.sorokin;

import dev.sorokin.async.config.properties.IOThreadPoolProperties;
import dev.sorokin.async.config.properties.TaskAsyncProperties;
import dev.sorokin.async.config.properties.TaskAsyncSchedulerProperties;
import dev.sorokin.async.config.properties.TaskThreadPoolProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableFeignClients
@SpringBootApplication
@EnableConfigurationProperties({TaskAsyncSchedulerProperties.class, TaskAsyncProperties.class,
        TaskThreadPoolProperties.class, IOThreadPoolProperties.class})
public class OrderOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderOrchestratorApplication.class, args);
    }

}
