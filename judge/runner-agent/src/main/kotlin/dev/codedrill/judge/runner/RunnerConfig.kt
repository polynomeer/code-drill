package dev.codedrill.judge.runner

import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.runner.execution.ExecutionEngine
import dev.codedrill.judge.runner.execution.adapter.JavaAdapter
import dev.codedrill.judge.runner.execution.adapter.KotlinAdapter
import dev.codedrill.judge.runner.execution.adapter.PythonAdapter
import dev.codedrill.judge.runner.execution.adapter.RuntimeAdapter
import dev.codedrill.judge.runner.execution.sandbox.SandboxSelector
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Runner 조립.
 *
 * 런타임 이미지는 설정으로 바꿀 수 있다. 운영에서는 태그가 아니라 digest 로 고정해야
 * 과거 제출을 원래 런타임으로 재현할 수 있다 (§5.5).
 */
@Configuration
@EnableConfigurationProperties(SandboxProperties::class)
class RunnerConfig {

    @Bean
    fun runtimeAdapters(): Map<Language, RuntimeAdapter> = listOf(
        KotlinAdapter(),
        JavaAdapter(),
        PythonAdapter(),
    ).associateBy { it.language }

    @Bean
    fun sandboxSelector(
        properties: SandboxProperties,
        adapters: Map<Language, RuntimeAdapter>,
    ): SandboxSelector =
        SandboxSelector(
            images = mapOf(
                Language.KOTLIN to properties.images.kotlin,
                Language.JAVA to properties.images.java,
                Language.PYTHON to properties.images.python,
            ),
            requireIsolation = properties.requireIsolation,
            needsContainerForMemory = adapters.values
                .filterNot { it.enforcesMemoryWithoutContainer() }
                .map { it.language }
                .toSet(),
        ).also { it.report() }

    @Bean
    fun executionEngine(
        adapters: Map<Language, RuntimeAdapter>,
        selector: SandboxSelector,
    ) = ExecutionEngine(adapters, selector::forLanguage)
}

@ConfigurationProperties(prefix = "codedrill.sandbox")
data class SandboxProperties(
    /**
     * 컨테이너 격리를 강제할지.
     *
     * 공개 환경에서는 반드시 true 여야 한다. false 이면 컨테이너 런타임이 없을 때
     * 격리 없는 프로세스 실행으로 내려간다.
     */
    val requireIsolation: Boolean = false,
    val images: Images = Images(),
) {
    data class Images(
        val kotlin: String = "eclipse-temurin:21-jre",
        val java: String = "eclipse-temurin:21-jre",
        val python: String = "python:3.12-alpine",
    )
}
