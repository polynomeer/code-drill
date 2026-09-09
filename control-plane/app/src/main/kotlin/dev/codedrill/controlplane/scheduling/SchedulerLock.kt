package dev.codedrill.controlplane.scheduling

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

/**
 * 주기 작업을 한 인스턴스에서만 돌린다 (기술 설계서 §12.4).
 *
 * 인스턴스가 둘이 되면 `@Scheduled` 가 둘 다 돈다. **전부 막아야 하는 것은 아니다.**
 *
 * - 아웃박스 발행은 `FOR UPDATE SKIP LOCKED` 라 병렬로 도는 편이 오히려 빠르다.
 * - 적체 게이지는 인스턴스마다 같은 전역 값을 보고할 뿐이라 해가 없다.
 * - **일관성 점검은 막아야 한다.** 같은 위반을 인스턴스 수만큼 경고하면 당번은 사고가
 *   몇 건인지 셀 수 없다 (§13.4).
 *
 * 잠금이 아니라 **임대**다. 잡은 인스턴스가 죽어도 만료되면 다음 인스턴스가 가져간다 —
 * 영원히 잠긴 채로 아무도 점검하지 않는 상태가 더 나쁘다.
 */
@Component
class SchedulerLock(private val jdbc: JdbcTemplate) {

    private val log = LoggerFactory.getLogger(javaClass)

    /** 이 인스턴스를 가리키는 이름. 누가 잡고 있는지 물었을 때 답이 있어야 한다. */
    private val holder = UUID.randomUUID().toString()

    /**
     * 임대를 잡았을 때만 [work] 를 돌린다.
     *
     * [ttl] 은 작업 주기보다 넉넉해야 한다. 짧으면 아직 돌고 있는 사이에 만료돼 다른
     * 인스턴스가 함께 돌기 시작한다.
     */
    fun <T> runIfHolder(name: String, ttl: Duration, work: () -> T): T? {
        if (!acquire(name, ttl)) return null
        return work()
    }

    private fun acquire(name: String, ttl: Duration): Boolean {
        // 한 문장이라 원자적이다. 읽고 나서 쓰면 그 사이에 둘 다 "비어 있다"를 본다.
        val taken = jdbc.update(
            """
            INSERT INTO scheduled_lock (name, holder, expires_at)
            VALUES (?, ?, now() + make_interval(secs => ?))
            ON CONFLICT (name) DO UPDATE
               SET holder = excluded.holder, expires_at = excluded.expires_at
             WHERE scheduled_lock.expires_at < now()
            """.trimIndent(),
            name, holder, ttl.seconds.toDouble(),
        )
        if (taken > 0) log.debug("주기 작업 임대를 잡았다: {} ({}초)", name, ttl.seconds)
        return taken > 0
    }
}
