package dev.sorokin.async.config;

import dev.sorokin.async.config.properties.TaskThreadPoolProperties;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@RequiredArgsConstructor
public class AsyncConfiguration {

    private final TaskThreadPoolProperties taskThreadPoolProperties;

    @Bean("taskThreadPool")
    ThreadPoolTaskExecutor taskThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(taskThreadPoolProperties.getCorePoolSize());
        executor.setMaxPoolSize(taskThreadPoolProperties.getMaxPoolSize());
        executor.setKeepAliveSeconds(taskThreadPoolProperties.getKeepAliveSeconds());
        executor.setQueueCapacity(taskThreadPoolProperties.getQueueCapacity());
        executor.setAwaitTerminationSeconds(taskThreadPoolProperties.getAwaitTerminationSeconds());
        executor.setThreadNamePrefix(taskThreadPoolProperties.getThreadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

        executor.initialize();
        return executor;
    }

}
