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

    /**
     * Executor riêng cho RAG indexing (chunk + embed).
     * Dùng 1 thread sequential để tránh SQLite write contention.
     * Queue 20: tối đa 20 tài liệu chờ index đồng thời.
     */
    @Bean(name = "ragIndexExecutor")
    public Executor ragIndexExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("rag-index-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }
}

