package dev.codedrill.controlplane.workspace

import java.time.Instant
import java.util.UUID

/**
 * 사용자가 자기 입력으로 돌려 보는 실행 (기획서 부록 A 실행 도메인, PRD §5.2 테스트 패널).
 *
 * **판정이 아니다.** 제출 이력에도, 정답률에도, 역량 증거에도 들어가지 않는다. 사용자가
 * "이 입력에서 내 코드가 무엇을 내놓나"를 보려는 것이고, 그 답에는 점수가 붙지 않는다.
 *
 * 제출과 가장 크게 다른 점은 **정확히 한 번을 보장하지 않는다**는 것이다. 메시지를
 * 잃어버리면 [TrialStatus.PENDING] 인 채로 남고, 사용자는 다시 누르면 된다. 임대와
 * fencing 은 판정이 두 번 기록되는 것을 막으려고 있는 장치인데, 여기에는 기록될 판정이
 * 없다.
 */
data class TrialRun(
    val id: UUID,
    val userId: String,
    val problemId: String,
    val problemVersion: Int,
    val language: String,
    val cases: List<TrialCase>,
    val status: TrialStatus,
    val compileLog: String?,
    val results: List<TrialCaseResult>,
    val createdAt: Instant,
)

/**
 * 사용자가 적은 한 건.
 *
 * [expected] 는 없어도 된다. 기대 출력을 모르는 채로 "무엇이 나오나" 보려는 경우가
 * 실제로 더 흔하고, 그때 억지로 적게 하면 아무 값이나 넣게 된다.
 */
data class TrialCase(val args: List<Any>, val expected: Any? = null)

/**
 * 한 건의 결과.
 *
 * 기대 출력과의 대조는 여기서 하지 않는다. 무엇을 적었는지는 [TrialRun.cases] 에 그대로
 * 있고, 맞았는지는 [outcome] 이 말한다 — 기대를 적지 않은 케이스는 실행이 그것과 비교할
 * 수 없으므로 `WRONG_ANSWER` 가 되며, 화면이 그것을 "기대를 적지 않음"으로 되돌린다.
 *
 * **모르는 것을 "틀림"으로 표시하지 않는 책임은 화면에 있다.** 여기서 미리 판단해 버리면
 * 사용자가 나중에 기대를 적었을 때 되짚을 근거가 사라진다.
 */
data class TrialCaseResult(
    val index: Int,
    /** 실행이 내린 판정 이름. ACCEPTED / WRONG_ANSWER / RUNTIME_ERROR / TIME_LIMIT … */
    val outcome: String,
    /** 사용자 코드가 실제로 내놓은 값. 실행되지 못했으면 null 이다. */
    val actual: String?,
    /** 짧은 사유. 예외 종류나 한도 초과 같은 것이 온다. */
    val message: String?,
    val wallTimeMillis: Long,
    val peakMemoryBytes: Long,
)

enum class TrialStatus {
    /** 큐에 올렸다. 잃어버리면 여기서 멈춘다 — 다시 누르면 된다. */
    PENDING,
    COMPLETED,

    /** 컴파일 실패나 플랫폼 오류로 케이스를 하나도 돌리지 못했다. */
    FAILED,
}
