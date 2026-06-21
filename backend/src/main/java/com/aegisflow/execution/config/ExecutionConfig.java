package com.aegisflow.execution.config;

import com.aegisflow.execution.service.WorkerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ExecutionConfig {

    @Bean
    CommandLineRunner registerDefaultWorker(WorkerService workerService) {
        return args -> workerService.registerWorker("local-worker-1", 5);
    }
}
