package dev.sorokin.async.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.async.task.thread-pool")
public class TaskThreadPoolProperties {

    @Min(1)
    @Max(100)
    @NotNull
    private Integer corePoolSize = 10;

    @Min(1)
    @Max(200)
    @NotNull
    private Integer maxPoolSize = 50;

    @NotNull
    private Integer keepAliveSeconds = 60;

    @Min(10)
    @NotNull
    private Integer queueCapacity = 1000;

    @Min(15)
    @Max(600)
    @NotNull
    private Integer awaitTerminationSeconds = 120;

    @NotNull
    private String threadNamePrefix = "async-task-pool-";
}
