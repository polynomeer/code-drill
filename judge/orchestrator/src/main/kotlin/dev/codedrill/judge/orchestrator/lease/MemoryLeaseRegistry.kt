package dev.codedrill.judge.orchestrator.lease

import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.SubmissionQueued
import java.time.Clock
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * 프로세스 메모리의 임대. **테스트와 Redis 없는 개발용이다.**
 *
 * 재시작하면 전부 사라지고, 인스턴스가 둘이면 서로의 임대를 모른다. 토큰도 1 부터 다시
 * 센다 — 재시작 이전 워커의 결과가 새 임대보다 높은 토큰을 들고 올 수 있다. 그래서 공개
 * 환경에서는 [RedisLeaseRegistry] 를 쓴다.
 */
class MemoryLeaseRegistry(
    private val clock: Clock = Clock.systemUTC(),
    private val leaseDuration: Duration = LeaseTiming.DEFAULT_LEASE,
    private val dispatchTimeout: Duration = LeaseTiming.DEFAULT_DISPATCH_TIMEOUT,
) : LeaseRegistry {

    private val active = ConcurrentHashMap<String, Lease>()
    private val completed = ConcurrentHashMap<String, String>()
    private var nextToken = 0L

    override fun lease(origin: SubmissionQueued, executionId: String): Lease {
        completed.remove(origin.submissionId)
        return checkNotNull(active.compute(origin.submissionId) { _, previous -> next(previous, origin, executionId) })
    }

    override fun reclaim(expired: Lease, executionId: String): Lease? {
        var won: Lease? = null
        active.computeIfPresent(expired.submissionId) { _, current ->
            if (current.token != expired.token) current else next(current, current.origin, executionId).also { won = it }
        }
        return won
    }

    @Synchronized
    private fun next(previous: Lease?, origin: SubmissionQueued, executionId: String) = Lease(
        submissionId = origin.submissionId,
        attempt = (previous?.attempt ?: 0) + 1,
        token = FencingToken(++nextToken),
        // 아직 아무도 집어 들지 않았다. 여기서부터는 큐 대기이지 실행이 아니다.
        expiresAt = clock.instant().plus(dispatchTimeout),
        started = false,
        executionId = executionId,
        origin = origin,
    )

    override fun renew(submissionId: String, token: FencingToken): Boolean {
        val renewed = active.computeIfPresent(submissionId) { _, lease ->
            if (lease.token != token) lease else lease.copy(expiresAt = clock.instant().plus(leaseDuration), started = true)
        }
        return renewed != null && renewed.token == token
    }

    override fun expired(): List<Lease> {
        val now = clock.instant()
        return active.values.filter { now.isAfter(it.expiresAt) }
    }

    override fun abandon(submissionId: String) {
        completed[submissionId] = Acceptance.ABANDONED
        active.remove(submissionId)
    }

    override fun accept(result: ExecutionResult): Acceptance {
        completed[result.submissionId]?.let { digest ->
            return if (digest == result.resultDigest) Acceptance.Duplicate else Acceptance.AlreadyCompleted
        }
        val lease = active[result.submissionId] ?: return Acceptance.Unleased
        if (result.fencingToken < lease.token) {
            return Acceptance.Stale("fencing 토큰이 낮다: 받은 ${result.fencingToken.value}, 현재 ${lease.token.value}")
        }
        if (result.attempt != lease.attempt) {
            return Acceptance.Stale("attempt 가 다르다: 받은 ${result.attempt}, 현재 ${lease.attempt}")
        }
        completed[result.submissionId] = result.resultDigest
        active.remove(result.submissionId)
        return Acceptance.Accepted(lease.origin)
    }
}

/**
 * 두 시간 한계는 재는 것이 다르다.
 *
 * [DEFAULT_LEASE] 는 워커가 실행을 집어 든 뒤 다음 심장 박동까지 기다리는 시간이다. 실행이
 * 이보다 오래 걸려도 괜찮다 — 워커가 계속 박동을 보내는 한 임대는 연장된다. 이 값이 재는
 * 것은 실행 시간이 아니라 **워커의 생존**이다.
 *
 * [DEFAULT_DISPATCH_TIMEOUT] 은 임대해 놓고 아무 워커도 집어 들지 않은 채로 기다리는
 * 한계다. 브로커 큐에서 차례를 기다리는 시간은 워커 유실이 아니므로 위와 따로 둔다 —
 * 둘을 같은 값으로 묶으면, 큐가 밀렸을 뿐인 멀쩡한 제출이 유실로 오해받아 다시 돌고 결국
 * SYSTEM_ERROR 로 끝난다. 그렇다고 무한정 기다리지는 않는다. Runner 가 하나도 없으면
 * 제출은 영영 끝나지 않고 아무 데도 드러나지 않는다. 이 한계를 넘는 것은 용량 문제이며
 * §10.2 backpressure 로 다뤄야 한다는 신호다.
 */
object LeaseTiming {
    val DEFAULT_LEASE: Duration = Duration.ofMinutes(2)
    val DEFAULT_DISPATCH_TIMEOUT: Duration = Duration.ofMinutes(10)
}
