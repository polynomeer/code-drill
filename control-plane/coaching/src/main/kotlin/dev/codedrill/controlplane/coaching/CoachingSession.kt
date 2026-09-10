package dev.codedrill.controlplane.coaching

import dev.codedrill.platform.problempackage.Competency
import java.time.Instant
import java.util.UUID

/**
 * 코칭 세션 (PRD FR-802, 기획서 §4.3).
 *
 * > 코칭 풀이는 한 세션에 약한 역량 1~2개만 선택적으로 개입합니다. 자유 풀이를 방해하지
 * > 않으며 도움 단계를 사용자가 확인할 수 있습니다.
 *
 * 세 가지가 이 문장에서 나온다.
 *
 * - **[focus] 가 최대 두 개다.** 약한 것을 전부 건드리면 그것은 코칭이 아니라 과외이고,
 *   한 문제에서 네 가지를 고치려 들면 아무것도 고쳐지지 않는다.
 * - **관문이 아니다.** 세션을 열지 않아도 문제는 그대로 풀리고, 세션을 열어도 힌트는
 *   사용자가 **직접 눌러야** 나온다. 밀어 넣으면 자유 풀이를 방해한다.
 * - **받은 도움이 남는다.** 세션이 끝난 뒤에도 "몇 단계까지 봤는지"를 사용자가 확인할 수
 *   있어야 하고, 그 기록이 증거의 가중치를 낮춘다 (FR-806).
 */
data class CoachingSession(
    val id: UUID,
    val userId: String,
    val problemId: String,
    /** 이번 세션에 개입할 역량. 비어 있을 수 있다 — 약한 것이 없으면 도울 것도 없다. */
    val focus: List<Competency>,
    /** 지금까지 펼친 도움. 역량마다 몇 단계까지 봤는지가 여기 남는다. */
    val revealed: List<Assistance>,
    val startedAt: Instant,
    /** 끝나지 않았으면 null. 문제를 다시 열면 이 세션을 이어 쓴다. */
    val endedAt: Instant? = null,
) {
    /**
     * 이 세션에서 받은 도움의 깊이 (0.0 = 없음).
     *
     * 단계 수가 아니라 **가장 깊이 본 단계**로 잰다. 1단계를 세 역량에서 본 사람과
     * 3단계까지 파고든 사람 중 답에 가까운 것은 후자이고, 개수로 세면 그 둘이 뒤집힌다.
     */
    fun deepestLevel(): Int = revealed.maxOfOrNull { it.level } ?: 0
}

/** 도움 한 번. 사용자가 눌러서 펼친 단계 하나다. */
data class Assistance(
    val competency: Competency,
    val level: Int,
    val revealedAt: Instant,
)
