package dev.codedrill.controlplane.coaching

import dev.codedrill.platform.problempackage.Competency
import dev.codedrill.platform.problempackage.ProblemCatalog
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 전이 확인 (PRD FR-807).
 *
 * > 코칭 후 설명 과제와 변형 문제로 전이를 확인합니다. 힌트·AI 없이 완료한 결과를 가장
 * > 높은 가중치의 증거로 반영합니다.
 *
 * 이 흐름이 원본이 MVP 성공 조건으로 적어 둔 문장(기획서 §14.1)을 그대로 잰다 —
 * "코칭 세션 후 힌트 없는 변형 문제 성과가 개선된다". **잴 수 있게 만드는 것이 이 기능의
 * 목적이고, 재지 못하면 코칭이 효과가 있는지 아무도 모른다.**
 */
@Service
class TransferService(
    private val repository: TransferRepository,
    private val sessions: CoachingRepository,
    private val packages: ProblemPackageLoader,
    private val candidates: CoachableProblems = CoachableProblems.NONE,
    private val solved: SolvedProblems = SolvedProblems.NONE,
    private val signals: TransferSignals = TransferSignals.NONE,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 세션에 변형 문제를 하나 붙인다. 이미 붙어 있으면 그것을 돌려준다.
     *
     * 고르는 기준은 **코칭한 역량을 같이 요구하는가**다. 태그가 같은 문제를 고르면 같은
     * 자료구조를 쓰는 문제가 오는데, 전이는 도구가 아니라 **생각**이 옮겨졌는지를 묻는
     * 것이라 그 둘이 다르다.
     */
    @Transactional
    fun assign(userId: String, sessionId: UUID): Outcome {
        val session = sessions.find(sessionId)?.takeIf { it.userId == userId }
            ?: return Outcome.NotFound
        repository.ofSession(sessionId)?.let { return Outcome.Assigned(it) }

        // 도움을 하나도 받지 않았으면 확인할 전이가 없다. 스스로 푼 것은 이미 그 자체로
        // 증거이고, 그 위에 과제를 얹으면 안 받아도 될 숙제가 된다.
        if (session.revealed.isEmpty()) return Outcome.NothingToTransfer

        val target = pick(userId, session) ?: return Outcome.NoTarget

        val task = TransferTask(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            userId = userId,
            sourceProblemId = session.problemId,
            targetProblemId = target,
            explanation = null,
            status = TransferStatus.ASSIGNED,
            helpLevel = null,
            createdAt = Instant.now(),
        )
        repository.insert(task)
        return Outcome.Assigned(task)
    }

    /**
     * 설명 과제를 낸다.
     *
     * **이 글은 아직 증거가 아니다.** 채점할 방법이 없기 때문이다 — 무엇을 써도 "썼다"
     * 밖에 말할 수 없고, 그것으로 설명 역량을 STRONG 으로 올리면 지도가 거짓말을 한다.
     * 여기 두는 이유는 **다음 문제로 넘어가기 전에 자기 말로 정리하게** 하려는 것이고,
     * 그 효과는 뒤따르는 변형 문제의 결과로 드러난다.
     *
     * 글을 채점하는 것은 AI 모드가 붙을 때다 (docs/feature-roadmap.md 10단계).
     */
    @Transactional
    fun explain(userId: String, id: UUID, text: String): Outcome {
        val task = repository.find(id)?.takeIf { it.userId == userId } ?: return Outcome.NotFound
        if (task.status != TransferStatus.ASSIGNED) return Outcome.AlreadyExplained
        if (text.trim().length < MIN_EXPLANATION) return Outcome.TooShort(MIN_EXPLANATION)

        repository.explain(id, text.trim())
        return Outcome.Assigned(task.copy(explanation = text.trim(), status = TransferStatus.EXPLAINED))
    }

    /**
     * 판정이 끝났다. 이 문제로 걸린 과제가 있으면 닫는다.
     *
     * 전이 증거가 되는 조건은 셋이 **모두** 맞을 때다 — 설명을 먼저 썼고, 통과했고,
     * 그 문제에서 힌트를 하나도 안 봤다. 하나라도 어긋나면 과제는 끝나되 증거가 되지
     * 않는다. 실패가 아니라 **그 결과가 "옮겨졌다"를 말해 주지 못할 뿐**이다.
     */
    @Transactional
    fun judged(userId: String, problemId: String, accepted: Boolean) {
        val task = repository.pending(userId, problemId) ?: return
        val helpLevel = sessions.deepestHelp(userId, problemId)

        val verified = accepted &&
            helpLevel == 0 &&
            task.status == TransferStatus.EXPLAINED

        // 통과하지 못한 것은 아직 끝난 것이 아니다. 다시 풀 수 있어야 한다 — 한 번
        // 틀렸다고 과제를 닫으면 전이를 확인할 기회가 그 한 번뿐이 된다.
        if (!accepted) return

        val status = if (verified) TransferStatus.VERIFIED else TransferStatus.UNVERIFIED
        if (repository.complete(task.id, status, helpLevel) == 0) return

        if (!verified) return
        runCatching {
            signals.transferred(
                userId = userId,
                problemId = problemId,
                taskId = task.id.toString(),
                competencies = sharedCompetencies(task.sourceProblemId, problemId),
            )
        }.onFailure { log.warn("전이를 기록하지 못했다: {} ({})", task.id, it.message) }
    }

    fun find(userId: String, id: UUID): TransferTask? =
        repository.find(id)?.takeIf { it.userId == userId }

    fun ofSession(userId: String, sessionId: UUID): TransferTask? =
        repository.ofSession(sessionId)?.takeIf { it.userId == userId }

    /**
     * 변형 문제를 고른다.
     *
     * 거를 것이 셋이다 — 아직 안 풀었고, 과제로 받은 적이 없고, 그 문제 자신이 아니다.
     * 남은 것 중에서 고르는 순서는 이렇다.
     *
     * 1. **이 문제를 선수로 적어 둔 문제.** 저작자가 "저것을 알고 나서 푸는 문제"라고
     *    직접 적은 관계라, 역량이 겹친다는 사실보다 훨씬 강한 신호다.
     * 2. 코칭한 역량을 많이 겹치는 문제.
     *
     * 겹치는 역량만으로 고르다가 두 수의 합에서 **이분 그래프 판정**이 나온 적이 있다.
     * 둘 다 모델링과 구현력을 요구하는 것은 맞지만, 해시맵으로 짝을 찾던 생각이 그래프
     * 색칠로 옮겨 갈 리는 없다. 역량 태그는 **무엇을 재는지**를 말할 뿐 **무엇이 이어지는지**는
     * 말해 주지 않는다.
     */
    private fun pick(userId: String, session: CoachingSession): String? {
        val focus = session.focus.toSet()
        if (focus.isEmpty()) return null

        val exclude = solved.solvedBy(userId) + repository.assignedTargets(userId) + session.problemId

        val ranked = catalogs()
            .filterKeys { it !in exclude }
            .map { (id, catalog) ->
                Candidate(
                    id = id,
                    buildsOnSource = session.problemId in catalog.prerequisites,
                    sharedFocus = catalog.competencies.count { it in focus },
                )
            }
            .filter { it.buildsOnSource || it.sharedFocus > 0 }

        return ranked.sortedWith(
            compareByDescending<Candidate> { it.buildsOnSource }
                .thenByDescending { it.sharedFocus }
                // 같은 상황에서 같은 문제가 나오게 한다. 무작위로 고르면 "왜 이 문제인가"에
                // 답할 수 없고, 사용자가 새로고침으로 마음에 드는 문제를 뽑게 된다.
                .thenBy { it.id },
        ).firstOrNull()?.id
    }

    private data class Candidate(
        val id: String,
        val buildsOnSource: Boolean,
        val sharedFocus: Int,
    )

    /** 두 문제가 함께 요구하는 역량. 전이 증거가 붙는 자리다. */
    private fun sharedCompetencies(source: String, target: String): List<Competency> {
        val a = catalogOf(source)?.competencies.orEmpty().toSet()
        val b = catalogOf(target)?.competencies.orEmpty()
        return b.filter { it in a }.ifEmpty { b }
    }

    private fun catalogs(): Map<String, ProblemCatalog> =
        candidates.ids().mapNotNull { id -> catalogOf(id)?.let { id to it } }.toMap()

    private fun catalogOf(problemId: String): ProblemCatalog? =
        runCatching { packages.load(problemId).catalog }.getOrNull()

    sealed interface Outcome {
        data class Assigned(val task: TransferTask) : Outcome

        /** 도움을 하나도 받지 않았다. 확인할 전이가 없다. */
        data object NothingToTransfer : Outcome

        /** 이 역량을 같이 요구하면서 아직 안 푼 문제가 없다. */
        data object NoTarget : Outcome
        data object AlreadyExplained : Outcome
        data class TooShort(val required: Int) : Outcome
        data object NotFound : Outcome
    }

    private companion object {
        /**
         * 설명의 최소 길이.
         *
         * 길이로 내용을 잴 수는 없다. 다만 한 줄도 쓰지 않고 넘어가는 것을 막을 수는
         * 있고, 이 과제의 목적이 **넘어가기 전에 멈춰 세우는 것**이라 그것으로 족하다.
         */
        const val MIN_EXPLANATION = 40
    }
}
