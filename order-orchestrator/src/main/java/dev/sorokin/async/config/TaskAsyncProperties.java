package dev.sorokin.async.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.async.task.scheduler")
public class TaskAsyncProperties {

    @Min(100)
    @NotNull
    private Long fixedDelayMs = 5000L;

    @Min(1)
    @NotNull
    private Integer inProgressTimeoutMinutes = 5;

    @Min(10)
    @NotNull
    private Integer batchSize = 100;
}
