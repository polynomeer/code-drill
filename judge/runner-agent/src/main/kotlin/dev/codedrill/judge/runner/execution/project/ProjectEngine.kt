package dev.codedrill.judge.runner.execution.project

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.ProbeBundle
import dev.codedrill.judge.protocol.Probes
import dev.codedrill.judge.protocol.ProjectProbeOutcome
import dev.codedrill.judge.protocol.ProjectRequest
import dev.codedrill.judge.protocol.ProjectResult
import dev.codedrill.judge.protocol.ProjectTestOutcome
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.judge.protocol.Workspaces
import dev.codedrill.judge.runner.execution.ExecutionEngine
import dev.codedrill.judge.runner.execution.sandbox.ExecOutcome
import dev.codedrill.judge.runner.execution.sandbox.Sandbox
import dev.codedrill.judge.runner.execution.sandbox.SandboxSpec
import dev.codedrill.platform.problempackage.ProjectPackage
import dev.codedrill.platform.storage.BlobStore
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * 두 번째 판정기 (feature-roadmap 11단계).
 *
 * ```
 * resolve(워크스페이스·스위트) → materialize → build → test → report → cleanup
 * ```
 *
 * [ExecutionEngine] 과 **격리만 같이 쓴다.** 같은 [Sandbox] 로 빌드와 스위트를 돌리되,
 * 요청도 채점 규칙도 다르다 — 케이스별 출력 비교가 아니라 테스트 리포트가 판정이고,
 * 한도는 초가 아니라 분이다.
 *
 * 판정 규칙:
 * - 워크스페이스나 스위트를 못 받거나 digest 가 다르면 [Verdict.SYSTEM_ERROR]. 사용자 탓이 아니다.
 * - 빌드가 실패하거나 한도 안에 끝나지 않으면 [Verdict.COMPILE_ERROR].
 * - 스위트가 한도 안에 끝나지 않으면 [Verdict.TIME_LIMIT], 메모리로 죽으면 [Verdict.MEMORY_LIMIT],
 *   리포트 없이 끝나면 [Verdict.RUNTIME_ERROR].
 * - 테스트 기반이 손댄 것으로 판정되면 **전부 실패**로 [Verdict.WRONG_ANSWER].
 * - 나머지는 전부 통과면 [Verdict.ACCEPTED], 아니면 [Verdict.WRONG_ANSWER].
 *
 * 판정 뒤에 **사용자의 테스트를 시험한다** ([Probes], 실무군 셋째 역량). 요청에 시험판이 있고
 * 사용자가 시작 저장소보다 테스트를 더 썼으면, 그 테스트를 참조 구현과 대표 오답 위에서 각각
 * 돌린다 — 참조 위에서 통과해야 하고, 오답 위에서 떨어지면 잡은 것이다. 판정과 무관하다:
 * 시험이 어떻게 끝나든 판정은 위 규칙 그대로이고, 시험이 아예 못 돌면 결과에 없을 뿐이다.
 */
class ProjectEngine(
    private val adapters: Map<Language, ProjectAdapter>,
    private val sandboxes: (Language) -> Sandbox,
    private val store: BlobStore,
    private val workRoot: Path? = null,
    private val onPhase: (phase: String, language: Language, outcome: String, nanos: Long) -> Unit = { _, _, _, _ -> },
) {

    private val json = ObjectMapper().registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    fun execute(request: ProjectRequest): ProjectResult {
        val files = try {
            Files(
                workspace = fetch(request.workspace.key, request.workspace.digest, "워크스페이스"),
                suite = fetch(request.suite.key, request.suite.digest, "스위트"),
            )
        } catch (e: Exception) {
            return terminal(request, Verdict.SYSTEM_ERROR, e.message)
        }
        // 시험판은 판정의 전제가 아니다. 못 받으면 시험만 빠진다.
        val probe = request.probe?.let { ref ->
            runCatching { Probes.decode(fetchBytes(ref.key, ref.digest, "시험판")) }.getOrNull()
        }

        val sandboxDir = (
            workRoot?.resolve(request.executionId)?.also { it.createDirectories() }
                ?: createTempDirectory("codedrill-project-")
            ).toRealPath()

        return try {
            val result = run(request, files, sandboxDir)
            if (probe == null || result.verdict !in PROBEABLE) result else result.copy(probe = probe(request, files.workspace, probe, sandboxDir))
        } catch (e: Exception) {
            terminal(request, Verdict.SYSTEM_ERROR, e.message)
        } finally {
            sandboxDir.toFile().deleteRecursively()
        }
    }

    private fun fetch(key: String, digest: String, what: String): Map<String, String> =
        Workspaces.decode(fetchBytes(key, digest, what))

    private fun fetchBytes(key: String, digest: String, what: String): ByteArray {
        val bytes = store.get(key) ?: error("$what 를 스토어에서 찾지 못했다: $key")
        val actual = Workspaces.digest(bytes)
        check(actual == digest) { "$what 의 digest 가 요청과 다르다: $key (저장 ${actual.take(12)}, 요청 ${digest.take(12)})" }
        return bytes
    }

    /**
     * 사용자의 테스트를 참조 구현과 오답 위에서 돌린다. 더 쓴 테스트가 없으면 null — 안 쓴 것은
     * 못 쓰는 것의 증거가 아니다.
     *
     * 판마다 새 워크스페이스다: 판의 파일 위에 사용자의 **테스트 파일만** 덮는다. 사용자의 구현은
     * 들이지 않는다 — 시험하는 것은 테스트이지 구현이 아니다. 참조 위에서 하나라도 떨어지면 그
     * 테스트는 틀린 것을 기대하는 것이라 오답 판은 돌리지 않는다.
     *
     * 오답을 "잡았다"는 것은 그 판에서 리포트가 전부 통과가 아니라는 것이다 — 단언 실패든, 예외든,
     * 빌드 실패든. 참조에서 되던 테스트가 오답에서 어떤 식으로든 깨졌으면 잡은 것이다.
     */
    private fun probe(request: ProjectRequest, workspace: Map<String, String>, bundle: ProbeBundle, sandboxDir: Path): ProjectProbeOutcome? {
        val userTests = workspace.filterKeys(ProjectPackage::isTestModule)
        val written = userTests.entries.sumOf { ProjectPackage.testMethods(it.key, it.value) }
        val given = bundle.starterTests.entries.sumOf { ProjectPackage.testMethods(it.key, it.value) }
        if (written <= given) return null
        val reference = bundle.variants[Probes.REFERENCE] ?: return null

        val onReference = suite(request, reference + userTests, sandboxDir.resolve("probe-reference"))
        if (!onReference.allPassed) {
            return ProjectProbeOutcome(referencePassed = false, killed = emptyList(), survived = emptyList(), log = onReference.explain())
        }
        val killed = ArrayList<String>()
        val survived = ArrayList<String>()
        for ((name, files) in bundle.variants) {
            if (name == Probes.REFERENCE) continue
            val outcome = suite(request, files + userTests, sandboxDir.resolve("probe-" + name.replace(Regex("[^A-Za-z0-9_-]"), "_")))
            if (outcome.allPassed) survived += name else killed += name
        }
        return ProjectProbeOutcome(referencePassed = true, killed = killed.sorted(), survived = survived.sorted())
    }

    /** 판 하나를 빌드하고 돌린 결과. [allPassed] 만이 시험의 답이고 나머지는 사유다. */
    private class SuiteRun(val verdict: Verdict, val report: ProjectReport?, val log: String?) {
        val allPassed: Boolean get() = verdict == Verdict.ACCEPTED

        fun explain(): String? = when {
            log != null -> log
            report == null -> null
            else -> report.tests.filterNot { it.passed }.joinToString("\n") { "${it.module}.${it.name}: ${it.message ?: "실패"}" }.take(MAX_LOG_CHARS)
        }
    }

    /** 워크스페이스 하나를 통째로 빌드하고 스위트를 돌린다. 판정 본체와 시험판이 같은 길을 간다. */
    private fun suite(request: ProjectRequest, files: Map<String, String>, dir: Path): SuiteRun {
        val adapter = adapters[request.language] ?: error("지원하지 않는 언어다: ${request.language}")
        val sandbox = sandboxes(request.language)
        dir.createDirectories()
        val workspace = dir.resolve("ws").also { it.createDirectories() }
        val harness = dir.resolve("harness").also { it.createDirectories() }
        val out = dir.resolve("out").also { it.createDirectories() }
        for ((path, content) in files) write(workspace, path, content)
        for ((name, content) in adapter.harnessFiles()) harness.resolve(name).also { it.parent.createDirectories() }.writeText(content)

        val build = adapter.buildCommand(workspace)?.let { command ->
            sandbox.exec(spec(command, dir, out, adapter, adapter.buildMemoryMb(request.limits.memoryMb), request.limits.buildSeconds * 1000L, ExecutionEngine.COMPILER_PIDS_LIMIT))
        }
        if (build != null && build.exitCode != 0) {
            return SuiteRun(Verdict.COMPILE_ERROR, null, build.output.replace(workspace.absolutePathString() + "/", "").trim().take(MAX_LOG_CHARS))
        }
        val reportFile = out.resolve("report.json")
        val nonceFile = out.resolve("nonce")
        val nonce = java.util.UUID.randomUUID().toString()
        nonceFile.writeText(nonce)
        val test = sandbox.exec(
            spec(adapter.testCommand(workspace, harness, out, reportFile, nonceFile, request.limits.memoryMb), dir, out, adapter, request.limits.memoryMb, request.limits.testSeconds * 1000L, TEST_PIDS_LIMIT),
        )
        if (test.exitCode == null) return SuiteRun(Verdict.TIME_LIMIT, null, "스위트가 ${request.limits.testSeconds}초 안에 끝나지 않았다")
        if (!reportFile.isRegularFile()) return SuiteRun(if (test.exitCode == OOM_KILLED) Verdict.MEMORY_LIMIT else Verdict.RUNTIME_ERROR, null, test.output.takeLast(MAX_LOG_CHARS).trim())
        val parsed = json.readValue<ProjectReport>(reportFile.readText())
        parsed.loadError?.let { return SuiteRun(Verdict.RUNTIME_ERROR, null, it.takeLast(MAX_LOG_CHARS)) }
        val report = if (parsed.nonce != nonce) parsed.copy(tampered = parsed.tampered ?: "리포트를 하네스가 쓰지 않았다") else parsed
        val verdict = when {
            report.tampered != null -> Verdict.WRONG_ANSWER
            report.tests.isEmpty() -> Verdict.RUNTIME_ERROR
            report.tests.all { it.passed } -> Verdict.ACCEPTED
            else -> Verdict.WRONG_ANSWER
        }
        return SuiteRun(verdict, report, if (report.tampered != null) "테스트 기반이 바뀌었다: ${report.tampered}" else null)
    }

    private fun run(request: ProjectRequest, files: Files, sandboxDir: Path): ProjectResult {
        val adapter = adapters[request.language] ?: error("지원하지 않는 언어다: ${request.language}")
        val sandbox = sandboxes(request.language)

        // materialize — 사용자 파일 위에 숨은 테스트를 덮는다. 같은 경로면 숨은 것이 이긴다.
        val workspace = sandboxDir.resolve("ws").also { it.createDirectories() }
        val harness = sandboxDir.resolve("harness").also { it.createDirectories() }
        val out = sandboxDir.resolve("out").also { it.createDirectories() }
        for ((path, content) in files.workspace) write(workspace, path, content)
        for ((path, content) in files.suite) write(workspace, path, content)
        for ((name, content) in adapter.harnessFiles()) harness.resolve(name).also { it.parent.createDirectories() }.writeText(content)

        // build
        val buildStart = System.nanoTime()
        val buildTimeout = request.limits.buildSeconds * 1000L
        val build = adapter.buildCommand(workspace)?.let { command ->
            sandbox.exec(spec(command, sandboxDir, out, adapter, adapter.buildMemoryMb(request.limits.memoryMb), buildTimeout, ExecutionEngine.COMPILER_PIDS_LIMIT))
        }
        val buildMillis = (System.nanoTime() - buildStart) / 1_000_000
        onPhase("build", request.language, if (build == null || build.exitCode == 0) "success" else "failure", System.nanoTime() - buildStart)
        if (build != null && build.exitCode != 0) {
            // 컴파일러가 찍는 경로는 샌드박스의 것이다. 사용자가 보는 것은 워크스페이스 상대 경로다.
            val output = build.output.replace(workspace.absolutePathString() + "/", "")
            val log = when (build.exitCode) {
                null -> "빌드가 ${request.limits.buildSeconds}초 안에 끝나지 않았다\n$output"
                else -> output.ifBlank { "빌드가 코드 ${build.exitCode} 로 끝났다" }
            }
            return terminal(request, Verdict.COMPILE_ERROR, log.trim().take(MAX_LOG_CHARS), buildMillis = buildMillis)
        }

        // test — 리포트의 진위는 nonce 로 본다. 하네스가 사용자 코드를 들이기 전에 읽고 지우는 값이다.
        val reportFile = out.resolve("report.json")
        val nonceFile = out.resolve("nonce")
        val nonce = java.util.UUID.randomUUID().toString()
        nonceFile.writeText(nonce)
        val testStart = System.nanoTime()
        val testTimeout = request.limits.testSeconds * 1000L
        val test = sandbox.exec(
            spec(adapter.testCommand(workspace, harness, out, reportFile, nonceFile, request.limits.memoryMb), sandboxDir, out, adapter, request.limits.memoryMb, testTimeout, TEST_PIDS_LIMIT),
        )
        val testMillis = (System.nanoTime() - testStart) / 1_000_000
        onPhase("test", request.language, if (test.exitCode == 0) "success" else "failure", System.nanoTime() - testStart)

        if (test.exitCode == null) {
            return terminal(request, Verdict.TIME_LIMIT, "스위트가 ${request.limits.testSeconds}초 안에 끝나지 않았다", buildMillis, testMillis)
        }
        if (!reportFile.isRegularFile()) {
            val verdict = if (test.exitCode == OOM_KILLED) Verdict.MEMORY_LIMIT else Verdict.RUNTIME_ERROR
            return terminal(request, verdict, test.output.takeLast(MAX_LOG_CHARS).trim().ifBlank { "스위트가 리포트 없이 코드 ${test.exitCode} 로 끝났다" }, buildMillis, testMillis)
        }

        val parsed = json.readValue<ProjectReport>(reportFile.readText())
        parsed.loadError?.let { return terminal(request, Verdict.RUNTIME_ERROR, it.takeLast(MAX_LOG_CHARS), buildMillis, testMillis) }
        // nonce 가 다르면 하네스가 쓴 리포트가 아니다 — 사용자 코드가 꾸며 쓴 것이다.
        val report = if (parsed.nonce != nonce) parsed.copy(tampered = parsed.tampered ?: "리포트를 하네스가 쓰지 않았다") else parsed

        // 테스트 기반이 손댄 것이면 리포트의 통과는 믿을 수 없다. 전부 실패다.
        val tests = report.tests.map { row ->
            if (report.tampered != null) row.copy(passed = false, message = null) else row
        }
        val verdict = when {
            report.tampered != null -> Verdict.WRONG_ANSWER
            tests.isEmpty() -> Verdict.RUNTIME_ERROR
            tests.all { it.passed } -> Verdict.ACCEPTED
            else -> Verdict.WRONG_ANSWER
        }
        return ProjectResult(
            executionId = request.executionId,
            submissionId = request.submissionId,
            attempt = request.attempt,
            fencingToken = request.fencingToken,
            projectVersionId = request.projectVersionId,
            verdict = verdict,
            log = when {
                report.tampered != null -> "테스트 기반이 바뀌었다: ${report.tampered}"
                tests.isEmpty() -> "테스트를 하나도 찾지 못했다"
                else -> null
            },
            tests = tests,
            buildMillis = buildMillis,
            testMillis = testMillis,
            resultDigest = digestOf(verdict, tests),
        )
    }

    private fun spec(
        command: List<String>,
        workDir: Path,
        out: Path,
        adapter: ProjectAdapter,
        memoryMb: Int,
        timeoutMillis: Long,
        pidsLimit: Int,
    ) = SandboxSpec(
        command = command,
        workDir = workDir,
        readOnlyPaths = adapter.readOnlyPaths(),
        env = adapter.env(),
        memoryMb = memoryMb,
        perCaseTimeoutMillis = timeoutMillis,
        outputByteLimit = MAX_LOG_CHARS.toLong(),
        writablePaths = listOf(out),
        pidsLimit = pidsLimit,
    )

    /**
     * 경로를 한 번 더 다듬어 쓴다. 제어 영역이 받을 때 거른 것이지만, 스토어를 손댄 사람이
     * `../` 로 샌드박스 밖에 파일을 쓰게 두면 안 된다 — Runner 는 자기 밖의 것을 믿지 않는다.
     */
    private fun write(root: Path, path: String, content: String) {
        val clean = Workspaces.normalize(path)
        val target = root.resolve(clean).normalize()
        check(target.startsWith(root)) { "경로가 워크스페이스 밖을 가리킨다: $path" }
        target.parent.createDirectories()
        target.writeText(content)
    }

    private fun terminal(
        request: ProjectRequest,
        verdict: Verdict,
        log: String?,
        buildMillis: Long = 0,
        testMillis: Long = 0,
    ) = ProjectResult(
        executionId = request.executionId,
        submissionId = request.submissionId,
        attempt = request.attempt,
        fencingToken = request.fencingToken,
        projectVersionId = request.projectVersionId,
        verdict = verdict,
        log = log,
        tests = emptyList(),
        buildMillis = buildMillis,
        testMillis = testMillis,
        resultDigest = "$verdict:${request.executionId}",
    )

    /** 같은 결과가 두 번 도착했는지 판단하는 근거 (§4.3). */
    private fun digestOf(verdict: Verdict, tests: List<ProjectTestOutcome>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(verdict.name.toByteArray())
        tests.forEach { digest.update("${it.module}/${it.name}=${it.passed}".toByteArray()) }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private data class Files(val workspace: Map<String, String>, val suite: Map<String, String>)

    /** 하네스가 적는 리포트의 모양. 언어마다 하네스는 다르지만 리포트는 하나다. */
    data class ProjectReport(
        val nonce: String? = null,
        val tests: List<ProjectTestOutcome> = emptyList(),
        val tampered: String? = null,
        val loadError: String? = null,
    )

    companion object {
        /** 스위트는 프로세스를 여럿 띄울 수 있다 (subprocess 를 쓰는 테스트). 컴파일러만큼은 아니다. */
        const val TEST_PIDS_LIMIT = 128

        /** 컨테이너가 메모리 초과로 죽였을 때의 종료 코드 (128 + SIGKILL). */
        const val OOM_KILLED = 137

        /** 이 판정 뒤에만 사용자의 테스트를 시험한다. 빌드가 안 되거나 판정기가 못 돈 제출의 테스트는 볼 것이 없다. */
        val PROBEABLE = setOf(Verdict.ACCEPTED, Verdict.WRONG_ANSWER)

        const val MAX_LOG_CHARS = 16 * 1024
    }
}
