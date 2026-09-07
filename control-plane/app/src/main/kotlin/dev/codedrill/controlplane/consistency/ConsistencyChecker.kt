package dev.codedrill.controlplane.consistency

import dev.codedrill.platform.observability.Metrics
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 일관성 점검 작업 (기술 설계서 §12.4).
 *
 * 분산 시스템에서 데이터가 어긋나는 것은 예외가 아니라 **정상 상황**이다. 프로세스는
 * 아무 데서나 죽고, 메시지는 늦게 오고, 재시도는 절반만 성공한다. 그래서 "어긋나지
 * 않게 만든다"가 아니라 "어긋난 것을 찾아낸다"가 설계 목표다.
 *
 * 여기서 하는 일은 탐지까지다. 자동 복구를 붙이지 않는 것은 의도다 — 어긋난 이유를
 * 모르는 채로 고치면 원인은 남고 증거만 사라진다. 각 점검의 대응은
 * `docs/runbook.md#consistency` 가 정한다.
 *
 * 조립 지점인 :control-plane:app 에 둔다. 여러 도메인의 테이블을 가로질러 읽으므로,
 * 어느 한 도메인 모듈에 넣으면 그 모듈이 남의 데이터를 소유하게 된다 (§3.1).
 */
@Component
class ConsistencyChecker(
    private val jdbc: JdbcTemplate,
    registry: MeterRegistry,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val counts = ConcurrentHashMap<String, AtomicLong>()

    init {
        // 게이지는 미리 0 으로 등록한다. 위반이 처음 생길 때 시계열이 나타나면
        // "값이 없음"과 "0 건"을 구분할 수 없고, 경보도 그때까지 평가되지 않는다.
        CHECKS.forEach { check ->
            val holder = counts.computeIfAbsent(check.id) { AtomicLong() }
            Gauge.builder(Metrics.CONSISTENCY_VIOLATIONS, holder) { it.get().toDouble() }
                .tag(Metrics.Tag.CHECK, check.id)
                .register(registry)
        }
    }

    /**
     * 모든 점검을 돌리고 게이지를 갱신한다.
     *
     * 주기가 길다. 이 쿼리들은 인덱스를 타지 않는 전수 검사에 가까워서, 자주 돌리면
     * 점검이 스스로 DB 포화를 만든다 — 찾으려던 장애를 만들어 내는 셈이다.
     */
    @Scheduled(fixedDelayString = "\${codedrill.consistency.interval-ms:60000}")
    fun sweep(): List<CheckResult> {
        val results = CHECKS.map { check ->
            val ids = runCatching { jdbc.queryForList(check.sql, String::class.java, *check.args) }
                .getOrElse { error ->
                    log.warn("일관성 점검 {} 이 실패했다: {}", check.id, error.message)
                    return@map CheckResult(check.id, check.description, -1, emptyList())
                }
            counts.getValue(check.id).set(ids.size.toLong())
            CheckResult(check.id, check.description, ids.size, ids.take(SAMPLE_SIZE))
        }

        results.filter { it.count > 0 }.forEach {
            log.warn("일관성 점검 {} 위반 {}건: {}", it.check, it.count, it.samples)
        }
        return results
    }

    data class CheckResult(
        val check: String,
        val description: String,
        /** -1 이면 점검 자체가 실패했다는 뜻이다. 0 과 구분해야 한다. */
        val count: Int,
        val samples: List<String>,
    )

    /**
     * 점검 하나.
     *
     * [staleSeconds] 가 0 이면 "오래됨"이라는 개념이 없는 점검이다 — 어긋난 순간부터
     * 위반이다. 그때는 바인딩할 인자도 없다.
     */
    private data class Check(
        val id: String,
        val description: String,
        val staleSeconds: Int,
        val sql: String,
    ) {
        val args: Array<Any> = if (staleSeconds > 0) arrayOf(staleSeconds) else emptyArray()
    }

    private companion object {
        const val SAMPLE_SIZE = 5

        val CHECKS = listOf(
            // §12.4 "QUEUED 인데 outbox 가 없는 제출"
            //
            // 두 행은 같은 트랜잭션에서 커밋되므로 정상적으로는 생길 수 없다. 생겼다면
            // 아웃박스를 우회해 제출을 만든 경로가 있다는 뜻이고, 그 제출은 영영
            // 채점되지 않는다.
            Check(
                id = "queued_without_outbox",
                description = "QUEUED 인데 아웃박스 이벤트가 없다 (아웃박스 우회 경로 의심)",
                staleSeconds = 60,
                sql = """
                    SELECT s.id::text FROM submission s
                     WHERE s.status = 'QUEUED'
                       AND s.created_at < now() - make_interval(secs => ?)
                       AND NOT EXISTS (
                           SELECT 1 FROM outbox_event o WHERE o.aggregate_id = s.id::text
                       )
                     LIMIT 100
                """.trimIndent(),
            ),

            // §12.4 "오래된 QUEUED 제출" — 발행이 멈췄다. 브로커나 퍼블리셔를 본다.
            Check(
                id = "queued_unpublished",
                description = "QUEUED 이고 아웃박스가 아직 발행되지 않았다 (브로커·퍼블리셔)",
                staleSeconds = 120,
                sql = """
                    SELECT s.id::text FROM submission s
                     JOIN outbox_event o ON o.aggregate_id = s.id::text
                     WHERE s.status = 'QUEUED'
                       AND o.published_at IS NULL
                       AND o.occurred_at < now() - make_interval(secs => ?)
                     LIMIT 100
                """.trimIndent(),
            ),

            // §4.2 실행 중 상태에서 멈춘 제출. 오케스트레이터의 임대 회수가 살아 있으면
            // 여기까지 오지 않는다. 걸린다면 회수 자체가 멈춘 것이다.
            Check(
                id = "stuck_in_flight",
                description = "실행 중 상태로 멈췄다 (임대 회수가 동작하지 않는다)",
                staleSeconds = 600,
                sql = """
                    SELECT id::text FROM submission
                     WHERE status IN ('LEASED', 'COMPILING', 'RUNNING', 'AGGREGATING')
                       AND updated_at < now() - make_interval(secs => ?)
                     LIMIT 100
                """.trimIndent(),
            ),

            // §12.4 "종료 submission 과 결과 불일치"
            //
            // 종료 상태인데 판정이 없다. 사용자에게는 "끝났는데 결과가 없는" 화면이 된다.
            Check(
                id = "completed_without_verdict",
                description = "COMPLETED 인데 판정이 비어 있다",
                staleSeconds = 0,
                sql = """
                    SELECT id::text FROM submission
                     WHERE status = 'COMPLETED' AND (verdict IS NULL OR score IS NULL)
                     LIMIT 100
                """.trimIndent(),
            ),

            // §12.4 "DB 참조 객체의 존재 검증"
            Check(
                id = "orphan_trace",
                description = "트레이스가 없는 제출을 가리킨다",
                staleSeconds = 0,
                sql = """
                    SELECT t.id FROM trace t
                     WHERE NOT EXISTS (SELECT 1 FROM submission s WHERE s.id = t.submission_id)
                     LIMIT 100
                """.trimIndent(),
            ),

            // §3.2 공개 포인터가 없는 버전을 가리킨다. 목록에는 보이는데 열리지 않는
            // 문제가 되므로, 사용자에게 바로 드러난다.
            Check(
                id = "published_version_missing",
                description = "공개 포인터가 등록되지 않은 버전을 가리킨다",
                staleSeconds = 0,
                sql = """
                    SELECT p.id FROM problem p
                     WHERE p.published_version_id IS NOT NULL
                       AND NOT EXISTS (
                           SELECT 1 FROM problem_version v WHERE v.id = p.published_version_id
                       )
                     LIMIT 100
                """.trimIndent(),
            ),
        )
    }
}
