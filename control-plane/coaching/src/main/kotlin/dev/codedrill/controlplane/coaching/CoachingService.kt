package dev.codedrill.controlplane.coaching

import dev.codedrill.platform.problempackage.Competency
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 코칭 (PRD FR-802).
 *
 * > 코칭 풀이는 한 세션에 약한 역량 1~2개만 선택적으로 개입합니다. 자유 풀이를 방해하지
 * > 않으며 도움 단계를 사용자가 확인할 수 있습니다.
 *
 * **밀지 않고 당긴다.** 이 서비스는 아무것도 먼저 보내지 않는다. 세션을 여는 것도,
 * 단계를 펼치는 것도 사용자가 누른다. 열어 두기만 해도 힌트가 보이면 그것은 "선택적
 * 개입"이 아니라 그냥 힌트가 붙은 문제이고, 그 뒤의 제출은 아무것도 재지 못한다.
 */
@Service
class CoachingService(
    private val repository: CoachingRepository,
    private val packages: ProblemPackageLoader,
    private val ladder: HintLadder,
    private val diagnosis: Diagnosis = Diagnosis.NONE,
) {

    /**
     * 세션을 연다. 이미 열려 있으면 그것을 돌려준다.
     *
     * 문제를 다시 열 때마다 새 세션을 만들면 "이 문제에서 몇 단계까지 봤나"가 세션마다
     * 흩어지고, 사용자는 한 문제를 놓고 자기가 받은 도움을 한눈에 볼 수 없다.
     */
    @Transactional
    fun open(userId: String, problemId: String): CoachingSession {
        repository.open(userId, problemId)?.let { return it }

        val competencies = runCatching { packages.load(problemId).catalog.competencies }
            .getOrDefault(emptyList())

        // **도울 수 있는 것 중에서** 약한 것을 고른다. 사다리가 빈 역량을 초점으로
        // 잡으면 세션이 "이것을 돕겠다"고 말해 놓고 아무것도 내놓지 못하는데, 그것은
        // 도움이 없는 것보다 나쁘다 — 사용자는 버튼을 누르며 기다린다.
        val session = CoachingSession(
            id = UUID.randomUUID(),
            userId = userId,
            problemId = problemId,
            focus = diagnosis.weakest(userId, ladder.coachable(problemId, competencies), FOCUS_LIMIT),
            revealed = emptyList(),
            startedAt = Instant.now(),
        )
        repository.insert(session)
        return session
    }

    /**
     * 다음 단계를 펼친다.
     *
     * **한 단계씩만 나간다.** 목록을 통째로 내려보내고 화면에서 가리면, 개발자 도구를 열
     * 줄 아는 사람에게는 도움이 아니라 그냥 정답이다. 그 사람의 증거만 조용히 부풀려진다.
     *
     * 초점 밖의 역량은 펼치지 않는다 — 그것이 "1~2개만 선택적으로 개입"의 뜻이다.
     */
    @Transactional
    fun reveal(userId: String, id: UUID, competency: Competency): Outcome {
        val session = repository.find(id)?.takeIf { it.userId == userId }
            ?: return Outcome.NotFound
        if (session.endedAt != null) return Outcome.Closed
        if (competency !in session.focus) return Outcome.OutOfFocus

        val steps = ladder.of(session.problemId, competency)
        val seen = session.revealed.count { it.competency == competency }
        if (seen >= steps.size) return Outcome.Exhausted(steps.size)

        val revealed = session.revealed + Assistance(competency, seen + 1, Instant.now())
        repository.replaceRevealed(id, revealed)
        return Outcome.Revealed(steps[seen], session.copy(revealed = revealed))
    }

    /**
     * 이 세션에서 지금까지 펼친 것들.
     *
     * 다시 볼 수 있어야 한다 — 한 번 보고 잊어버린 힌트를 다시 보려고 다음 단계를 펼치면,
     * 사용자는 필요하지도 않은 도움을 받고 그만큼 증거가 가벼워진다.
     */
    fun revealedTexts(session: CoachingSession): List<Pair<Assistance, Hint>> =
        session.revealed.mapNotNull { assistance ->
            ladder.of(session.problemId, assistance.competency)
                .getOrNull(assistance.level - 1)
                ?.let { assistance to it }
        }

    /** 역량별로 사다리가 몇 단이나 남았는지. 텍스트는 주지 않는다. */
    fun remaining(session: CoachingSession): Map<Competency, Int> =
        session.focus.associateWith { competency ->
            val total = ladder.of(session.problemId, competency).size
            (total - session.revealed.count { it.competency == competency }).coerceAtLeast(0)
        }

    @Transactional
    fun close(userId: String, id: UUID): Boolean {
        val session = repository.find(id)?.takeIf { it.userId == userId } ?: return false
        return repository.end(session.id) > 0
    }

    fun find(userId: String, id: UUID): CoachingSession? =
        repository.find(id)?.takeIf { it.userId == userId }

    /**
     * 이 문제에서 받은 가장 깊은 도움. 제출 증거의 가중치가 이 값을 본다 (FR-806).
     *
     * 세션이 닫혔어도 센다. 3단계까지 보고 닫은 뒤 제출하면 무게가 온전해지는 구멍을
     * 두면, 그 구멍을 아는 사람의 숙련도만 높아진다.
     */
    fun helpLevel(userId: String, problemId: String): Int = repository.deepestHelp(userId, problemId)

    sealed interface Outcome {
        data class Revealed(val hint: Hint, val session: CoachingSession) : Outcome
        data class Exhausted(val total: Int) : Outcome
        data object OutOfFocus : Outcome
        data object Closed : Outcome
        data object NotFound : Outcome
    }

    private companion object {
        /**
         * 한 세션에 개입할 역량 수의 상한 (FR-802 "1~2개").
         *
         * 상수로 둔다. 약한 것을 전부 건드리면 그것은 코칭이 아니라 과외이고, 한 문제에서
         * 네 가지를 고치려 들면 아무것도 고쳐지지 않는다.
         */
        const val FOCUS_LIMIT = 2
    }
}
