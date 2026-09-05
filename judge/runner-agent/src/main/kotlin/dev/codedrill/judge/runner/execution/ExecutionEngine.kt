package dev.codedrill.judge.runner.execution

import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.Measurements
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.TestCaseResult
import dev.codedrill.judge.protocol.TraceCapture
import dev.codedrill.judge.protocol.TraceEvent
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.judge.runner.execution.adapter.RuntimeAdapter
import dev.codedrill.judge.runner.execution.adapter.SandboxProtocol
import dev.codedrill.judge.runner.execution.adapter.parseTraceEvent
import dev.codedrill.judge.runner.execution.sandbox.Sandbox
import dev.codedrill.judge.runner.execution.sandbox.SandboxRun
import dev.codedrill.judge.runner.execution.sandbox.SandboxSpec
import dev.codedrill.platform.problempackage.StopPolicy
import dev.codedrill.platform.problempackage.TestCase
import dev.codedrill.platform.problempackage.ValueType
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory

/**
 * 실행 파이프라인 (기술 설계서 §5.3).
 *
 * ```
 * prepare → compile → execute → check → cleanup
 * ```
 *
 * 언어별로 다른 것은 [RuntimeAdapter] 가, 격리는 [Sandbox] 가 맡는다. 이 클래스에는
 * **언어와 격리 방식에 무관한 판정 규칙만** 남는다. 그래야 언어를 늘리거나 샌드박스를
 * 바꿔도 같은 코드가 같은 판정을 받는다 (§12.1 재현성).
 *
 * 단계별 실패를 서로 다른 판정으로 갈라 낸다. 컴파일 실패는 테스트를 한 건도 돌리지
 * 않고 종료하고, 플랫폼 장애는 사용자 실패로 덮지 않는다 (§4.4).
 */
class ExecutionEngine(
    private val adapters: Map<Language, RuntimeAdapter>,
    private val sandboxes: (Language) -> Sandbox,
    private val workRoot: Path? = null,
) {

    fun execute(request: ExecutionRequest): ExecutionResult {
        // 심볼릭 링크를 풀어 실제 경로로 만든다. macOS 의 임시 디렉터리는 /var/folders 로
        // 보이지만 실제로는 /private/var/folders 이고, 컨테이너 런타임의 파일 공유 설정은
        // 실제 경로 기준이라 링크된 경로로 마운트하면 조용히 비어 있는 디렉터리가 된다.
        val sandboxDir = (
            workRoot?.resolve(request.executionId)?.also { it.createDirectories() }
                ?: createTempDirectory("codedrill-exec-")
            ).toRealPath()

        return try {
            runPipeline(request, sandboxDir)
        } catch (e: Exception) {
            // 여기까지 온 예외는 사용자 코드가 아니라 우리 쪽 문제다.
            terminal(request, Verdict.SYSTEM_ERROR, compileLog = null, reason = e.message)
        } finally {
            sandboxDir.toFile().deleteRecursively()
        }
    }

    private fun runPipeline(request: ExecutionRequest, sandboxDir: Path): ExecutionResult {
        val adapter = adapters[request.language]
            ?: error("지원하지 않는 언어다: ${request.language}")

        // prepare
        val sourceDir = sandboxDir.resolve("src").also { it.createDirectories() }
        val outputDir = sandboxDir.resolve("out").also { it.createDirectories() }
        adapter.prepare(request, sourceDir)

        // compile
        when (val outcome = adapter.compile(sourceDir, outputDir)) {
            is RuntimeAdapter.CompileOutcome.Failure ->
                return terminal(request, Verdict.COMPILE_ERROR, outcome.log, reason = null)
            RuntimeAdapter.CompileOutcome.Success -> Unit
        }

        // execute: 그룹마다 별도 샌드박스를 띄운다.
        //
        // stop_policy 는 그룹 단위 정책이다 (§6.2). 한 샌드박스에서 전부 돌리면 앞 그룹의
        // FAIL_FAST 나 TIME_LIMIT 이 뒤 그룹까지 통째로 날려버려, 채점하지 못한 그룹과
        // 정책대로 멈춘 그룹을 구분할 수 없게 된다.
        val executedGroups = when (request.mode) {
            ExecutionMode.JUDGE -> request.groups
            // 리플레이는 실행 하나를 되짚는 것이다. 숨은 입력의 상태 변화를 보여주면
            // 테스트가 그대로 샌다 (§7.1).
            ExecutionMode.TRACE -> request.groups
                .filter { it.policy.exposesInput }
                .take(1)
                .map { it.copy(cases = it.cases.take(1)) }
        }

        val events = mutableListOf<TraceEvent>()
        val results = mutableListOf<TestCaseResult>()
        val sandbox = sandboxes(request.language)

        for (group in executedGroups) {
            val run = runGroup(request, adapter, sandbox, group, sourceDir, outputDir, events)
            // 케이스를 하나도 시작하지 못했다. 컴파일 단계가 잡지 못한 같은 부류의 실패다.
            run.fatal?.let { return terminal(request, Verdict.COMPILE_ERROR, it, reason = null) }
            results += toCaseResults(run, group, request.limits.outputBytes)
        }

        if (request.mode == ExecutionMode.TRACE) return traceResult(request, results, events)

        return ExecutionResult(
            executionId = request.executionId,
            submissionId = request.submissionId,
            attempt = request.attempt,
            fencingToken = request.fencingToken,
            problemVersionId = request.problemVersionId,
            terminalVerdict = null,
            compileLog = null,
            cases = results,
            resultDigest = digestOf(results),
        )
    }

    private fun runGroup(
        request: ExecutionRequest,
        adapter: RuntimeAdapter,
        sandbox: Sandbox,
        group: RequestedGroup,
        sourceDir: Path,
        outputDir: Path,
        events: MutableList<TraceEvent>,
    ): SandboxRun {
        val byId = group.cases.associateBy { it.qualifiedId() }
        val outputLimit = request.limits.outputBytes
        val memoryMb = (request.limits.memoryMb * group.policy.limitMultiplier.memory).toInt()

        val spec = SandboxSpec(
            command = adapter.command(sourceDir, outputDir, group.policy.id, memoryMb),
            workDir = sourceDir.parent,
            readOnlyPaths = adapter.readOnlyPaths(),
            env = DETERMINISM_ENV,
            memoryMb = memoryMb,
            perCaseTimeoutMillis =
                (request.limits.timeMillis * group.policy.limitMultiplier.time).toLong(),
            outputByteLimit = outputLimit,
        )

        return sandbox.run(
            spec = spec,
            caseIds = group.cases.map { it.qualifiedId() },
            onEvent = { _, line -> parseTraceEvent(line)?.let(events::add) },
        ) { caseId, outcome ->
            // 기대 출력은 샌드박스에 넘기지 않으므로 정답 비교는 여기서만 일어난다.
            val passed = verdictOf(outcome, byId.getValue(caseId), outputLimit) == Verdict.ACCEPTED
            passed || group.policy.stopPolicy != StopPolicy.FAIL_FAST
        }
    }

    private fun toCaseResults(
        run: SandboxRun,
        group: RequestedGroup,
        outputLimit: Long,
    ): List<TestCaseResult> {
        val byId = group.cases.associateBy { it.qualifiedId() }
        return run.outcomes.mapNotNull { (caseId, outcome) ->
            // 실행되지 못한 케이스는 결과에 싣지 않는다. 오답으로 만들면 거짓말이 된다.
            if (outcome is CaseOutcome.NotRun) return@mapNotNull null
            val case = byId.getValue(caseId)
            TestCaseResult(
                caseId = case.id,
                groupId = case.groupId,
                verdict = verdictOf(outcome, case, outputLimit),
                measurements = measurementsOf(outcome),
                message = messageOf(outcome, group.policy.exposesInput),
            )
        }
    }

    /** 기대 출력과 실제 출력을 맞춘다 (§5.3 check). 기본 checker 는 정확 일치다. */
    private fun verdictOf(outcome: CaseOutcome, case: TestCase, outputLimit: Long): Verdict =
        when (outcome) {
            is CaseOutcome.Completed -> when {
                // 출력 한도 초과는 정답 여부보다 먼저 판정한다. 한도를 넘긴 실행은 결과를
                // 신뢰할 수 없다.
                outcome.userOutputBytes > outputLimit -> Verdict.OUTPUT_LIMIT
                outcome.output == SandboxProtocol.encode(case.expectedType(), case.expected) ->
                    Verdict.ACCEPTED
                else -> Verdict.WRONG_ANSWER
            }
            // 하네스가 최상위 예외를 잡으므로 메모리 초과도 여기로 온다. 자원 초과를
            // 사용자 코드 버그로 뭉뚱그리면 사용자가 잘못된 곳을 고치게 된다.
            is CaseOutcome.Threw ->
                if (outcome.exceptionClass in MEMORY_EXCEPTIONS) {
                    Verdict.MEMORY_LIMIT
                } else {
                    Verdict.RUNTIME_ERROR
                }
            CaseOutcome.TimedOut -> Verdict.TIME_LIMIT
            CaseOutcome.MemoryExceeded -> Verdict.MEMORY_LIMIT
            CaseOutcome.OutputExceeded -> Verdict.OUTPUT_LIMIT
            is CaseOutcome.Broken -> Verdict.SYSTEM_ERROR
            CaseOutcome.NotRun -> Verdict.SYSTEM_ERROR
        }

    private fun measurementsOf(outcome: CaseOutcome): Measurements = when (outcome) {
        is CaseOutcome.Completed -> Measurements(
            cpuTimeMillis = outcome.elapsedMillis,
            wallTimeMillis = outcome.elapsedMillis,
            peakMemoryBytes = outcome.heapBytes,
            outputBytes = outcome.userOutputBytes,
        )
        is CaseOutcome.Threw -> Measurements(
            cpuTimeMillis = outcome.elapsedMillis,
            wallTimeMillis = outcome.elapsedMillis,
            peakMemoryBytes = 0,
            outputBytes = 0,
        )
        else -> Measurements.NONE
    }

    /** 숨은 그룹에서는 케이스 내용을 유추할 단서를 담지 않는다 (§8.3). */
    private fun messageOf(outcome: CaseOutcome, exposesInput: Boolean): String? = when (outcome) {
        is CaseOutcome.Threw -> outcome.exceptionClass
        is CaseOutcome.Broken -> if (exposesInput) outcome.reason else "실행을 완료하지 못했다"
        else -> null
    }

    /**
     * 트레이스 실행 결과.
     *
     * 트레이스는 판정과 독립이다. 이벤트가 하나도 없어도 실패로 만들지 않고 그 사실만
     * 진단으로 남긴다. 계측을 강제하면 사용자는 계측을 지운다 (§7.3, §12.2).
     */
    private fun traceResult(
        request: ExecutionRequest,
        results: List<TestCaseResult>,
        events: List<TraceEvent>,
    ): ExecutionResult {
        val truncated = events.size >= TraceCapture.EVENT_BUDGET
        return ExecutionResult(
            executionId = request.executionId,
            submissionId = request.submissionId,
            attempt = request.attempt,
            fencingToken = request.fencingToken,
            problemVersionId = request.problemVersionId,
            terminalVerdict = null,
            compileLog = null,
            cases = results,
            resultDigest = digestOf(results),
            mode = ExecutionMode.TRACE,
            trace = TraceCapture(
                caseId = results.firstOrNull()?.let { it.groupId + "/" + it.caseId } ?: "",
                events = events,
                truncated = truncated,
                diagnostics = when {
                    events.isEmpty() -> "계측 호출이 없어 트레이스가 비어 있다"
                    truncated -> "이벤트 예산 " + TraceCapture.EVENT_BUDGET + "개를 넘겨 이후를 잘랐다"
                    else -> null
                },
            ),
        )
    }

    /** 같은 실행 결과가 두 번 도착했는지 판단하는 근거 (§4.3 결과 중복). */
    private fun digestOf(results: List<TestCaseResult>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        results.forEach { digest.update("${it.groupId}/${it.caseId}=${it.verdict}".toByteArray()) }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun terminal(
        request: ExecutionRequest,
        verdict: Verdict,
        compileLog: String?,
        reason: String?,
    ) = ExecutionResult(
        executionId = request.executionId,
        submissionId = request.submissionId,
        attempt = request.attempt,
        fencingToken = request.fencingToken,
        problemVersionId = request.problemVersionId,
        terminalVerdict = verdict,
        compileLog = compileLog ?: reason,
        cases = emptyList(),
        resultDigest = "$verdict:${request.executionId}",
        mode = request.mode,
    )

    private companion object {
        /**
         * 실행마다 결과가 달라지지 않게 하는 환경 (§12.1 재현성).
         *
         * 파이썬 해시 시드를 고정하지 않으면 dict/set 순회 순서가 실행마다 달라져 같은
         * 코드가 다른 판정을 받을 수 있다.
         */
        val DETERMINISM_ENV = mapOf(
            "PYTHONHASHSEED" to "0",
            "PYTHONDONTWRITEBYTECODE" to "1",
            "TZ" to "UTC",
        )

        /** 언어별 메모리 초과 예외. 하네스가 잡아 ERROR 로 보고한 것을 가려낸다. */
        val MEMORY_EXCEPTIONS = setOf("java.lang.OutOfMemoryError", "builtins.MemoryError")
    }
}

/** 기대 출력의 타입. 시그니처 반환 타입과 같다. */
private fun TestCase.expectedType() =
    if (expected is List<*>) ValueType.INT_ARRAY else ValueType.INT
