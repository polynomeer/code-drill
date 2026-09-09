package dev.codedrill.judge.runner

import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.runner.execution.ExecutionEngine
import dev.codedrill.judge.runner.execution.RuntimeClasspath
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
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories

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
    fun runtimeAdapters(properties: SandboxProperties): Map<Language, RuntimeAdapter> = listOf(
        // Kotlin 만 자기 런타임 jar 를 샌드박스에 들여보낸다. Java 와 Python 은
        // 샌드박스 이미지 안의 런타임을 쓰므로 경로를 맞출 것이 없다.
        KotlinAdapter(runtime = workRoot(properties)?.let(RuntimeClasspath::sharedInto) ?: RuntimeClasspath.all),
        JavaAdapter(),
        PythonAdapter(),
    ).associateBy { it.language }

    private fun workRoot(properties: SandboxProperties): Path? =
        properties.workRoot.takeIf { it.isNotBlank() }?.let(Path::of)?.also { it.createDirectories() }

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
        properties: SandboxProperties,
    ) = ExecutionEngine(
        adapters,
        selector::forLanguage,
        workRoot = workRoot(properties),
    ) { phase, language, outcome, nanos ->
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

    /**
     * 실행 디렉터리를 만들 자리. 비우면 임시 디렉터리를 쓴다.
     *
     * **Runner 가 컨테이너 안에서 돌 때 필요하다.** Runner 는 이 디렉터리를 샌드박스
     * 컨테이너에 마운트하는데, 형제 컨테이너를 띄우면 그 경로를 해석하는 것은 Runner 가
     * 아니라 **호스트의 컨테이너 데몬**이다. Runner 안에서만 존재하는 임시 경로를 주면
     * 데몬은 그런 경로가 없으니 빈 디렉터리를 새로 만들어 마운트하고, 사용자 코드는
     * 자기 소스가 사라진 채로 돌아 전부 SYSTEM_ERROR 가 된다.
     *
     * 그래서 호스트의 한 경로를 **같은 경로로** Runner 에 마운트하고 그 값을 여기 준다.
     * 안과 밖의 이름이 같아지면 누가 해석하든 같은 곳을 가리킨다
     * (deploy/docker-compose.runner.yml).
     */
    val workRoot: String = "",

    val images: Images = Images(),
) {
    data class Images(
        val kotlin: String = "eclipse-temurin:21-jre",
        val java: String = "eclipse-temurin:21-jre",
        val python: String = "python:3.12-alpine",
    )
}
