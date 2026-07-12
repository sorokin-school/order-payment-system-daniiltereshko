package dev.sorokin.async.config;

import dev.sorokin.async.config.properties.TaskThreadPoolProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@RequiredArgsConstructor
public class AsyncConfiguration {

    private final TaskThreadPoolProperties taskThreadPoolProperties;

    @Bean
    ThreadPoolTaskExecutor taskThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(taskThreadPoolProperties.getCorePoolSize());
        executor.setMaxPoolSize(taskThreadPoolProperties.getMaxPoolSize());
        executor.setKeepAliveSeconds(taskThreadPoolProperties.getKeepAliveSeconds());
        executor.setQueueCapacity(taskThreadPoolProperties.getQueueCapacity());
        executor.setAwaitTerminationSeconds(taskThreadPoolProperties.getAwaitTerminationSeconds());
        executor.setThreadNamePrefix(taskThreadPoolProperties.getThreadNamePrefix());

        executor.initialize();
        return executor;
    }
}
