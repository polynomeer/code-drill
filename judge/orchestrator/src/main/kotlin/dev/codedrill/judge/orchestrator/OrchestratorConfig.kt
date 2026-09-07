package dev.codedrill.judge.orchestrator

import dev.codedrill.judge.orchestrator.lease.AttemptRegistry
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.time.Duration

/**
 * 오케스트레이터 조립.
 *
 * 문제 패키지는 실행 영역의 read-only 아티팩트 캐시에서 읽는다 (§2.3). 슬라이스에서는
 * 저장소의 `content/problems` 를 그 자리에 놓는다.
 */
@Configuration
@EnableScheduling
class OrchestratorConfig {

    @Bean
    fun problemPackageLoader(@Value("\${codedrill.content.root}") root: String) =
        ProblemPackageLoader(Path.of(root))

    /**
     * 임대 기간은 설정으로 뺀다.
     *
     * 기본 2분은 가장 느린 문제의 실행 시간보다 충분히 길어야 한다 — 짧으면 멀쩡히
     * 돌고 있는 실행을 워커 유실로 오해해 같은 제출을 두 번 돌린다. DR 훈련에서는
     * 짧게 줄여 회수 경로를 몇 초 안에 확인한다 (docs/runbook.md).
     */
    @Bean
    fun attemptRegistry(
        @Value("\${codedrill.judge.lease-seconds:120}") leaseSeconds: Long,
    ) = AttemptRegistry(leaseDuration = Duration.ofSeconds(leaseSeconds))

    @Bean
    fun judgeMetrics(registry: MeterRegistry) = JudgeMetrics(registry)

    @Bean
    fun judgeCoordinator(
        packages: ProblemPackageLoader,
        registry: AttemptRegistry,
        gateway: JudgeGateway,
        metrics: JudgeMetrics,
        @Value("\${codedrill.judge.max-attempts:3}") maxAttempts: Int,
    ) = JudgeCoordinator(packages, registry, gateway, metrics, maxAttempts)
}

/**
 * 만료된 임대를 주기적으로 회수한다 (기술 설계서 §4.3, §12.4).
 *
 * 조정자 자신이 스케줄을 갖지 않게 분리해 둔다. 조정자는 브로커도 스프링도 없이
 * 테스트할 수 있어야 하고, "언제 도는가"는 배치의 관심사다.
 */
@Component
class LeaseReaper(private val coordinator: JudgeCoordinator) {

    @Scheduled(fixedDelayString = "\${codedrill.judge.reap-interval-ms:5000}")
    fun reap() = coordinator.reclaimExpiredLeases()
}
