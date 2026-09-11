package dev.codedrill.judge.protocol

import dev.codedrill.platform.problempackage.Limits
import dev.codedrill.platform.problempackage.Signature

/**
 * 실험실 실행 (기획서 §6.4~6.6 해설·비교·실험실).
 *
 * 한 입력에 여러 풀이를 돌려 나란히 놓는다. 해설은 참조 풀이를 사용자의 입력으로 다시
 * 실행한 것이고, 비교 경기장은 거기에 내 풀이를 옆에 세운 것이며, 실험실은 입력을 바꿔
 * 가며 그것을 반복하는 것이다 — 셋이 같은 요청이다.
 *
 * **여기 실리는 참조 풀이 소스는 그 문제를 맞힌 사람에게만 나간다** (FR-214). 그 전에는
 * 제어 영역이 요청을 만들지 않는다.
 */
data class LabRequest(
    val schemaVersion: String = SCHEMA_VERSION,
    val labId: String,
    val correlationId: String,
    val problemVersionId: String,
    val packageDigest: String,
    val signature: Signature,
    val limits: Limits,
    /** 사용자가 적은 입력. 실험실의 "입력을 바꾸면"이 이것이다. */
    val args: List<Any>,
    val approaches: List<Approach>,
) {
    companion object {
        const val SCHEMA_VERSION = "1.0"

        /** 한 번에 돌릴 수 있는 풀이 수. 실행 횟수를 곱하는 기능이라 상한이 있어야 한다 (§15). */
        const val MAX_APPROACHES = 4
    }
}

/** 나란히 놓을 풀이 하나. [label] 이 화면에 보이는 이름이다. */
data class Approach(val label: String, val language: Language, val source: String)

/**
 * 실험실 결과.
 *
 * 풀이마다 **요약과 이벤트를 함께** 싣는다. 비교 경기장의 축(§6.5) 중 연산량·메모리·상태
 * 방문은 여기서 나오고, 최초 선택 차이는 두 이벤트 열을 견줘서 나온다.
 */
data class LabReport(
    val schemaVersion: String = LabRequest.SCHEMA_VERSION,
    val labId: String,
    val results: List<ApproachResult>,
)

data class ApproachResult(
    val label: String,
    val verdict: Verdict,
    /** 내놓은 값. 두 풀이가 다른 답을 냈다면 그것부터 보여야 한다. */
    val actual: String?,
    val measurements: Measurements,
    /** 이벤트 종류별 횟수. "연산량"의 첫 근사다 — 비교 몇 번, 방문 몇 번. */
    val eventCounts: Map<TraceEventType, Int>,
    /** 이벤트 열. 예산을 넘기면 잘린다. */
    val events: List<TraceEvent>,
    val truncated: Boolean,
)
