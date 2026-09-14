package dev.codedrill.judge.orchestrator.lease

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.SubmissionQueued
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Redis 에 두는 임대 (기술 설계서 §4.3, production-readiness A3).
 *
 * 프로세스 메모리에 두면 두 가지가 깨진다. 재시작 이전에 띄운 실행이 영영 결과를 보내지
 * 않으면 회수할 기록이 없고, 토큰이 1 부터 다시 세어 재시작 이전 워커의 결과가 새 임대를
 * 이길 수 있다. 여기서는 토큰이 `INCR` 하나에서 나오므로 재시작과 인스턴스 여럿을 넘어
 * 단조롭다.
 *
 * **판단은 전부 Lua 안에서 한다.** 임대·연장·수락은 "읽고 비교하고 쓰는" 한 덩어리이고,
 * 그 사이에 다른 인스턴스가 끼어들면 두 워커가 같은 제출을 붙들거나 스테일 결과가
 * 통과한다. 스크립트 하나가 원자적으로 도는 것이 잠금 없이 그것을 막는 길이다.
 *
 * 키는 한 접두사 아래 둔다. 제어 영역과 같은 Redis 를 써도 섞이지 않는다.
 *
 * | 키 | 무엇 |
 * |---|---|
 * | `judge:lease:<제출>` | 해시 — attempt, token, expiresAt, started, executionId, origin |
 * | `judge:expiry` | 만료 시각 순 정렬 집합. 회수는 이것만 훑는다 |
 * | `judge:done:<제출>` | 끝난 실행의 result digest. 중복과 뒤늦은 결과를 가른다 |
 * | `judge:fencing` | 토큰 카운터 |
 *
 * 완료 기록에는 만료가 있다. 메모리 구현은 그것을 영원히 들고 있었다 — 하루 뒤에 오는
 * 중복 결과는 없으므로, 그때까지만 기억하면 된다.
 */
class RedisLeaseRegistry(
    private val redis: StringRedisTemplate,
    private val clock: Clock = Clock.systemUTC(),
    private val leaseDuration: Duration = LeaseTiming.DEFAULT_LEASE,
    private val dispatchTimeout: Duration = LeaseTiming.DEFAULT_DISPATCH_TIMEOUT,
    private val prefix: String = "judge",
) : LeaseRegistry {

    private val mapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())

    private fun leaseKey(id: String) = "$prefix:lease:$id"
    private fun doneKey(id: String) = "$prefix:done:$id"
    private val expiryKey = "$prefix:expiry"
    private val fencingKey = "$prefix:fencing"

    override fun lease(origin: SubmissionQueued, executionId: String): Lease =
        checkNotNull(grant(origin, executionId, expectedToken = null)) { "새 임대는 조건이 없어 실패하지 않는다" }

    override fun reclaim(expired: Lease, executionId: String): Lease? =
        grant(expired.origin, executionId, expectedToken = expired.token)

    private fun grant(origin: SubmissionQueued, executionId: String, expectedToken: FencingToken?): Lease? {
        val expiresAt = clock.instant().plus(dispatchTimeout)
        val reply = redis.execute(
            LEASE,
            listOf(leaseKey(origin.submissionId), expiryKey, doneKey(origin.submissionId), fencingKey),
            origin.submissionId,
            executionId,
            mapper.writeValueAsString(origin),
            expiresAt.toEpochMilli().toString(),
            expectedToken?.value?.toString() ?: "",
            LEASE_TTL_SECONDS.toString(),
        )
        // Lua 의 nil 은 클라이언트에 따라 null 로도, null 하나가 든 목록으로도 온다.
        if (reply?.firstOrNull() == null) return null
        return Lease(
            submissionId = origin.submissionId,
            attempt = (reply[0] as Number).toInt(),
            token = FencingToken((reply[1] as Number).toLong()),
            expiresAt = expiresAt,
            started = false,
            executionId = executionId,
            origin = origin,
        )
    }

    override fun renew(submissionId: String, token: FencingToken): Boolean {
        val expiresAt = clock.instant().plus(leaseDuration).toEpochMilli()
        return redis.execute(RENEW, listOf(leaseKey(submissionId), expiryKey), submissionId, token.value.toString(), expiresAt.toString()) == 1L
    }

    override fun expired(): List<Lease> {
        val now = clock.instant().toEpochMilli().toDouble()
        val ids = redis.opsForZSet().rangeByScore(expiryKey, Double.NEGATIVE_INFINITY, now, 0, EXPIRED_BATCH).orEmpty()
        return ids.mapNotNull { id ->
            val hash = redis.opsForHash<String, String>().entries(leaseKey(id))
            if (hash.isEmpty()) {
                // 임대는 끝났는데 정렬 집합에만 남은 항목이다. 다음 훑기가 다시 보지 않게 치운다.
                redis.opsForZSet().remove(expiryKey, id)
                return@mapNotNull null
            }
            Lease(
                submissionId = id,
                attempt = hash.getValue("attempt").toInt(),
                token = FencingToken(hash.getValue("token").toLong()),
                expiresAt = Instant.ofEpochMilli(hash.getValue("expiresAt").toLong()),
                started = hash["started"] == "1",
                executionId = hash.getValue("executionId"),
                origin = mapper.readValue(hash.getValue("origin")),
            )
        }
    }

    override fun abandon(submissionId: String) {
        redis.execute(
            ABANDON, listOf(leaseKey(submissionId), expiryKey, doneKey(submissionId)),
            submissionId, Acceptance.ABANDONED, DONE_TTL_SECONDS.toString(),
        )
    }

    override fun accept(result: ExecutionResult): Acceptance {
        val reply = checkNotNull(
            redis.execute(
                ACCEPT, listOf(leaseKey(result.submissionId), expiryKey, doneKey(result.submissionId)),
                result.submissionId, result.fencingToken.value.toString(), result.attempt.toString(),
                result.resultDigest, DONE_TTL_SECONDS.toString(),
            ),
        )
        return when (reply[0]) {
            "ACCEPTED" -> Acceptance.Accepted(mapper.readValue(reply[1] as String))
            "DUPLICATE" -> Acceptance.Duplicate
            "COMPLETED" -> Acceptance.AlreadyCompleted
            "UNLEASED" -> Acceptance.Unleased
            "STALE" -> Acceptance.Stale(reply[1] as String)
            else -> error("임대 스크립트가 모르는 답을 냈다: $reply")
        }
    }

    private companion object {
        /** 한 번에 훑는 만료 임대 수. 회수는 몇 초마다 돌므로 밀린 것은 다음 회차가 잇는다. */
        const val EXPIRED_BATCH = 100L

        /** 임대 해시의 안전망. 회수·수락·포기가 지우지 못한 것이 있어도 영원히 남지는 않는다. */
        const val LEASE_TTL_SECONDS = 24 * 60 * 60L

        /** 완료 기록을 기억하는 기간. 이보다 늦게 오는 중복 결과는 없다. */
        const val DONE_TTL_SECONDS = 24 * 60 * 60L

        val LEASE = script<List<*>>("lease.lua")
        val RENEW = script<Long>("renew.lua")
        val ABANDON = script<Long>("abandon.lua")
        val ACCEPT = script<List<*>>("accept.lua")

        private inline fun <reified T> script(name: String): DefaultRedisScript<T> {
            val source = RedisLeaseRegistry::class.java.getResourceAsStream("/lease/$name")
                ?.bufferedReader()?.readText()
                ?: error("임대 스크립트가 없다: $name")
            return DefaultRedisScript(source, T::class.java)
        }
    }
}
