package dev.codedrill.controlplane.workspace

import com.fasterxml.jackson.databind.ObjectMapper
import dev.codedrill.judge.protocol.MutationRequest
import dev.codedrill.platform.messaging.OutboxEvent
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import dev.codedrill.platform.problempackage.TestCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * 내 테스트의 결함 탐지력을 잰다 (PRD FR-804).
 *
 * > 사용자 테스트를 변이 구현에 실행해 mutation score와 누락 유형을 제공합니다.
 *
 * **정답과 오답 소스는 여기서 봉투에 담겨 실행 영역으로만 간다.** 제어 영역이 그것을
 * 저장하지도, 응답에 싣지도 않는다 — 저장하는 순간 그것을 읽는 조회가 생기고, 언젠가
 * 하나가 사용자에게 나간다 (§8.3).
 *
 * **기대 출력이 없는 케이스는 아예 보내지 않는다.** 입력만 넣고 무엇이 나오나 본 것은
 * 시험이 아니고, 그것으로 오답을 잡을 수는 없으므로 보내 봐야 점수만 깎는다.
 */
@Service
class MutationService(
    private val repository: MutationRepository,
    private val packages: ProblemPackageLoader,
    private val json: ObjectMapper,
    private val limits: MutationLimits,
) {

    @Transactional
    fun start(userId: String, problemId: String, cases: List<TrialCase>): Outcome {
        val tests = cases.filter { it.expected != null }
        if (tests.isEmpty()) {
            // 큐에 올려 놓고 "잴 것이 없었다"를 돌려주는 것보다 여기서 말하는 편이 낫다.
            // 사용자가 고쳐야 할 것은 실행 결과가 아니라 입력이다.
            return Outcome.Invalid("기대 출력을 적은 케이스가 최소 하나는 있어야 한다")
        }
        if (tests.size > limits.maxCases) {
            return Outcome.Invalid(
                "한 번에 ${limits.maxCases}개까지 잴 수 있다 (요청 ${tests.size}개)",
            )
        }

        val used = repository.recentCount(userId, Instant.now().minus(WINDOW))
        if (used >= limits.perHour) return Outcome.Throttled(used, limits.perHour)

        val pkg = packages.load(problemId)
        val parameters = pkg.manifest.signature.parameters
        tests.forEachIndexed { index, case ->
            CaseShape.mismatch(case.args, parameters.map { it.type })?.let {
                return Outcome.Invalid("${index + 1}번 케이스: $it")
            }
        }

        val reference = packages.referenceSolution(problemId)
            ?: return Outcome.Unavailable("이 문제에는 대조할 정답이 없다")
        val mutants = packages.mutants(problemId)
        if (mutants.isEmpty()) return Outcome.Unavailable("이 문제에는 대조할 오답이 없다")

        val id = UUID.randomUUID()
        val evaluation = MutationEvaluation(
            id = id,
            userId = userId,
            problemId = problemId,
            problemVersion = pkg.manifest.version,
            cases = tests,
            status = MutationRunStatus.PENDING,
            message = null,
            mistakenCases = emptyList(),
            outcomes = emptyList(),
            createdAt = Instant.now(),
        )

        val request = MutationRequest(
            evaluationId = id.toString(),
            correlationId = id.toString(),
            problemVersionId = pkg.problemVersionId,
            packageDigest = pkg.packageDigest,
            signature = pkg.manifest.signature,
            // 채점과 같은 제한으로 돈다. 느슨하게 주면 성능 오답이 여기서만 살아남는다.
            limits = pkg.manifest.limits,
            reference = reference,
            mutants = mutants,
            cases = tests.mapIndexed(::toTestCase),
        )

        repository.insertWithOutbox(
            evaluation,
            OutboxEvent(
                id = UUID.randomUUID(),
                aggregate = "mutation",
                aggregateId = id.toString(),
                type = MUTATION_EVENT,
                payload = json.writeValueAsString(request),
                occurredAt = Instant.now(),
            ),
        )
        return Outcome.Started(evaluation)
    }

    fun find(userId: String, id: UUID): MutationEvaluation? =
        repository.find(id)?.takeIf { it.userId == userId }

    private fun toTestCase(index: Int, case: TrialCase) = TestCase(
        id = "case-${index + 1}",
        groupId = "mutation",
        args = case.args,
        expected = case.expected,
    )

    sealed interface Outcome {
        data class Started(val evaluation: MutationEvaluation) : Outcome
        data class Invalid(val reason: String) : Outcome

        /** 요청은 맞지만 이 문제로는 잴 수 없다. 사용자가 고칠 수 있는 것이 아니다. */
        data class Unavailable(val reason: String) : Outcome
        data class Throttled(val used: Int, val allowed: Int) : Outcome
    }

    companion object {
        const val MUTATION_EVENT = "MutationQueued"

        private val WINDOW: Duration = Duration.ofHours(1)
    }
}
