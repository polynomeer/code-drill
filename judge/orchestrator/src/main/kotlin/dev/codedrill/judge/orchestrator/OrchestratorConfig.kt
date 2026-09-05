package dev.codedrill.judge.orchestrator

import dev.codedrill.judge.orchestrator.lease.AttemptRegistry
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

/**
 * 오케스트레이터 조립.
 *
 * 문제 패키지는 실행 영역의 read-only 아티팩트 캐시에서 읽는다 (§2.3). 슬라이스에서는
 * 저장소의 `content/problems` 를 그 자리에 놓는다.
 */
@Configuration
class OrchestratorConfig {

    @Bean
    fun problemPackageLoader(@Value("\${codedrill.content.root}") root: String) =
        ProblemPackageLoader(Path.of(root))

    @Bean
    fun attemptRegistry() = AttemptRegistry()

    @Bean
    fun judgeCoordinator(
        packages: ProblemPackageLoader,
        registry: AttemptRegistry,
        gateway: JudgeGateway,
    ) = JudgeCoordinator(packages, registry, gateway)
}
