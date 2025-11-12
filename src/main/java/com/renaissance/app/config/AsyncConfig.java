package com.renaissance.app.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync  // Enables @Async support
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 🧠 Core settings
        executor.setCorePoolSize(5);           // Minimum number of threads
        executor.setMaxPoolSize(15);           // Maximum number of threads
        executor.setQueueCapacity(100);        // Queue size before new threads are created
        executor.setKeepAliveSeconds(60);      // Idle thread retention time
        executor.setThreadNamePrefix("AsyncExecutor-"); // Thread naming pattern

        // 🚨 Rejection policy (what happens when all threads are busy)
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        // Ensure all threads are properly initialized
        executor.initialize();

        return executor;
    }
}
