package dev.sorokin;

import dev.sorokin.async.config.TaskAsyncProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties({TaskAsyncProperties.class})
public class OrderOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderOrchestratorApplication.class, args);
    }

}
