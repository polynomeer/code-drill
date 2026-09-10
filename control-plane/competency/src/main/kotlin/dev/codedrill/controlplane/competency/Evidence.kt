package dev.codedrill.controlplane.competency

import dev.codedrill.platform.problempackage.Competency
import java.time.Instant
import java.util.UUID

/**
 * 역량 증거 하나 (기획서 §4.2).
 *
 * **사건이지 점수가 아니다.** 사용자가 무엇을 했고 그것이 성공이었는지를 적을 뿐, 숙련도는
 * 여기에 없다 — 숙련도는 증거의 함수이며 [Mastery] 가 그때그때 계산한다.
 *
 * 그렇게 나눠 두면 계산 규칙을 고쳐도 지난 증거를 다시 해석할 필요가 없다. 규칙을 고치고
 * 다시 계산하면 끝이다.
 */
data class Evidence(
    val id: UUID,
    val userId: String,
    val competency: Competency,
    val source: EvidenceSource,
    val success: Boolean,
    /**
     * 이 증거의 무게 (§3.4).
     *
     * 지금은 전부 1.0 이다. 도움 기능(힌트·AI)이 붙으면 그것을 쓴 증거의 무게가 낮아진다 —
     * 도움을 받아 맞힌 것과 혼자 맞힌 것은 같은 증거가 아니다.
     */
    val weight: Double,
    val problemId: String,
    /** 원본을 가리키는 값. 화면이 이것으로 제출이나 응답을 연다 (FR-806). */
    val reference: String?,
    /** 사람이 읽을 한 줄. */
    val detail: String?,
    val occurredAt: Instant,
)

/**
 * 증거의 출처 (기획서 §8.2 관찰 신호).
 *
 * 원본이 세는 신호는 이보다 많다 — 힌트 의존도, 변이 탐지율, 반례 품질, 설명-코드 일치.
 * **없는 신호를 목록에 적어 두지 않는다.** 적어 두면 화면이 "아직 측정되지 않음"과 "그런
 * 신호가 없음"을 같게 보여주고, 그 둘은 다르다.
 */
enum class EvidenceSource {
    /** 판정된 제출. 문제의 역량 태그가 가리키는 역량 전부에 증거가 된다. */
    SUBMISSION,

    /** 풀이 전 질문 응답 (FR-803). 예측이 맞았는지를 본다. */
    PREQUESTION,

    /** 사용자가 직접 만든 테스트 (부록 A 실행 도메인). 무엇을 시험해야 하는지 아는가. */
    TRIAL,
}
