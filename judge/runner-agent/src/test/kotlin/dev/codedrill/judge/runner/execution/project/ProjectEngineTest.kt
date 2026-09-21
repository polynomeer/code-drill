package dev.codedrill.judge.runner.execution.project

import dev.codedrill.judge.protocol.BundleRef
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.ProbeBundle
import dev.codedrill.judge.protocol.Probes
import dev.codedrill.judge.protocol.ProjectRequest
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.judge.protocol.WorkspaceRef
import dev.codedrill.judge.protocol.Workspaces
import dev.codedrill.judge.runner.execution.sandbox.ProcessSandbox
import dev.codedrill.platform.problempackage.ProjectPackage
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

    // --- 사용자의 테스트를 시험한다 (실무군 셋째 역량) ---

    private val probeBytes = Probes.encode(
        ProbeBundle(
            starterTests = pkg.starter.filterKeys(ProjectPackage::isTestModule),
            variants = mapOf(Probes.REFERENCE to pkg.starter + loader.reference("inventory-ledger")!!) +
                loader.mutants("inventory-ledger").filterNot { it.name.startsWith("tamper") }.associate { it.name to pkg.starter + it.overlay },
        ),
    )
    private val probe = BundleRef(Probes.probeKey(pkg.packageDigest), Workspaces.digest(probeBytes))
        .also { store.put(it.key, probeBytes, Workspaces.CONTENT_TYPE, it.digest) }

    private val reference = pkg.starter + loader.reference("inventory-ledger")!!

    /**
     * 부분 로트의 나머지 원가를 묻는 테스트. partial-lot 과 lifo 를 떨어뜨리지만 그 둘은 시작 저장소의
     * 공개 테스트만으로도 떨어진다 — 사용자의 몫이 아니다. 되돌리기·이력 오답은 못 잡는다.
     */
    private val partialLotTest = """
        import unittest
        from ledger import Ledger

        class MineTests(unittest.TestCase):
            def test_remainder_keeps_its_cost(self):
                ledger = Ledger()
                ledger.receive("A", 4, 10)
                ledger.receive("A", 6, 20)
                ledger.ship("A", 7)
                self.assertEqual(3 * 20, ledger.ship("A", 3))
    """.trimIndent()

    @Test
    fun `더 쓴 테스트가 없으면 시험하지 않는다`() {
        val result = engine.execute(request("probe-none", reference, probe = probe))

        assertEquals(Verdict.ACCEPTED, result.verdict)
        assertEquals(null, result.probe)
    }

    @Test
    fun `더 쓴 테스트를 참조와 오답 위에서 돌려 잡은 것을 센다`() {
        val result = engine.execute(request("probe-mine", reference + ("tests/test_mine.py" to partialLotTest), probe = probe))

        assertEquals(Verdict.ACCEPTED, result.verdict)
        val outcome = result.probe!!
        assertTrue(outcome.referencePassed)
        assertEquals(emptyList(), outcome.killed, "공개 테스트가 이미 잡는 오답은 내 몫이 아니다")
        assertEquals(listOf("lifo--consumes-newest-lot-first", "partial-lot--drops-remainder"), outcome.alreadyCaught)
        assertEquals(listOf("history--returns-internal-list", "no-rollback--ships-partial-on-shortage"), outcome.survived)
    }

    @Test
    fun `숨은 테스트를 그대로 더 쓰면 오답 전부를 잡는다`() {
        val hidden = pkg.hidden.entries.first { it.key.endsWith(".py") && it.key.contains("test_") }
        val result = engine.execute(request("probe-hidden", reference + ("tests/test_mine.py" to hidden.value), probe = probe))

        assertTrue(result.probe!!.referencePassed)
        assertEquals(emptyList(), result.probe!!.survived)
        // 공개 테스트가 못 잡던 둘을 잡았다. 나머지 둘은 공개 테스트의 몫이다.
        assertEquals(listOf("history--returns-internal-list", "no-rollback--ships-partial-on-shortage"), result.probe!!.killed)
        assertEquals(2, result.probe!!.alreadyCaught.size)
    }

    @Test
    fun `참조에서 떨어지는 테스트는 아무것도 잡지 못한다`() {
        val wrong = partialLotTest.replace("3 * 20", "3 * 10")
        val result = engine.execute(request("probe-wrong", reference + ("tests/test_mine.py" to wrong), probe = probe))

        // 틀린 테스트는 자기 제출도 떨어뜨린다 — 판정은 그대로 오답이고, 시험은 그 위에 따로 붙는다.
        assertEquals(Verdict.WRONG_ANSWER, result.verdict)
        val outcome = result.probe!!
        assertEquals(false, outcome.referencePassed)
        assertTrue(outcome.killed.isEmpty() && outcome.survived.isEmpty())
        assertTrue(outcome.log!!.contains("test_remainder_keeps_its_cost"), outcome.log)
    }

    @Test
    fun `시험은 사용자의 테스트만 들이고 구현은 들이지 않는다`() {
        // 사용자의 구현이 오답이어도, 그 테스트는 참조·오답 위에서 돈다.
        val mutant = loader.mutants("inventory-ledger").first { it.name.startsWith("partial-lot") }
        val result = engine.execute(request("probe-impl", pkg.starter + mutant.overlay + ("tests/test_mine.py" to partialLotTest), probe = probe))

        assertEquals(Verdict.WRONG_ANSWER, result.verdict)
        assertTrue(result.probe!!.referencePassed)
        assertTrue("partial-lot--drops-remainder" in result.probe!!.alreadyCaught)
    }

    private fun request(id: String, files: Map<String, String>, upload: Boolean = true, probe: BundleRef? = null): ProjectRequest {
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
            probe = probe,
        )
    }

}
