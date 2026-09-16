package dev.codedrill.judge.orchestrator.lease

import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.JudgeOrigin
import dev.codedrill.judge.protocol.LeasedResult
import java.time.Instant

/**
 * 실행 임대와 fencing 토큰 (기술 설계서 §4.3).
 *
 * 작업은 at-least-once 로 전달되고 워커는 언제든 사라질 수 있다. 그래서 "지금 유효한
 * 실행이 무엇인가"를 한 곳에서 정하고, 그 판단을 [FencingToken] 이라는 단조 증가 값으로
 * 표현한다. 잃어버린 줄 알았던 워커가 살아 돌아와도 토큰이 낮으면 결과가 거절된다.
 *
 * **임대는 원 요청을 품는다.** 워커가 죽으면 요청 자체가 사라지므로 다시 걸려면 원본이
 * 있어야 하고, 그것이 임대와 다른 곳에 있으면 재시작 뒤에 임대만 남고 요청이 없거나 그
 * 반대가 된다. 실제로 그랬다 — 재시작 이전에 띄운 실행이 영영 결과를 보내지 않으면 그것을
 * 회수할 기록이 없었다.
 *
 * 구현이 둘이다. [RedisLeaseRegistry] 는 재시작과 인스턴스 여럿을 넘기고,
 * [MemoryLeaseRegistry] 는 테스트와 Redis 없는 개발용이다.
 *
 * **판정기가 둘이어도 임대는 하나다.** 알고리즘 제출과 프로젝트형 제출은 같은 표에서 같은
 * 규칙으로 임대된다 — 임대가 품는 원 요청([JudgeOrigin])과 받아들일 결과([LeasedResult])의
 * 종류만 다르다.
 */
interface LeaseRegistry {

    /**
     * 새 실행을 임대한다. 같은 제출을 다시 임대하면 attempt 와 토큰이 함께 올라가고,
     * 이전 임대는 그 순간 무효가 된다.
     *
     * **지난 완료 기록을 지운다.** 중복 판정은 "같은 실행의 결과가 두 번 왔다"를 잡는
     * 장치이지, "이 제출은 이미 채점됐다"를 뜻하지 않는다. 지우지 않으면 재채점 결과가
     * 지난 판정과 내용이 같을 때 — 재채점에서는 흔한 일이다 — 중복으로 오해받아 조용히
     * 버려지고, 결과를 기다리던 재채점은 영영 끝나지 않는다.
     */
    fun lease(origin: JudgeOrigin, executionId: String): Lease

    /**
     * 만료된 임대를 새 임대로 바꾼다. **한 인스턴스만 성공한다.**
     *
     * 회수를 도는 인스턴스가 여럿이면 같은 만료를 동시에 본다. 토큰이 [expired] 의 것과
     * 같을 때만 바꾸므로, 먼저 바꾼 쪽 뒤에 온 쪽은 null 을 받고 물러난다 — 같은 제출이
     * 두 번 걸리지 않는다.
     */
    fun reclaim(expired: Lease, executionId: String): Lease?

    /**
     * 임대를 연장한다 (§4.3).
     *
     * 실행을 집어 든 워커가 살아 있다고 알려 올 때만 부른다. 토큰이 현재 임대와 다르면
     * 연장하지 않는다 — 이미 무효가 된 워커가 자기 임대를 살려 두면, 재실행된 새 워커와
     * 둘이 같은 제출을 붙들고 있게 된다.
     */
    fun renew(submissionId: String, token: FencingToken): Boolean

    /**
     * 만료된 임대 전부.
     *
     * 워커가 죽으면 결과가 영영 오지 않고, 제출은 LEASED 에서 멈춘 채 남는다. 임대에
     * 만료를 두는 이유가 이것이므로, 만료를 **주기적으로 읽어 가는 쪽**이 있어야 한다.
     * 만료 시각만 두고 아무도 보지 않으면 필드가 하나 늘었을 뿐이다.
     */
    fun expired(): List<Lease>

    /**
     * 더 재시도하지 않고 실행을 포기한다 (§4.2 종료는 불변).
     *
     * 종료로 표시해 두면 늦게 살아 돌아온 워커의 결과가 [Acceptance.AlreadyCompleted] 로
     * 갈리고, 이미 사용자에게 보인 SYSTEM_ERROR 를 조용히 덮어쓰지 못한다.
     */
    fun abandon(submissionId: String)

    /**
     * 도착한 결과를 받아들일지 판단한다.
     *
     * 순서가 중요하다. 중복 판정을 fencing 검사보다 먼저 해야, 같은 결과의 재전달이
     * 스테일로 잘못 기록되지 않는다.
     */
    fun accept(result: LeasedResult): Acceptance
}

data class Lease(
    val submissionId: String,
    val attempt: Int,
    val token: FencingToken,
    val expiresAt: Instant,
    /** 워커가 집어 들었는지. 만료의 의미가 이 값에 따라 달라진다. */
    val started: Boolean,
    /** 이 임대로 띄운 실행. 재시도 한계에서 SYSTEM_ERROR 를 낼 때 그 이름이 필요하다. */
    val executionId: String,
    /** 이 임대가 답하는 제출. 다시 걸 때 실행 요청을 여기서 다시 만든다. */
    val origin: JudgeOrigin,
)

sealed interface Acceptance {
    /** 유효한 결과다. 집계로 넘긴다. [origin] 으로 트레이스를 이어 만든다. */
    data class Accepted(val origin: JudgeOrigin) : Acceptance

    /** 같은 결과가 다시 왔다. no-op 이며 오류가 아니다 (§4.3 결과 중복). */
    data object Duplicate : Acceptance

    /** 종료된 제출에 다른 결과가 왔다. 감사 기록 대상이다. */
    data object AlreadyCompleted : Acceptance

    /** 유효하지 않은 실행에서 온 결과다. 폐기하고 감사 기록만 남긴다. */
    data class Stale(val reason: String) : Acceptance

    /**
     * 임대 기록이 없는 결과다. 넘기되 그 사실을 남긴다 (§4.3).
     *
     * Redis 임대에서는 임대가 만료 뒤 치워졌거나 저장소가 비워진 경우다. **정상 경로에서
     * 자주 보이면 안 된다** — 자주 보인다면 임대가 쓸모없어졌다는 뜻이고, 그러면 만료로
     * 워커 유실을 잡는 장치도 함께 무너져 있다.
     */
    data object Unleased : Acceptance

    companion object {
        /** 포기한 실행의 자리 표시. 어떤 실제 result digest 와도 같지 않다. */
        const val ABANDONED = "abandoned"
    }
}
