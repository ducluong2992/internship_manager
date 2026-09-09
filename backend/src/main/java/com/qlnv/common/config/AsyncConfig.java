package com.qlnv.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Cấu hình ThreadPool cho @Async import jobs.
 * - corePoolSize = 2: Tối đa 2 job chạy song song (tránh lock SQLite)
 * - maxPoolSize  = 4: Mở rộng khi queue đầy
 * - queueCapacity = 50: Hàng đợi tối đa 50 job
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "importTaskExecutor")
    public Executor importTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("import-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
