package dev.codedrill.judge.runner.execution

import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.judge.protocol.Measurements
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.TestCaseResult
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.problempackage.StopPolicy
import dev.codedrill.platform.problempackage.TestCase
import dev.codedrill.platform.problempackage.ValueType
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

/**
 * 런타임 어댑터 계약(기술 설계서 §5.3)을 Kotlin 런타임에 대해 구현한다.
 *
 * `prepare → compile → execute → check → cleanup` 순서로 진행하며, 각 단계의 실패를
 * 서로 다른 판정으로 갈라 낸다. 컴파일 실패는 테스트를 한 건도 돌리지 않고 종료하고,
 * 플랫폼 장애는 사용자 실패로 덮지 않는다 (§4.4).
 */
class ExecutionEngine(
    private val compiler: KotlinSourceCompiler = KotlinSourceCompiler(RuntimeClasspath.all),
    private val workRoot: Path? = null,
) {

    fun execute(request: ExecutionRequest): ExecutionResult {
        val sandboxDir = workRoot?.resolve(request.executionId)?.also { it.createDirectories() }
            ?: createTempDirectory("codedrill-exec-")

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
        // prepare: 사용자 소스와 생성한 하네스를 같은 소스 트리에 놓는다.
        val sourceDir = sandboxDir.resolve("src").also { it.createDirectories() }
        val classesDir = sandboxDir.resolve("classes").also { it.createDirectories() }
        sourceDir.resolve("Solution.kt").writeText(request.source)
        sourceDir.resolve("Main.kt").writeText(
            HarnessGenerator.generate(request.signature, request.groups),
        )

        // compile
        when (val outcome = compiler.compile(sourceDir, classesDir)) {
            is KotlinSourceCompiler.CompileOutcome.Failure ->
                return terminal(request, Verdict.COMPILE_ERROR, compileLog = outcome.log, reason = null)
            KotlinSourceCompiler.CompileOutcome.Success -> Unit
        }

        // execute: 그룹마다 별도 샌드박스를 띄운다.
        //
        // stop_policy 는 그룹 단위 정책이다 (§6.2). 한 프로세스에서 전부 돌리면 앞 그룹의
        // FAIL_FAST 나 TIME_LIMIT 이 뒤 그룹까지 통째로 날려버려, 채점하지 못한 그룹과
        // 정책대로 멈춘 그룹을 구분할 수 없게 된다. 그룹별 격리는 한 그룹의 OOM 이 다른
        // 그룹의 측정치를 오염시키지 않는 효과도 있다.
        val classpath = RuntimeClasspath.all.plusElement(classesDir)
        val results = request.groups.flatMap { group ->
            runGroup(request, group, classpath, sandboxDir)
        }

        return ExecutionResult(
            executionId = request.executionId,
            submissionId = request.submissionId,
            attempt = request.attempt,
            fencingToken = request.fencingToken,
            terminalVerdict = null,
            compileLog = null,
            cases = results,
            resultDigest = digestOf(results),
        )
    }

    /** 기대 출력과 실제 출력을 맞춘다 (§5.3 check). 기본 checker 는 정확 일치다. */
    private fun verdictOf(outcome: CaseOutcome, case: TestCase, outputLimit: Long): Verdict = when (outcome) {
        is CaseOutcome.Completed -> when {
            // 출력 한도 초과는 정답 여부보다 먼저 판정한다. 한도를 넘긴 실행은 결과를
            // 신뢰할 수 없다.
            outcome.userOutputBytes > outputLimit -> Verdict.OUTPUT_LIMIT
            outcome.output == encodeExpected(case.expected) -> Verdict.ACCEPTED
            else -> Verdict.WRONG_ANSWER
        }
        // 하네스가 Throwable 을 잡으므로 OOM 도 여기로 온다. 자원 초과를 사용자 코드
        // 버그(RUNTIME_ERROR)로 뭉뚱그리면 사용자가 잘못된 곳을 고치게 된다.
        is CaseOutcome.Threw ->
            if (outcome.exceptionClass == OUT_OF_MEMORY) Verdict.MEMORY_LIMIT else Verdict.RUNTIME_ERROR
        CaseOutcome.TimedOut -> Verdict.TIME_LIMIT
        CaseOutcome.MemoryExceeded -> Verdict.MEMORY_LIMIT
        CaseOutcome.OutputExceeded -> Verdict.OUTPUT_LIMIT
        is CaseOutcome.Broken -> Verdict.SYSTEM_ERROR
        CaseOutcome.NotRun -> Verdict.SYSTEM_ERROR
    }

    /** 그룹 하나를 자기 프로세스에서 실행하고 케이스 결과로 바꾼다. */
    private fun runGroup(
        request: ExecutionRequest,
        group: RequestedGroup,
        classpath: List<Path>,
        sandboxDir: Path,
    ): List<TestCaseResult> {
        val byId = group.cases.associateBy { it.qualifiedId() }
        val outputLimit = request.limits.outputBytes

        val outcomes = SandboxProcess(classpath, sandboxDir).run(
            groupId = group.policy.id,
            caseIds = group.cases.map { it.qualifiedId() },
            perCaseTimeoutMillis = (request.limits.timeMillis * group.policy.limitMultiplier.time).toLong(),
            memoryMb = (request.limits.memoryMb * group.policy.limitMultiplier.memory).toInt(),
            outputByteLimit = outputLimit,
        ) { caseId, outcome ->
            // 기대 출력은 자식에게 넘기지 않으므로 정답 비교는 여기서만 일어난다.
            val passed = verdictOf(outcome, byId.getValue(caseId), outputLimit) == Verdict.ACCEPTED
            passed || group.policy.stopPolicy != StopPolicy.FAIL_FAST
        }

        return outcomes.mapNotNull { (caseId, outcome) ->
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

    private companion object {
        const val OUT_OF_MEMORY = "java.lang.OutOfMemoryError"
    }

    /** 하네스의 인코딩과 같은 규칙으로 기대값을 문자열로 만든다. */
    private fun encodeExpected(expected: Any): String = when (expected) {
        is List<*> -> expected.joinToString(",") { (it as Number).toInt().toString() }
        is Number -> expected.toInt().toString()
        else -> error("지원하지 않는 기대 출력 형식: ${expected::class}")
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

    /**
     * 사용자에게 보여줄 사유. 숨은 그룹에서는 케이스 내용을 유추할 단서를 담지 않는다 (§8.3).
     */
    private fun messageOf(outcome: CaseOutcome, exposesInput: Boolean): String? = when (outcome) {
        is CaseOutcome.Threw -> outcome.exceptionClass
        is CaseOutcome.Broken -> if (exposesInput) outcome.reason else "실행을 완료하지 못했다"
        else -> null
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
        terminalVerdict = verdict,
        compileLog = compileLog ?: reason,
        cases = emptyList(),
        resultDigest = "$verdict:${request.executionId}",
    )
}
