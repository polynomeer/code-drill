package dev.codedrill.controlplane.trace

import dev.codedrill.controlplane.scheduling.SchedulerLock
import dev.codedrill.platform.observability.Metrics
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

/**
 * 트레이스 보존 정리 (기술 설계서 §7.4, §8.1).
 *
 * 트레이스는 판정의 부산물이고 **판정보다 훨씬 크다** — 제출 하나에 이벤트 수천 개가
 * 청크로 붙는다. 보존 기간이 없으면 제출이 늘어나는 속도로 스토리지가 늘고, 어느
 * 시점에 DB 가 먼저 한계에 닿는다. 판정 기록은 남겨야 하지만 그때 본 화면까지 영원히
 * 남길 이유는 없다.
 *
 * **판정은 건드리지 않는다.** 트레이스가 사라져도 제출의 판정은 그대로 유효하고(§1.2),
 * 리플레이만 "트레이스가 없다"가 된다 — 이미 있는 상태다.
 *
 * 나눠서 지운다. 한 번에 지우면 큰 트랜잭션이 잠금을 오래 쥐고, 정리가 스스로 장애가
 * 된다 — 일관성 점검을 자주 돌리지 않는 이유와 같다 (§12.4).
 */
@Component
class TraceRetention(
    private val jdbc: JdbcTemplate,
    private val lock: SchedulerLock,
    private val policy: TraceRetentionPolicy,
    registry: MeterRegistry,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 가장 오래된 트레이스의 나이(일).
     *
     * 지운 수만 세면 **정리가 멈춘 것**과 **지울 것이 없는 것**이 같아 보인다. 이 값이
     * 보존 기간을 넘어 계속 자라면 정리가 안 돌고 있다는 뜻이다 (§13.2).
     */
    private val oldestDays = AtomicLong()

    init {
        Gauge.builder(Metrics.TRACE_OLDEST_AGE, oldestDays) { it.get().toDouble() }
            .baseUnit("days")
            .register(registry)
    }

    private val deleted = registry.counter(Metrics.TRACE_RETENTION_DELETED)

    /** 인스턴스가 여럿이어도 한 번만 돈다 (§12.4). */
    @Scheduled(fixedDelayString = "\${codedrill.trace.sweep-interval-ms:3600000}")
    fun scheduledSweep() {
        lock.runIfHolder(LOCK_NAME, LOCK_TTL) { sweep() }
    }

    fun sweep(): Int {
        var removed = 0
        // 한 번에 도는 양을 묶는다. 밀린 것이 많아도 이번 회차에 다 지우려 들지 않고
        // 다음 회차로 넘긴다 — 밀렸다는 사실은 게이지가 말해 준다.
        repeat(policy.maxBatches) {
            val batch = jdbc.update(
                """
                DELETE FROM trace WHERE id IN (
                    SELECT id FROM trace
                     WHERE created_at < now() - make_interval(days => ?)
                     LIMIT ?
                )
                """.trimIndent(),
                policy.retentionDays, policy.batchSize,
            )
            removed += batch
            if (batch == 0) return@repeat
        }

        if (removed > 0) {
            deleted.increment(removed.toDouble())
            log.info("보존 기간이 지난 트레이스 {}건을 지웠다 ({}일)", removed, policy.retentionDays)
        }
        oldestDays.set(oldestAgeDays())
        return removed
    }

    private fun oldestAgeDays(): Long = jdbc.queryForObject(
        "SELECT coalesce(extract(day from now() - min(created_at)), 0)::bigint FROM trace",
        Long::class.java,
    ) ?: 0

    private companion object {
        const val LOCK_NAME = "trace-retention"

        /** 정리 주기(기본 1시간)보다 넉넉하게. 짧으면 돌고 있는 사이에 만료된다. */
        val LOCK_TTL: Duration = Duration.ofHours(2)
    }
}

/**
 * 보존 정책 (§7.4).
 *
 * 30일은 설계서가 정한 값이다. 리플레이는 **방금 낸 제출을 되짚어 보는 기능**이라,
 * 그보다 오래된 트레이스를 여는 일은 거의 없다.
 */
@ConfigurationProperties(prefix = "codedrill.trace")
data class TraceRetentionPolicy(
    val retentionDays: Int = 30,
    /** 한 번에 지우는 행 수. 큰 트랜잭션이 잠금을 오래 쥐지 않게 나눈다. */
    val batchSize: Int = 500,
    /** 한 회차에 도는 배치 수. 밀린 것이 많아도 정리가 DB 를 독차지하지 않게 한다. */
    val maxBatches: Int = 20,
)
