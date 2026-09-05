package dev.codedrill.judge.runner

import dev.codedrill.judge.runner.execution.ExecutionEngine
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RunnerConfig {

    @Bean
    fun executionEngine() = ExecutionEngine()
}
