package dev.codedrill.controlplane.competency

import dev.codedrill.platform.problempackage.Competency
import dev.codedrill.platform.common.LearningRhythm
import dev.codedrill.platform.problempackage.CompetencyGroup
import java.time.Instant

/**
 * 역량 하나의 현재 상태 (PRD §3.4, FR-806).
 *
 * > 표시는 역량별 **Level + Confidence + Evidence** 로 구성합니다. 단일 백분율을 피하고,
 * > 표본 부족·힌트 사용·AI 도움을 신뢰도와 증거 가중치에 반영합니다.
 *
 * 셋을 한 숫자로 합치지 않는다. "72%" 는 세 번 풀어 두 번 맞힌 사람과 백 번 풀어 일흔두 번
 * 맞힌 사람을 같게 보이게 하는데, 그 둘에게 필요한 다음 행동은 정반대다.
 */
data class Mastery(
    val competency: Competency,
    val group: CompetencyGroup,
    val level: MasteryLevel,
    val confidence: Confidence,
    /** 이 판단이 선 증거의 수. 화면이 "무엇에 근거했나"를 말할 수 있어야 한다. */
    val evidenceCount: Int,
    val successCount: Int,
)

/**
 * 숙련도 등급.
 *
 * 백분율이 아니라 등급인 이유는 정밀해 보이는 숫자가 **정밀하지 않기** 때문이다. 증거
 * 다섯 개로 계산한 68.3% 는 소수점 아래가 뜻하는 바가 없다.
 */
enum class MasteryLevel {
    /** 증거가 없다. **모르는 것을 낮은 점수로 표시하지 않는다** (FR-801). */
    UNMEASURED,

    /** 성공보다 실패가 많거나 비슷하다. */
    DEVELOPING,

    /** 대체로 해낸다. */
    PROFICIENT,

    /** 거의 놓치지 않는다. */
    STRONG,
}

/**
 * 신뢰도 (§3.4 표본 부족 반영).
 *
 * 등급과 **따로** 낸다. 증거 두 개로 얻은 STRONG 과 서른 개로 얻은 STRONG 은 같은 등급이지만
 * 같은 사실이 아니고, 그 차이를 감추면 진단이 거짓말이 된다.
 */
enum class Confidence { NONE, LOW, MEDIUM, HIGH }

object MasteryProjection {

    /**
     * 증거를 역량별 상태로 바꾼다.
     *
     * **모든 역량을 돌려준다.** 증거가 없는 역량도 `UNMEASURED` 로 낸다 — 목록에서 빼면
     * 화면은 "측정하지 않았다"를 말할 수 없고, 사용자는 자기가 무엇을 아직 보이지 않았는지
     * 모른다 (FR-801 — 진단 미완료를 명시한다).
     */
    fun of(evidence: List<Evidence>, now: Instant = Instant.now()): List<Mastery> {
        // "지금" 이후의 증거는 없는 것으로 본다. 그래야 과거 시점의 지도를 같은 함수로
        // 그릴 수 있다 — 주간 리포트가 "지난주보다"를 말하는 방법이다.
        val byCompetency = evidence.filter { it.occurredAt <= now }.groupBy { it.competency }
        val recentSince = now.minus(LearningRhythm.RECENCY_WINDOW)

        return Competency.entries.map { competency ->
            val rows = byCompetency[competency].orEmpty()
            val weight = rows.sumOf { it.weight }
            val achieved = rows.filter { it.success }.sumOf { it.weight }

            Mastery(
                competency = competency,
                group = competency.group,
                level = levelOf(weight, achieved),
                confidence = confidenceOf(
                    recent = rows.count { it.occurredAt >= recentSince },
                    total = rows.size,
                ),
                evidenceCount = rows.size,
                successCount = rows.count { it.success },
            )
        }
    }

    /**
     * 등급 경계.
     *
     * 가중 성공률로 가른다. 경계값은 저작자 판단이며, 바꾸면 지난 증거를 그대로 두고
     * 다시 계산하면 된다 — 그러라고 증거만 저장한다.
     */
    private fun levelOf(weight: Double, achieved: Double): MasteryLevel = when {
        weight <= 0.0 -> MasteryLevel.UNMEASURED
        achieved / weight < DEVELOPING_BELOW -> MasteryLevel.DEVELOPING
        achieved / weight < STRONG_AT -> MasteryLevel.PROFICIENT
        else -> MasteryLevel.STRONG
    }

    /**
     * 최근 표본 수로 정한다.
     *
     * 3단계에서는 표본 수만 봤다 — "언제부터 오래된 것인가"를 정할 근거가 없어서였다. 6단계에서
     * 복습 간격이 정해졌고, 그 네 배([LearningRhythm.RECENCY_WINDOW])보다 오래된 증거는
     * 신뢰도에 세지 않는다. 한 달 전에 열 번 맞힌 것으로 오늘 "근거 충분"이라 말하면 그
     * 사이에 잊었는지 아무도 모른다.
     *
     * **등급은 그대로 둔다.** 오래된 증거도 그 사람이 한 번은 해냈다는 사실이고, 그것을
     * 지우면 지도가 매달 백지가 된다. 낡는 것은 등급이 아니라 그 등급을 믿을 근거다.
     * 증거는 있는데 전부 오래됐으면 NONE 이 아니라 LOW 다 — "재지 않았다"와 "오래됐다"는
     * 다른 사실이다.
     */
    private fun confidenceOf(recent: Int, total: Int): Confidence = when {
        total == 0 -> Confidence.NONE
        recent < LOW_BELOW -> Confidence.LOW
        recent < HIGH_AT -> Confidence.MEDIUM
        else -> Confidence.HIGH
    }

    private const val DEVELOPING_BELOW = 0.5
    private const val STRONG_AT = 0.85
    private const val LOW_BELOW = 3
    private const val HIGH_AT = 10
}
