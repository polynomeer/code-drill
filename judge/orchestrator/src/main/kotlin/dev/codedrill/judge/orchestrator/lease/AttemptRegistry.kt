package dev.codedrill.judge.orchestrator.lease

import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.judge.protocol.FencingToken
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * 실행 임대와 fencing 토큰을 관리한다 (기술 설계서 §4.3).
 *
 * 작업은 at-least-once 로 전달되고 워커는 언제든 사라질 수 있다. 그래서 "지금 유효한
 * 실행이 무엇인가"를 한 곳에서 정하고, 그 판단을 [FencingToken] 이라는 단조 증가 값으로
 * 표현한다. 잃어버린 줄 알았던 워커가 살아 돌아와도 토큰이 낮으면 결과가 거절된다.
 *
 * 슬라이스는 프로세스 메모리에 둔다. 운영에서는 Redis 의 원자적 INCR 또는 실행 영역
 * 자체 저장소로 옮겨야 여러 오케스트레이터 replica 사이에서도 같은 불변식이 선다.
 */
class AttemptRegistry(
    private val clock: Clock = Clock.systemUTC(),
    /**
     * 워커가 실행을 집어 든 뒤, 다음 심장 박동까지 기다리는 시간.
     *
     * 실행이 이보다 오래 걸려도 괜찮다 — 워커가 계속 박동을 보내는 한 임대는 연장된다.
     * 이 값이 재는 것은 실행 시간이 아니라 **워커의 생존**이다.
     */
    private val leaseDuration: Duration = Duration.ofMinutes(2),
    /**
     * 임대해 놓고 아무 워커도 집어 들지 않은 채로 기다리는 한계.
     *
     * 브로커 큐에서 차례를 기다리는 시간은 워커 유실이 아니다. 그래서 [leaseDuration]
     * 과 따로 둔다 — 둘을 같은 값으로 묶으면, 큐가 밀렸을 뿐인 멀쩡한 제출이 유실로
     * 오해받아 다시 돌고 결국 SYSTEM_ERROR 로 끝난다.
     *
     * 그렇다고 무한정 기다리지는 않는다. Runner 가 하나도 없으면 제출은 영영 끝나지
     * 않고, 사용자에게도 대시보드에도 아무것도 드러나지 않는다. 이 한계를 넘는 것은
     * 용량 문제이며 §10.2 backpressure 로 다뤄야 한다는 신호다.
     */
    private val dispatchTimeout: Duration = Duration.ofMinutes(10),
) {

    private val active = ConcurrentHashMap<String, Lease>()
    private val completed = ConcurrentHashMap<String, String>()

    /**
     * 새 실행을 임대한다. 같은 제출을 다시 임대하면 attempt 와 토큰이 함께 올라가고,
     * 이전 임대는 그 순간 무효가 된다.
     *
     * **지난 완료 기록을 지운다.** 중복 판정은 "같은 실행의 결과가 두 번 왔다"를 잡는
     * 장치이지, "이 제출은 이미 채점됐다"를 뜻하지 않는다. 지우지 않으면 재채점 결과가
     * 지난 판정과 내용이 같을 때 — 재채점에서는 흔한 일이다 — 중복으로 오해받아 조용히
     * 버려지고, 결과를 기다리던 재채점은 영영 끝나지 않는다.
     */
    fun lease(submissionId: String): Lease {
        completed.remove(submissionId)
        val next = active.compute(submissionId) { _, previous ->
            val attempt = (previous?.attempt ?: 0) + 1
            Lease(
                submissionId = submissionId,
                attempt = attempt,
                token = FencingToken((previous?.token?.value ?: 0) + 1),
                // 아직 아무도 집어 들지 않았다. 여기서부터는 큐 대기이지 실행이 아니다.
                expiresAt = clock.instant().plus(dispatchTimeout),
                started = false,
            )
        }
        return checkNotNull(next)
    }

    /** 임대가 만료됐는지. 만료된 임대는 재임대 대상이다 (§4.3 워커 유실). */
    fun isExpired(submissionId: String): Boolean {
        val lease = active[submissionId] ?: return false
        return clock.instant().isAfter(lease.expiresAt)
    }

    /**
     * 만료된 임대 전부.
     *
     * 워커가 죽으면 결과가 영영 오지 않고, 제출은 LEASED 에서 멈춘 채 남는다. 임대에
     * 만료를 두는 이유가 이것이므로, 만료를 **주기적으로 읽어 가는 쪽**이 있어야 한다.
     * 만료 시각만 두고 아무도 보지 않으면 필드가 하나 늘었을 뿐이다.
     */
    fun expired(): List<String> {
        val now = clock.instant()
        return active.values.filter { now.isAfter(it.expiresAt) }.map { it.submissionId }
    }

    /**
     * 임대를 연장한다 (§4.3).
     *
     * 실행을 집어 든 워커가 살아 있다고 알려 올 때만 부른다. 토큰이 현재 임대와 다르면
     * 연장하지 않는다 — 이미 무효가 된 워커가 자기 임대를 살려 두면, 재실행된 새 워커와
     * 둘이 같은 제출을 붙들고 있게 된다.
     */
    fun renew(submissionId: String, token: FencingToken): Boolean {
        val renewed = active.computeIfPresent(submissionId) { _, lease ->
            if (lease.token != token) {
                lease
            } else {
                lease.copy(expiresAt = clock.instant().plus(leaseDuration), started = true)
            }
        }
        return renewed != null && renewed.token == token
    }

    /**
     * 더 재시도하지 않고 실행을 포기한다 (§4.2 종료는 불변).
     *
     * 종료로 표시해 두면 늦게 살아 돌아온 워커의 결과가 [Acceptance.AlreadyCompleted] 로
     * 갈리고, 이미 사용자에게 보인 SYSTEM_ERROR 를 조용히 덮어쓰지 못한다.
     */
    fun abandon(submissionId: String) {
        completed[submissionId] = ABANDONED
        active.remove(submissionId)
    }

    /**
     * 도착한 결과를 받아들일지 판단한다.
     *
     * 순서가 중요하다. 중복 판정을 fencing 검사보다 먼저 해야, 같은 결과의 재전달이
     * 스테일로 잘못 기록되지 않는다.
     */
    fun accept(result: ExecutionResult): Acceptance {
        completed[result.submissionId]?.let { digest ->
            return if (digest == result.resultDigest) Acceptance.Duplicate else Acceptance.AlreadyCompleted
        }

        // 임대 기록이 없다고 버리지 않는다.
        //
        // 기록은 이 프로세스의 메모리에 있다. 오케스트레이터가 재시작했거나, 인스턴스가
        // 둘이어서 **띄운 쪽과 결과를 받은 쪽이 다르면** 기록이 없다. 버리면 그 제출은
        // 결과가 멀쩡히 도착했는데도 영영 끝나지 않는다.
        //
        // 낡은 결과가 섞여 들어올 위험은 제어 영역이 받는다 — 끝난 제출의 판정은
        // 승인된 재채점으로만 바뀐다 (SubmissionService.complete).
        val lease = active[result.submissionId] ?: return Acceptance.Unleased

        if (result.fencingToken < lease.token) {
            return Acceptance.Stale(
                "fencing 토큰이 낮다: 받은 ${result.fencingToken.value}, 현재 ${lease.token.value}",
            )
        }
        if (result.attempt != lease.attempt) {
            return Acceptance.Stale("attempt 가 다르다: 받은 ${result.attempt}, 현재 ${lease.attempt}")
        }

        completed[result.submissionId] = result.resultDigest
        active.remove(result.submissionId)
        return Acceptance.Accepted
    }

    data class Lease(
        val submissionId: String,
        val attempt: Int,
        val token: FencingToken,
        val expiresAt: Instant,
        /** 워커가 집어 들었는지. 만료의 의미가 이 값에 따라 달라진다. */
        val started: Boolean = false,
    )

    private companion object {
        /** 포기한 실행의 자리 표시. 어떤 실제 result digest 와도 같지 않다. */
        const val ABANDONED = "abandoned"
    }

    sealed interface Acceptance {
        /** 유효한 결과다. 집계로 넘긴다. */
        data object Accepted : Acceptance

        /** 같은 결과가 다시 왔다. no-op 이며 오류가 아니다 (§4.3 결과 중복). */
        data object Duplicate : Acceptance

        /** 종료된 제출에 다른 결과가 왔다. 감사 기록 대상이다. */
        data object AlreadyCompleted : Acceptance

        /** 유효하지 않은 실행에서 온 결과다. 폐기하고 감사 기록만 남긴다. */
        data class Stale(val reason: String) : Acceptance

        /**
         * 임대 기록이 없는 결과다. 넘기되 그 사실을 남긴다 (§4.3).
         *
         * 재시작했거나, 인스턴스가 둘이어서 띄운 쪽과 받은 쪽이 다르면 이렇게 된다.
         * **정상 경로에서 자주 보이면 안 된다** — 자주 보인다면 임대가 쓸모없어졌다는
         * 뜻이고, 그러면 만료로 워커 유실을 잡는 장치도 함께 무너져 있다.
         */
        data object Unleased : Acceptance
    }
}
