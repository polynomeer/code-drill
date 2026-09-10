package dev.codedrill.controlplane.submission.trace

import dev.codedrill.judge.protocol.TraceEvent
import java.util.UUID

/**
 * 참조 풀이와 처음 갈라진 지점 (PRD FR-805).
 *
 * > 리플레이 중 다음 상태 예측과 최초 분기 진단을 지원합니다. 선택·근거·결과가
 * > 코드·입력·이벤트 시점과 연결됩니다.
 *
 * **참조 코드는 나가지 않는다.** 나가는 것은 갈라진 그 한 이벤트를 사람 말로 옮긴
 * 한 줄뿐이다. 그 앞은 어차피 사용자 것과 같았고, 그 뒤는 알려 주면 정답이 된다.
 */
data class Divergence(
    val submissionId: UUID,
    val caseId: String,
    val outcome: DivergenceOutcome,
    /** 갈라지기 전까지 같았던 이벤트 수. */
    val sharedPrefix: Int?,
    /** 갈라진 지점의 사용자 이벤트 seq. 리플레이가 이 자리로 이동한다. */
    val divergedAtSeq: Long?,
    /** 그 이벤트의 코드 줄. 사용자 자신의 코드라 그대로 짚어 준다. */
    val sourceLine: Int?,
    /** 참조 풀이가 그 시점에 한 일, 한 줄. */
    val expectedStep: String?,
    /** 사용자가 그 시점에 한 일, 한 줄. */
    val actualStep: String?,
)

enum class DivergenceOutcome {
    /** 참조 트레이스를 아직 만들지 못했다. 요청은 나갔다. */
    PENDING,

    /** 끝까지 같았다. 같은 길로 같은 답에 이르렀다는 뜻이다. */
    SAME,

    /** 중간에 갈렸다. 여기가 짚어 줄 수 있는 유일한 지점이다. */
    DIVERGED,

    /**
     * 첫 이벤트부터 달랐다.
     *
     * **틀렸다는 뜻이 아니다.** 참조와 다른 접근이면 이벤트 열이 처음부터 다르고, 그때
     * "1번에서 갈렸습니다"는 아무것도 알려 주지 못한다. 그 사실을 그대로 말한다 —
     * 억지로 한 지점을 짚으면 사용자는 멀쩡한 첫 줄을 의심한다.
     */
    DIFFERENT_APPROACH,

    /** 참조 트레이스가 없다. 참조 풀이가 계측을 부르지 않았거나 돌지 않았다. */
    NO_REFERENCE,
}

/**
 * 이벤트를 비교 가능한 한 줄로 줄인다 (**상태 계약**).
 *
 * seq 와 logicalTime 은 뺀다 — 계측 호출 횟수가 조금만 달라도 전부 어긋나므로, 그것으로
 * 비교하면 모든 제출이 1번에서 갈린다. 남기는 것은 **무엇을, 어디에, 어떻게 했나** 셋이다.
 *
 * `sourceLine` 도 뺀다. 같은 일을 다른 줄에서 하는 것은 갈라진 것이 아니다.
 */
internal fun TraceEvent.contractKey(): String =
    "${eventType.name}|$targetRef|${after.orEmpty()}"

/** 사람이 읽을 한 줄. 화면이 "이 시점에 무엇을 했나"를 말하는 데 쓴다. */
internal fun TraceEvent.describe(): String = buildString {
    append(eventType.name)
    append(' ')
    append(targetKind.name.lowercase())
    append('[')
    append(targetRef)
    append(']')
    after?.let {
        append(" → ")
        append(it)
    }
}

/**
 * 두 이벤트 열을 견줘 처음 갈라진 자리를 찾는다.
 *
 * 길이가 다른 것도 갈라진 것이다 — 한쪽이 먼저 끝났다면 그 지점이 분기다.
 */
internal object FirstDivergence {

    data class Point(val index: Int, val userEvent: TraceEvent?, val referenceKey: String?)

    fun of(user: List<TraceEvent>, reference: List<String>): Point? {
        val shared = user.asSequence()
            .zip(reference.asSequence())
            .takeWhile { (event, key) -> event.contractKey() == key }
            .count()

        if (shared == user.size && shared == reference.size) return null

        return Point(
            index = shared,
            userEvent = user.getOrNull(shared),
            referenceKey = reference.getOrNull(shared),
        )
    }
}
