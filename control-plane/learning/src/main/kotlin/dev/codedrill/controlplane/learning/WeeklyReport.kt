package dev.codedrill.controlplane.learning

import dev.codedrill.platform.problempackage.Competency
import java.time.Instant
import java.time.LocalDate

/**
 * 주간 리포트 (PRD FR-808).
 *
 * > 일일 처방과 주간 리포트는 약점·재발·성장을 구체적인 행동으로 설명합니다.
 *
 * 그 문장의 세 단어가 세 칸이다 — [weakest]·[recurrences]·[growth]. 그리고 "구체적인
 * 행동"은 [actions] 인데, 그것은 오늘의 처방 그대로다. 리포트가 행동을 따로 지어내면
 * 처방과 리포트가 서로 다른 말을 한다.
 */
data class WeeklyReport(
    val from: LocalDate,
    val to: LocalDate,
    val activity: Activity,
    /** 지금 흔들리는 역량. */
    val weakest: List<Competency>,
    /** 전에 맞혔는데 이번 주에 다시 틀린 문제. 재발이다. */
    val recurrences: List<Recurrence>,
    /** 지난주보다 오른 역량. */
    val growth: List<Growth>,
    val actions: List<PrescribedProblem>,
    /** 다음에 다시 잴 시점. 행동 중 가장 이른 것이다. */
    val nextMeasurement: Instant,
)

data class Activity(
    val attempts: Int,
    val accepted: Int,
    val problemsSolved: Int,
    val activeDays: Int,
)

data class Recurrence(
    val problemId: String,
    /** 전에 맞힌 시각. "언제는 됐는데"의 언제다. */
    val solvedAt: Instant,
    val failedAt: Instant,
)

data class Growth(val competency: Competency, val from: Standing, val to: Standing)

/** 기초 통계 (부록 A 통계 — 정답률·주제·실수). */
data class Stats(
    val problemsAttempted: Int,
    val problemsSolved: Int,
    val submissions: Int,
    val accepted: Int,
    /** 최근 30일 판정 분포. "실수 유형"의 첫 근사다 — 오답인지 시간 초과인지 런타임 오류인지. */
    val verdicts: Map<String, Int>,
    /** 태그별 시도·정답 문제 수. */
    val byTag: Map<String, TagStat>,
)

data class TagStat(val attempted: Int, val solved: Int)
