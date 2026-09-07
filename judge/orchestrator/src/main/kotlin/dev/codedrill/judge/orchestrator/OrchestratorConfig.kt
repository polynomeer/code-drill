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
     * 두 시간 한계는 재는 것이 다르다.
     *
     * `lease-seconds` 는 실행을 집어 든 워커의 생존을 재고, `dispatch-timeout-seconds`
     * 는 아무도 집어 들지 않은 채 큐에서 기다린 시간을 잰다. 둘을 하나로 묶으면 큐가
     * 밀렸을 뿐인 제출이 워커 유실로 처리돼, 바쁠 때만 멀쩡한 제출이 SYSTEM_ERROR 가 된다.
     *
     * DR 훈련은 `lease-seconds` 만 줄여 회수 경로를 몇 초 안에 확인한다
     * (docs/runbook.md#worker-loss).
     */
    @Bean
    fun attemptRegistry(
        @Value("\${codedrill.judge.lease-seconds:120}") leaseSeconds: Long,
        @Value("\${codedrill.judge.dispatch-timeout-seconds:600}") dispatchSeconds: Long,
    ) = AttemptRegistry(
        leaseDuration = Duration.ofSeconds(leaseSeconds),
        dispatchTimeout = Duration.ofSeconds(dispatchSeconds),
    )

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
