package dev.codedrill.controlplane.submission

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * 제출 쿼터 (기술 설계서 §10.2 배압, §11.1 남용).
 *
 * §10.2 의 배압은 큐가 깊어졌을 때 **뒤에서** 밀어내는 장치다. 들어오는 쪽을 막는 것은
 * 아무것도 없었다 — 로그인한 계정 하나가 판정 큐를 혼자 채울 수 있고, 그러면 다른 모든
 * 사용자의 채점이 그 뒤에 선다. 무료 가입을 여는 순간 이것이 첫 사고가 된다.
 *
 * 두 가지를 본다. 둘이 막는 것이 다르다.
 *
 * - **동시 진행 수.** 판정 큐를 실제로 지키는 것은 이쪽이다. 한 사람이 200건을 걸어
 *   두지 못한다.
 * - **분당 제출 수.** 빨리 끝나는 제출을 무한히 반복하는 경우를 잡는다. 동시 진행
 *   제한만 있으면 초당 수십 건도 통과한다.
 *
 * **DB 로 센다.** §10.1 이 쿼터 카운터를 Redis 로 두라고 하지만, 세어야 할 값이 이미
 * `submission` 에 있다. 카운터를 따로 두면 그 둘이 어긋날 수 있고 — 제출은 커밋됐는데
 * 카운터는 안 올랐거나 그 반대 — 어긋난 카운터는 막아야 할 것을 통과시킨다. 부하가
 * 문제가 되면 그때 옮긴다.
 */
@Component
class SubmissionQuota(
    private val jdbc: JdbcTemplate,
    private val limits: QuotaLimits,
) {

    /** 넘었으면 무엇을 넘었는지 돌려준다. 통과면 `null`. */
    fun exceededBy(userId: String): String? {
        val inFlight = jdbc.queryForObject(
            """
            SELECT count(*) FROM submission
             WHERE user_id = ? AND status IN ('CREATED', 'QUEUED', 'LEASED', 'RUNNING')
            """.trimIndent(),
            Int::class.java, userId,
        ) ?: 0
        if (inFlight >= limits.inFlight) {
            return "채점 중인 제출이 ${limits.inFlight}건이다. 하나가 끝나면 다시 낼 수 있다"
        }

        val recent = jdbc.queryForObject(
            "SELECT count(*) FROM submission WHERE user_id = ? AND created_at > now() - make_interval(secs => ?)",
            Int::class.java, userId, WINDOW_SECONDS.toDouble(),
        ) ?: 0
        if (recent >= limits.perMinute) {
            return "1분에 ${limits.perMinute}건까지 낼 수 있다"
        }

        return null
    }

    private companion object {
        const val WINDOW_SECONDS = 60
    }
}

/**
 * 쿼터 상한.
 *
 * 기본값은 **사람이 손으로 푸는 속도**를 기준으로 잡았다. 한 문제를 고쳐 다시 내는 데
 * 몇 초는 걸리므로 분당 30건이면 사람에게는 닿지 않고, 스크립트에는 곧바로 닿는다.
 */
@ConfigurationProperties(prefix = "codedrill.quota")
data class QuotaLimits(
    /** 동시에 채점 중일 수 있는 제출 수. */
    val inFlight: Int = 5,
    /** 1분 동안 낼 수 있는 제출 수. */
    val perMinute: Int = 30,
)

/** 쿼터를 넘겼다. 컨트롤러가 429 로 옮긴다 (§9.4 QUOTA_EXCEEDED). */
class QuotaExceededException(message: String) : RuntimeException(message)
