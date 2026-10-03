package dev.codedrill.judge.runner.execution

import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.judge.runner.execution.adapter.KotlinAdapter
import dev.codedrill.judge.runner.execution.sandbox.ExecOutcome
import dev.codedrill.judge.runner.execution.sandbox.Sandbox
import dev.codedrill.judge.runner.execution.sandbox.SandboxRun
import dev.codedrill.judge.runner.execution.sandbox.SandboxSpec
import dev.codedrill.platform.problempackage.Aggregation
import dev.codedrill.platform.problempackage.GroupPolicy
import dev.codedrill.platform.problempackage.ProblemPackage
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import dev.codedrill.platform.problempackage.StopPolicy
import dev.codedrill.platform.problempackage.TestCase
import dev.codedrill.platform.problempackage.Visibility
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 같은 소스는 한 번만 컴파일한다 — 아레나·축소가 입력만 바꿔 같은 소스를 수십 번 돌린다.
 * 이유와 경계는 [CompileCache].
 */
class CompileCacheTest {

    private val pkg: ProblemPackage = ProblemPackageLoader(Path.of("../../content/problems")).load("two-sum")

    /** 컴파일하면 산출물을 하나 쓰고, 실행할 때 그 산출물이 있었는지를 적는 샌드박스. */
    private class Fake(private val failing: Set<String> = emptySet()) : Sandbox {
        var compiles = 0
        val sawArtifact = mutableListOf<String?>()

        override fun available() = true
        override fun startupGraceMillis() = 0L

        override fun exec(spec: SandboxSpec): ExecOutcome {
            compiles += 1
            val source = spec.workDir.resolve("src/Solution.kt").readText()
            if (source in failing) return ExecOutcome(exitCode = 1, output = "error")
            spec.writablePaths.single().resolve("artifact").writeText(source)
            return ExecOutcome(exitCode = 0, output = "")
        }

        override fun run(
            spec: SandboxSpec,
            caseIds: List<String>,
            onEvent: (String, String) -> Unit,
            shouldContinue: (String, CaseOutcome) -> Boolean,
        ): SandboxRun {
            val artifact = spec.workDir.resolve("out/artifact")
            sawArtifact += if (artifact.exists()) artifact.readText() else null
            return SandboxRun(caseIds.associateWith { CaseOutcome.NotRun })
        }
    }

    private val adapters = listOf(KotlinAdapter()).associateBy { it.language }

    @Test
    fun `입력만 다른 실행은 컴파일을 다시 하지 않는다`() {
        val sandbox = Fake()
        val engine = ExecutionEngine(adapters, { sandbox }, compileCache = CompileCache())

        engine.execute(request("fun a() = 1", args = listOf(listOf(2, 7, 11, 15), 9)))
        engine.execute(request("fun a() = 1", args = listOf(listOf(3, 3), 6)))

        assertEquals(1, sandbox.compiles, "케이스 파일은 키가 아니다")
        assertEquals(listOf<String?>("fun a() = 1", "fun a() = 1"), sandbox.sawArtifact, "두 번째 실행도 산출물을 받는다")
    }

    @Test
    fun `소스가 다르면 따로 컴파일한다`() {
        val sandbox = Fake()
        val engine = ExecutionEngine(adapters, { sandbox }, compileCache = CompileCache())

        engine.execute(request("fun a() = 1"))
        engine.execute(request("fun a() = 2"))
        engine.execute(request("fun a() = 1"))

        assertEquals(2, sandbox.compiles)
        assertEquals(listOf<String?>("fun a() = 1", "fun a() = 2", "fun a() = 1"), sandbox.sawArtifact)
    }

    @Test
    fun `실패한 컴파일은 담지 않는다`() {
        val sandbox = Fake(failing = setOf("fun broken("))
        val engine = ExecutionEngine(adapters, { sandbox }, compileCache = CompileCache())

        assertEquals(Verdict.COMPILE_ERROR, engine.execute(request("fun broken(")).terminalVerdict)
        assertEquals(Verdict.COMPILE_ERROR, engine.execute(request("fun broken(")).terminalVerdict)

        assertEquals(2, sandbox.compiles)
    }

    @Test
    fun `모드가 다르면 하네스가 달라 따로 컴파일한다`() {
        val sandbox = Fake()
        val engine = ExecutionEngine(adapters, { sandbox }, compileCache = CompileCache())

        engine.execute(request("fun a() = 1", mode = ExecutionMode.TRIAL))
        engine.execute(request("fun a() = 1", mode = ExecutionMode.TRACE))

        assertEquals(2, sandbox.compiles, "계측 SDK 가 모드마다 다르다")
    }

    private fun request(
        source: String,
        args: List<Any> = listOf(listOf(2, 7, 11, 15), 9),
        mode: ExecutionMode = ExecutionMode.TRIAL,
    ) = ExecutionRequest(
        executionId = "exec-cache",
        submissionId = "sub-cache",
        attempt = 1,
        fencingToken = FencingToken(1),
        correlationId = "corr-cache",
        problemVersionId = pkg.problemVersionId,
        packageDigest = pkg.packageDigest,
        language = Language.KOTLIN,
        source = source,
        signature = pkg.manifest.signature,
        limits = pkg.manifest.limits,
        groups = listOf(RequestedGroup(GROUP, listOf(TestCase("c", GROUP.id, args, expected = null)))),
        mode = mode,
    )

    private companion object {
        val GROUP = GroupPolicy("sample", 100, Visibility.PUBLIC, Aggregation.SUM, StopPolicy.CONTINUE)
    }
}
