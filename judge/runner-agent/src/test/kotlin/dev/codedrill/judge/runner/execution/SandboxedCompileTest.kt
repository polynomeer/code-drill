package dev.codedrill.judge.runner.execution

import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.judge.runner.execution.adapter.JavaAdapter
import dev.codedrill.judge.runner.execution.adapter.KotlinAdapter
import dev.codedrill.judge.runner.execution.adapter.PythonAdapter
import dev.codedrill.judge.runner.execution.sandbox.ExecOutcome
import dev.codedrill.judge.runner.execution.sandbox.Sandbox
import dev.codedrill.judge.runner.execution.sandbox.SandboxRun
import dev.codedrill.judge.runner.execution.sandbox.SandboxSpec
import dev.codedrill.platform.problempackage.ProblemPackage
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 컴파일은 샌드박스가 돌린다 (§5.5). 어댑터가 컴파일러를 Runner 프로세스 안에서 부르는
 * 길로 되돌아가면 여기서 잡힌다 — 그 길은 격리 회귀 스위트가 초록인 채로 열릴 수 있다.
 * 컨테이너 안에서 컴파일이 실제로 되는지는 SandboxRegressionTest 가 본다.
 */
class SandboxedCompileTest {

    private val pkg: ProblemPackage = ProblemPackageLoader(Path.of("../../content/problems")).load("two-sum")

    /** exec 을 기록하고 컴파일 실패를 돌려주는 샌드박스. 실행 단계까지 가면 안 된다. */
    private class Recording(private val output: (SandboxSpec) -> String) : Sandbox {
        val specs = mutableListOf<SandboxSpec>()
        override fun available() = true
        override fun startupGraceMillis() = 0L
        override fun run(
            spec: SandboxSpec,
            caseIds: List<String>,
            onEvent: (String, String) -> Unit,
            shouldContinue: (String, CaseOutcome) -> Boolean,
        ): SandboxRun = error("컴파일이 실패했으면 실행하지 않는다")

        override fun exec(spec: SandboxSpec): ExecOutcome {
            specs += spec
            return ExecOutcome(exitCode = 1, output = output(spec))
        }
    }

    private val adapters = listOf(KotlinAdapter(), JavaAdapter(), PythonAdapter()).associateBy { it.language }

    @Test
    fun `세 언어 모두 컴파일을 샌드박스의 exec 으로 보낸다`() {
        for (language in adapters.keys) {
            val sandbox = Recording { "boom" }
            val result = ExecutionEngine(adapters, { sandbox }).execute(request(language))

            assertEquals(Verdict.COMPILE_ERROR, result.terminalVerdict, "$language")
            assertEquals("boom", result.compileLog?.trim(), "$language")
            val spec = assertNotNull(sandbox.specs.singleOrNull(), "$language: 컴파일은 exec 한 번이다")

            // 산출물 디렉터리 하나만 쓸 수 있고, 그것은 작업 디렉터리 아래다.
            val out = spec.writablePaths.single()
            assertEquals("out", out.fileName.toString(), "$language")
            assertTrue(out.startsWith(spec.workDir), "$language: $out 가 ${spec.workDir} 아래가 아니다")

            // 명령은 이미지 안의 실행 파일을 이름으로 부른다. 호스트 절대 경로의 바이너리는
            // 컨테이너에 없다.
            assertFalse(spec.command.first().startsWith("/"), "$language: ${spec.command.first()}")
            assertTrue(spec.memoryMb > 0 && spec.perCaseTimeoutMillis > 0, "$language: 컴파일러에도 한도가 있다")
        }
    }

    @Test
    fun `Kotlin 은 컴파일러 jar 를 읽기 전용으로 들여보낸다`() {
        val sandbox = Recording { "" }
        ExecutionEngine(adapters, { sandbox }).execute(request(Language.KOTLIN))

        val spec = sandbox.specs.single()
        assertTrue(
            spec.readOnlyPaths.any { it.fileName.toString().startsWith("kotlin-compiler-embeddable") },
            "컴파일러 jar 가 없다: ${spec.readOnlyPaths}",
        )
        assertTrue(spec.command.contains("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"))
    }

    @Test
    fun `컴파일 로그에서 서버 경로를 지운다`() {
        val sandbox = Recording { spec ->
            val src = spec.workDir.resolve("src")
            "$src/Solution.kt:1:53: error: unresolved reference 'x'.\n$src/Solution.kt:2:1: error: y"
        }
        val result = ExecutionEngine(adapters, { sandbox }).execute(request(Language.KOTLIN))

        assertEquals(
            "Solution.kt:1:53: error: unresolved reference 'x'.\nSolution.kt:2:1: error: y",
            result.compileLog,
        )
    }

    @Test
    fun `끝나지 않는 컴파일은 사용자 코드 오류다`() {
        val sandbox = object : Sandbox {
            override fun available() = true
            override fun startupGraceMillis() = 0L
            override fun run(spec: SandboxSpec, caseIds: List<String>, onEvent: (String, String) -> Unit, shouldContinue: (String, CaseOutcome) -> Boolean) =
                error("실행하지 않는다")
            override fun exec(spec: SandboxSpec) = ExecOutcome(exitCode = null, output = "")
        }
        val result = ExecutionEngine(adapters, { sandbox }).execute(request(Language.JAVA))

        assertEquals(Verdict.COMPILE_ERROR, result.terminalVerdict)
        assertTrue(result.compileLog.orEmpty().contains("끝나지 않았다"), "${result.compileLog}")
    }

    private fun request(language: Language) = ExecutionRequest(
        executionId = "exec-compile",
        submissionId = "sub-compile",
        attempt = 1,
        fencingToken = FencingToken(1),
        correlationId = "corr-compile",
        problemVersionId = pkg.problemVersionId,
        packageDigest = pkg.packageDigest,
        language = language,
        source = "whatever",
        signature = pkg.manifest.signature,
        limits = pkg.manifest.limits,
        groups = pkg.groups.map { RequestedGroup(it.policy, it.cases) },
        mode = ExecutionMode.JUDGE,
    )
}
