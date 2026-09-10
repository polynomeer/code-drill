package dev.codedrill.controlplane.submission.trace

import com.fasterxml.jackson.databind.ObjectMapper
import dev.codedrill.controlplane.submission.SubmissionRepository
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.ShrinkReport
import dev.codedrill.judge.protocol.ShrinkRequest
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.messaging.OutboxEvent
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * 최소 반례 (§6.3, PRD §3.4 검증군 증거).
 *
 * **떨어진 케이스가 숨은 그룹의 것이어도 돌린다.** 줄이고 나면 원소 몇 개짜리 입력이
 * 남고, 그것은 숨은 테스트 묶음을 알려 주지 않는다 — 오히려 줄이는 것 자체가 노출을
 * 줄이는 장치다. 숨은 케이스를 통째로 보여주는 것과 그 케이스가 가리키던 결함 하나를
 * 최소 형태로 보여주는 것은 다른 이야기다 (§8.3).
 *
 * 사용자가 눌러야 돈다. 판정마다 자동으로 돌리면 채점 한 번에 이 시스템에서 가장 비싼
 * 작업이 하나씩 붙고, 그중 대부분은 아무도 열어 보지 않는다.
 */
@Service
class CounterexampleService(
    private val repository: CounterexampleRepository,
    private val submissions: SubmissionRepository,
    private val packages: ProblemPackageLoader,
    private val json: ObjectMapper,
) {

    @Transactional
    fun start(userId: String, submissionId: UUID): Outcome {
        val submission = submissions.findById(submissionId)?.takeIf { it.userId == userId }
            ?: return Outcome.NotFound

        repository.find(submissionId)?.let { return Outcome.Started(it) }

        if (submission.verdict == Verdict.ACCEPTED) {
            return Outcome.Unavailable("통과한 제출에는 반례가 없다")
        }

        val used = repository.recentCount(userId, Instant.now().minus(WINDOW))
        if (used >= PER_HOUR) return Outcome.Throttled(used, PER_HOUR)

        val source = submissions.findSource(submissionId)
            ?: return Outcome.Unavailable("제출 코드가 없다")
        val pkg = runCatching { packages.load(submission.problemId) }.getOrNull()
            ?: return Outcome.Unavailable("문제를 읽지 못했다")
        val reference = packages.referenceSolution(submission.problemId)
            ?: return Outcome.Unavailable("이 문제에는 대조할 정답이 없다")

        // **어느 케이스가 떨어졌는지 여기서는 알 수 없다.** 숨은 그룹의 케이스 내역은
        // 판정 봉투에서 잘려 나가고(§8.3), 떨어지는 것은 대개 그 숨은 그룹이다. 그래서
        // 한 건을 고르는 대신 후보를 전부 보내고, 어느 것이 재현되는지는 실제로 돌려
        // 보는 쪽이 정한다.
        //
        // 작은 것부터 예산이 찰 때까지 담는다. 큰 입력에서만 재현되는 결함은 놓치지만,
        // 그런 결함은 대개 값이 아니라 시간·메모리 쪽이라 축소가 답할 문제가 아니다.
        val starts = pkg.groups.asSequence()
            .flatMap { it.cases.asSequence() }
            .map { it.args }
            .sortedBy { args -> args.sumOf { (it as? List<*>)?.size ?: 0 } }
            .runningFold(emptyList<List<Any>>()) { acc, args -> acc + listOf(args) }
            .takeWhile { picked ->
                picked.sumOf { args -> args.sumOf { (it as? List<*>)?.size ?: 1 } } <=
                    ShrinkRequest.START_BUDGET
            }
            .lastOrNull()
            .orEmpty()

        if (starts.isEmpty()) return Outcome.Unavailable("시작할 케이스가 없다")

        val shrinkId = UUID.randomUUID().toString()
        val started = repository.startWithOutbox(
            submissionId, userId,
            OutboxEvent(
                id = UUID.randomUUID(),
                aggregate = "counterexample",
                aggregateId = submissionId.toString(),
                type = SHRINK_EVENT,
                payload = json.writeValueAsString(
                    ShrinkRequest(
                        shrinkId = submissionId.toString(),
                        correlationId = shrinkId,
                        problemVersionId = pkg.problemVersionId,
                        packageDigest = pkg.packageDigest,
                        signature = pkg.manifest.signature,
                        limits = pkg.manifest.limits,
                        reference = reference,
                        language = Language.valueOf(submission.language),
                        source = source,
                        starts = starts,
                    ),
                ),
                occurredAt = Instant.now(),
            ),
        )
        if (started == 0) {
            return repository.find(submissionId)?.let { Outcome.Started(it) } ?: Outcome.NotFound
        }

        return Outcome.Started(repository.find(submissionId) ?: return Outcome.NotFound)
    }

    @Transactional
    fun completed(report: ShrinkReport) {
        val submissionId = runCatching { UUID.fromString(report.shrinkId) }.getOrNull() ?: return
        val existing = repository.find(submissionId) ?: return

        repository.complete(
            submissionId,
            existing.copy(
                status = CounterexampleStatus.of(report.status),
                message = report.message,
                args = report.args,
                actual = report.actual,
                expected = report.expected,
                originalSize = report.originalSize,
                minimalSize = report.minimalSize,
                rounds = report.rounds,
            ),
        )
    }

    fun find(userId: String, submissionId: UUID): Counterexample? =
        repository.find(submissionId)?.takeIf { it.userId == userId }

    sealed interface Outcome {
        data class Started(val counterexample: Counterexample) : Outcome
        data class Unavailable(val reason: String) : Outcome
        data class Throttled(val used: Int, val allowed: Int) : Outcome
        data object NotFound : Outcome
    }

    companion object {
        const val SHRINK_EVENT = "ShrinkRequested"

        /**
         * 한 시간에 몇 번까지.
         *
         * 아주 짜다. 한 건이 라운드마다 컴파일 두 번에 후보 수십 개를 돌리므로, 이
         * 시스템에서 한 사람이 걸 수 있는 가장 비싼 작업이다 (§10.2).
         */
        private const val PER_HOUR = 5

        private val WINDOW: Duration = Duration.ofHours(1)
    }
}
