package dev.codedrill.judge.orchestrator

import dev.codedrill.judge.orchestrator.bundle.BundlePublisher
import dev.codedrill.judge.orchestrator.lease.LeaseRegistry
import dev.codedrill.judge.orchestrator.lease.MemoryLeaseRegistry
import dev.codedrill.judge.orchestrator.lease.RedisLeaseRegistry
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import dev.codedrill.platform.storage.BlobStore
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
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
     * 임대 저장소 (§4.3, production-readiness A3).
     *
     * 기본은 Redis 다. 재시작과 인스턴스 여럿을 넘기는 것은 그쪽뿐이다. `memory` 는 Redis
     * 없는 개발용이며, 그 사실을 기동 로그에 남긴다 — 조용히 내려가면 재시작 뒤에 멈춘
     * 제출이 왜 회수되지 않는지 아무도 모른다.
     *
     * `lease-seconds` 는 실행을 집어 든 워커의 생존을 재고, `dispatch-timeout-seconds`
     * 는 아무도 집어 들지 않은 채 큐에서 기다린 시간을 잰다 (LeaseTiming). DR 훈련은
     * `lease-seconds` 만 줄여 회수 경로를 몇 초 안에 확인한다 (docs/runbook.md#worker-loss).
     */
    @Bean
    fun leaseRegistry(
        @Value("\${codedrill.judge.lease-store:redis}") store: String,
        @Value("\${codedrill.judge.lease-seconds:120}") leaseSeconds: Long,
        @Value("\${codedrill.judge.dispatch-timeout-seconds:600}") dispatchSeconds: Long,
        redis: ObjectProvider<StringRedisTemplate>,
    ): LeaseRegistry {
        val lease = Duration.ofSeconds(leaseSeconds)
        val dispatch = Duration.ofSeconds(dispatchSeconds)
        return when (store) {
            "redis" -> RedisLeaseRegistry(redis.getObject(), leaseDuration = lease, dispatchTimeout = dispatch)
            "memory" -> {
                LoggerFactory.getLogger(javaClass).warn(
                    "임대를 프로세스 메모리에 둔다. 재시작하면 진행 중인 실행을 회수하지 못한다 — 공개 환경에서는 redis 를 쓴다",
                )
                MemoryLeaseRegistry(leaseDuration = lease, dispatchTimeout = dispatch)
            }
            else -> throw IllegalArgumentException("codedrill.judge.lease-store 는 redis 또는 memory 다: $store")
        }
    }

    @Bean
    fun judgeMetrics(registry: MeterRegistry) = JudgeMetrics(registry)

    /** 테스트 번들은 오브젝트 스토어로 간다 (§8.3). 스토어 자체는 platform:storage 가 조립한다. */
    @Bean
    fun bundlePublisher(store: BlobStore) = BundlePublisher(store)

    @Bean
    fun judgeCoordinator(
        packages: ProblemPackageLoader,
        registry: LeaseRegistry,
        gateway: JudgeGateway,
        bundles: BundlePublisher,
        metrics: JudgeMetrics,
        @Value("\${codedrill.judge.max-attempts:3}") maxAttempts: Int,
    ) = JudgeCoordinator(packages, registry, gateway, bundles, metrics, maxAttempts)
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
