package dev.codedrill.judge.runner

import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.runner.execution.ExecutionEngine
import dev.codedrill.judge.runner.execution.adapter.JavaAdapter
import dev.codedrill.judge.runner.execution.adapter.KotlinAdapter
import dev.codedrill.judge.runner.execution.adapter.PythonAdapter
import dev.codedrill.judge.runner.execution.adapter.RuntimeAdapter
import dev.codedrill.judge.runner.execution.sandbox.SandboxSelector
import dev.codedrill.platform.observability.Metrics
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

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

    /**
     * 실행 단계 계측 (§13.2 Runner).
     *
     * 컴파일과 실행을 나눠 잰다. 둘을 합쳐 두면 "느려졌다"까지만 알 뿐, 런타임 이미지가
     * 바뀐 것인지 문제 데이터가 커진 것인지 구분할 수 없다.
     */
    @Bean
    fun executionEngine(
        adapters: Map<Language, RuntimeAdapter>,
        selector: SandboxSelector,
        registry: MeterRegistry,
    ) = ExecutionEngine(adapters, selector::forLanguage) { phase, language, outcome, nanos ->
        Timer.builder(if (phase == "compile") Metrics.COMPILE else Metrics.EXECUTE)
            .tag(Metrics.Tag.LANGUAGE, language.name)
            .tag(if (phase == "compile") Metrics.Tag.OUTCOME else Metrics.Tag.MODE, outcome)
            .publishPercentileHistogram()
            .register(registry)
            .record(nanos, TimeUnit.NANOSECONDS)
    }
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
