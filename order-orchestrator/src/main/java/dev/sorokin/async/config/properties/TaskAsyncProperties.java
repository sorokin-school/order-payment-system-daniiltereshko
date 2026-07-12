package dev.sorokin.async.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.async.task")
public class TaskAsyncProperties {

    @Min(10)
    @NotNull
    private Long retryDelaySeconds = 10L;

    private List<String> retyableExceptions;

    private List<String> nonRetryableExceptions;

}
