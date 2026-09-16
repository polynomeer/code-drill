package dev.codedrill.judge.runner.execution.project

import dev.codedrill.judge.protocol.BundleRef
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.ProjectRequest
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.judge.protocol.WorkspaceRef
import dev.codedrill.judge.protocol.Workspaces
import dev.codedrill.judge.runner.execution.sandbox.ProcessSandbox
import dev.codedrill.platform.problempackage.ProjectPackageLoader
import dev.codedrill.platform.storage.DirectoryBlobStore
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 두 번째 판정기의 규칙 (feature-roadmap 11단계). 저장소의 프로젝트 하나로 확인한다.
 */
class ProjectEngineTest {

    private val loader = ProjectPackageLoader(Path.of("../../content/projects"))
    private val pkg = loader.load("inventory-ledger")
    private val store = DirectoryBlobStore(createTempDirectory("projects"))
    private val engine = ProjectEngine(
        adapters = mapOf(Language.PYTHON to PythonProjectAdapter()),
        sandboxes = { ProcessSandbox() },
        store = store,
    )

    private val suiteBytes = Workspaces.encode(pkg.hidden)
    private val suite = BundleRef(Workspaces.suiteKey(pkg.packageDigest), Workspaces.digest(suiteBytes))
        .also { store.put(it.key, suiteBytes, Workspaces.CONTENT_TYPE, it.digest) }

    @Test
    fun `참조 구현은 공개·숨은 테스트를 전부 통과한다`() {
        val result = engine.execute(request("ref", pkg.starter + loader.reference("inventory-ledger")!!))

        assertEquals(Verdict.ACCEPTED, result.verdict, result.log)
        assertTrue(result.tests.all { it.passed })
        assertTrue(result.tests.any { it.module in pkg.hiddenModules }, "숨은 모듈의 테스트가 돌았다")
        assertTrue(result.tests.any { it.module !in pkg.hiddenModules }, "공개 테스트도 돌았다")
    }

    @Test
    fun `시작 저장소 그대로는 오답이다`() {
        val result = engine.execute(request("starter", pkg.starter))

        assertEquals(Verdict.WRONG_ANSWER, result.verdict)
        assertTrue(result.tests.none { it.passed })
        // 사유는 Runner 가 다 채운다. 숨은 것을 지우는 것은 오케스트레이터다.
        assertTrue(result.tests.all { it.message != null })
    }

    @Test
    fun `대표 오답은 하나씩 잡힌다`() {
        for (mutant in loader.mutants("inventory-ledger")) {
            val result = engine.execute(request("m-${mutant.name}", pkg.starter + mutant.overlay))
            assertEquals(Verdict.WRONG_ANSWER, result.verdict, "${mutant.name}: ${result.log}")
            assertTrue(result.tests.any { !it.passed }, mutant.name)
        }
    }

    @Test
    fun `테스트 기반을 손대면 전부 실패로 판정한다`() {
        val tamper = loader.mutants("inventory-ledger").single { it.name.startsWith("tamper") }
        val result = engine.execute(request("tamper", pkg.starter + tamper.overlay))

        assertEquals(Verdict.WRONG_ANSWER, result.verdict)
        assertTrue(result.tests.isNotEmpty() && result.tests.none { it.passed })
        assertTrue(result.log.orEmpty().contains("테스트 기반이 바뀌었다"), result.log)
    }

    @Test
    fun `사용자가 숨은 테스트 파일을 갈아 끼울 수 없다`() {
        val hijacked = pkg.starter + loader.reference("inventory-ledger")!! +
            pkg.hidden.keys.associateWith { "import unittest\nclass Nothing(unittest.TestCase):\n    def test_pass(self):\n        pass\n" }
        // 참조 구현이 아니라 시작 골격에 얹는다 — 숨은 테스트가 정말 돌면 떨어져야 한다.
        val result = engine.execute(request("hijack", pkg.starter + hijacked.filterKeys { it in pkg.hidden.keys }))

        assertEquals(Verdict.WRONG_ANSWER, result.verdict)
        assertTrue(result.tests.any { it.module in pkg.hiddenModules && !it.passed }, "숨은 테스트가 덮어써지지 않고 돌았다")
    }

    @Test
    fun `문법이 깨진 파일은 컴파일 오류다`() {
        val broken = pkg.starter + ("ledger/inventory.py" to "def broken(:\n")
        val result = engine.execute(request("syntax", broken))

        assertEquals(Verdict.COMPILE_ERROR, result.verdict)
        assertTrue(result.log.orEmpty().contains("inventory.py"), result.log)
    }

    @Test
    fun `워크스페이스의 digest 가 다르면 채점하지 않는다`() {
        val files = pkg.starter
        val bytes = Workspaces.encode(files)
        store.put(Workspaces.workspaceKey("tampered"), bytes + "x".toByteArray(), Workspaces.CONTENT_TYPE, null)
        val request = request("tampered", files, upload = false)

        val result = engine.execute(request)

        assertEquals(Verdict.SYSTEM_ERROR, result.verdict)
        assertTrue(result.log.orEmpty().contains("digest"), result.log)
    }

    @Test
    fun `밖을 가리키는 경로는 받지 않는다`() {
        assertTrue(runCatching { Workspaces.validate(mapOf("../escape.py" to "")) }.isFailure)
        assertTrue(runCatching { Workspaces.validate(mapOf("/abs.py" to "")) }.isFailure)
        assertTrue(runCatching { Workspaces.validate(mapOf(".hidden" to "")) }.isFailure)
        assertEquals(mapOf("a/b.py" to "x"), Workspaces.validate(mapOf(" a/b.py " to "x")))
    }

    private fun request(id: String, files: Map<String, String>, upload: Boolean = true): ProjectRequest {
        val bytes = Workspaces.encode(files)
        val ref = WorkspaceRef(Workspaces.workspaceKey(id), Workspaces.digest(bytes))
        if (upload) store.put(ref.key, bytes, Workspaces.CONTENT_TYPE, ref.digest)
        return ProjectRequest(
            executionId = "exec-$id",
            submissionId = id,
            attempt = 1,
            fencingToken = FencingToken(1),
            correlationId = "corr-$id",
            projectVersionId = pkg.projectVersionId,
            packageDigest = pkg.packageDigest,
            language = Language.PYTHON,
            workspace = ref,
            suite = suite,
            limits = pkg.manifest.limits,
        )
    }

}
