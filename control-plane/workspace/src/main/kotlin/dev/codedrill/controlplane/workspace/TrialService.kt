package dev.codedrill.controlplane.workspace

import com.fasterxml.jackson.databind.ObjectMapper
import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.platform.messaging.OutboxEvent
import dev.codedrill.platform.problempackage.Aggregation
import dev.codedrill.platform.problempackage.GroupPolicy
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import dev.codedrill.platform.problempackage.StopPolicy
import dev.codedrill.platform.problempackage.TestCase
import dev.codedrill.platform.problempackage.Visibility
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * 시험 실행 (기획서 부록 A 실행 도메인, PRD §5.2 테스트 패널).
 *
 * 제출하지 않고 자기 입력으로 코드를 돌려 본다. 지금까지는 **제출해야만 코드를 시험할 수
 * 있었고**, 그래서 입력 하나를 확인하려고 판정 이력에 오답이 쌓였다.
 *
 * 판정 경로와 나누는 곳이 셋이다.
 *
 * - **큐**가 다르다. 시험 실행이 몰려도 채점이 그 뒤에 줄 서지 않는다 (JudgeQueues.TRIALS).
 * - **표**가 다르다. 제출 이력·정답률·역량 증거가 이것을 세지 않는다.
 * - **오케스트레이터를 거치지 않는다.** 임대와 fencing 은 판정이 두 번 기록되는 것을
 *   막으려고 있고, 여기에는 기록될 판정이 없다.
 *
 * 같아야 하는 것도 하나 있다. **엔진과 제한은 채점과 같은 것을 쓴다.** 다르면 사용자가
 * 여기서 본 결과와 채점 결과가 갈리고, 그러면 시험 실행은 쓸모가 없다.
 */
@Service
class TrialService(
    private val repository: TrialRepository,
    private val packages: ProblemPackageLoader,
    private val json: ObjectMapper,
    private val limits: TrialLimits,
) {

    /**
     * 실행을 큐에 올린다.
     *
     * 케이스는 시그니처에 맞는지 먼저 본다. 맞지 않는 입력을 그대로 실행에 넘기면 사용자
     * 코드가 아니라 **하네스가** 깨지고, 사용자에게는 "내 코드가 틀렸다"로 보인다.
     */
    @Transactional
    fun start(
        userId: String,
        problemId: String,
        language: String,
        source: String,
        cases: List<TrialCase>,
    ): Outcome {
        // 예외로 올리지 않는다. 여기서 던지면 500 이 나가고, 사용자는 자기 입력이 잘못된
        // 것인지 서버가 고장 난 것인지 구분할 수 없다.
        if (cases.isEmpty()) return Outcome.Invalid("케이스가 최소 하나는 있어야 한다")
        if (cases.size > limits.maxCases) {
            return Outcome.Invalid(
                "한 번에 ${limits.maxCases}개까지 돌릴 수 있다 (요청 ${cases.size}개)",
            )
        }

        val used = repository.recentCount(userId, Instant.now().minus(WINDOW))
        if (used >= limits.perHour) return Outcome.Throttled(used, limits.perHour)

        val pkg = packages.load(problemId)
        val parameters = pkg.manifest.signature.parameters
        cases.forEachIndexed { index, case ->
            CaseShape.mismatch(case.args, parameters.map { it.type })?.let {
                return Outcome.Invalid("${index + 1}번 케이스: $it")
            }
        }

        val id = UUID.randomUUID()
        val trial = TrialRun(
            id = id,
            userId = userId,
            problemId = problemId,
            problemVersion = pkg.manifest.version,
            language = language,
            cases = cases,
            status = TrialStatus.PENDING,
            compileLog = null,
            results = emptyList(),
            createdAt = Instant.now(),
        )

        val request = ExecutionRequest(
            executionId = id.toString(),
            // 제출이 아니므로 제출 id 가 없다. 실행 id 를 그대로 넣어, 로그에서 이 실행을
            // 되짚을 때 빈 칸이 생기지 않게 한다.
            submissionId = id.toString(),
            attempt = 1,
            fencingToken = FencingToken(1),
            correlationId = id.toString(),
            problemVersionId = pkg.problemVersionId,
            packageDigest = pkg.packageDigest,
            language = Language.valueOf(language),
            source = source,
            signature = pkg.manifest.signature,
            limits = pkg.manifest.limits,
            groups = listOf(RequestedGroup(TRIAL_GROUP, cases.mapIndexed(::toTestCase))),
            mode = ExecutionMode.TRIAL,
        )

        repository.insertWithOutbox(
            trial, source,
            OutboxEvent(
                id = UUID.randomUUID(),
                aggregate = "trial",
                aggregateId = id.toString(),
                type = TRIAL_EVENT,
                payload = json.writeValueAsString(request),
                occurredAt = Instant.now(),
            ),
        )
        return Outcome.Started(trial)
    }

    fun find(userId: String, id: UUID): TrialRun? =
        repository.find(id)?.takeIf { it.userId == userId }

    private fun toTestCase(index: Int, case: TrialCase) = TestCase(
        id = "case-${index + 1}",
        groupId = TRIAL_GROUP.id,
        args = case.args,
        // 적지 않았으면 null 그대로 보낸다. 자리값을 넣으면 실행이 그것과 비교해
        // "틀렸다"를 만들어 내고, 타입까지 맞춘 자리값은 실제 출력과 우연히 같아진다.
        expected = case.expected,
    )

    sealed interface Outcome {
        data class Started(val trial: TrialRun) : Outcome
        data class Invalid(val reason: String) : Outcome
        data class Throttled(val used: Int, val allowed: Int) : Outcome
    }

    companion object {
        const val TRIAL_EVENT = "TrialQueued"

        private val WINDOW: Duration = Duration.ofHours(1)

        /**
         * 시험 실행 그룹.
         *
         * 사용자가 적은 입력이므로 공개다 — 숨길 것이 없다. 하나가 실패해도 나머지를 끝까지
         * 돌린다: 사용자가 보려는 것은 "어디서부터 다른가"이고, 첫 실패에서 멈추면 그 뒤를
         * 볼 수 없다.
         */
        private val TRIAL_GROUP = GroupPolicy(
            id = "trial",
            weight = 100,
            visibility = Visibility.PUBLIC,
            aggregation = Aggregation.SUM,
            stopPolicy = StopPolicy.CONTINUE,
        )
    }
}
