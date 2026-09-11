package dev.codedrill.controlplane.learning

import dev.codedrill.platform.problempackage.Competency
import java.time.Instant
import java.time.LocalDate

/**
 * 오늘의 처방 (PRD FR-808, 기획서 §8.1).
 *
 * > 일일 처방과 주간 리포트는 약점·재발·성장을 구체적인 행동으로 설명합니다. 추천 이유와
 * > 다음 측정 시점을 표시하며 사용자가 조정할 수 있습니다.
 *
 * 처방은 문제 목록이 아니라 **이유가 붙은** 문제 목록이다. 이유 없는 추천은 "왜 이걸
 * 풀어야 하나"에 답하지 못하고, 답하지 못하는 추천은 건너뛰어진다.
 */
data class Prescription(
    val date: LocalDate,
    val items: List<PrescribedProblem>,
    val streak: Streak,
)

data class PrescribedProblem(
    val problemId: String,
    val reason: Reason,
    /** 사람이 읽을 이유. 어느 역량, 며칠 전, 무엇을 놓쳤는지가 여기 들어간다. */
    val detail: String,
    /** 이 문제에 붙은 역량 중 이유가 가리키는 것. 없을 수 있다. */
    val competency: Competency?,
    /**
     * 다음에 다시 잴 시점.
     *
     * "풀고 나면 끝"이 아니라 "풀고 일주일 뒤 다시 본다"가 처방이다. 그 시점이 없으면
     * 복습 간격이라는 개념이 화면에 나타나지 않는다.
     */
    val nextMeasurement: Instant,
)

/**
 * 추천 이유 (기획서 §8.1 — 정답률보다 최근 오답 원인·힌트 의존도·복습 간격·전이 성과).
 *
 * 순서가 우선순위다. 위에 있는 이유가 먼저 자리를 차지한다.
 */
enum class Reason(val label: String) {
    /** 며칠 안에 틀렸고 그 뒤로 맞히지 못했다. 원인이 아직 손에 있을 때 다시 본다. */
    RECENT_FAILURE("최근에 틀린 문제"),

    /** 코칭 뒤 걸린 전이 과제. 힌트 없이 풀어야 가장 무거운 증거가 된다. */
    TRANSFER("전이 확인"),

    /** 힌트를 보고 맞힌 문제. 이번엔 없이 풀어 본다. */
    HINT_DEPENDENT("힌트 없이 다시"),

    /** 맞힌 지 복습 간격이 지났다. */
    REVIEW_DUE("복습 시점"),

    /** 약한 역량을 요구하는, 아직 안 푼 문제. */
    WEAK_COMPETENCY("약점 보완"),

    /** 선수 문제를 다 풀어 이제 열린 문제. */
    NEXT_ON_PATH("다음 단계"),
}

/**
 * 스트릭 (기획서 §8.1 — 강제보다 유연한 회복 규칙).
 *
 * **하루를 빠뜨려도 끊기지 않는다.** 이틀 연속 빠뜨려야 끊긴다. 하루 빠뜨렸다고 0 으로
 * 돌아가는 규칙은 사람을 돌아오게 하는 것이 아니라 포기하게 한다 — 그 하루가 지난 뒤에
 * 돌아올 이유가 사라지기 때문이다.
 */
data class Streak(
    /** 이어진 날 수. 빠뜨린 하루는 세지 않되 끊지도 않는다. */
    val days: Int,
    /** 오늘 무언가 했나. */
    val activeToday: Boolean,
    /** 어제를 빠뜨렸나. 참이면 오늘 아무것도 안 하면 끊긴다 — 그 사실을 미리 말해 준다. */
    val atRisk: Boolean,
)
