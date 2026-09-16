package dev.codedrill.judge.orchestrator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.codedrill.judge.orchestrator.bundle.BundlePublisher
import dev.codedrill.judge.orchestrator.lease.MemoryLeaseRegistry
import dev.codedrill.judge.protocol.JudgeOrigin
import dev.codedrill.judge.protocol.JudgeProgressed
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.ProjectCompleted
import dev.codedrill.judge.protocol.ProjectQueued
import dev.codedrill.judge.protocol.ProjectRequest
import dev.codedrill.judge.protocol.ProjectResult
import dev.codedrill.judge.protocol.ProjectTestOutcome
import dev.codedrill.judge.protocol.SubmissionQueued
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.judge.protocol.WorkspaceRef
import dev.codedrill.judge.protocol.Workspaces
import dev.codedrill.platform.problempackage.ProjectPackageLoader
import dev.codedrill.platform.storage.DirectoryBlobStore
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 두 번째 판정기의 조정 규칙 (feature-roadmap 11단계): 숨은 것은 수로만 나가고, 임대는 두
 * 종류의 원 요청을 품는다.
 */
class ProjectCoordinatorTest {

    private val packages = ProjectPackageLoader(Path.of("../../content/projects"))
    private val pkg = packages.load("inventory-ledger")
    private val store = DirectoryBlobStore(createTempDirectory("suites"))
    private val registry = MemoryLeaseRegistry()
    private val gateway = RecordingGateway()
    private val coordinator = ProjectCoordinator(packages, registry, gateway, BundlePublisher(store))

    private val queued = ProjectQueued(
        submissionId = "p-1",
        correlationId = "corr",
        projectId = pkg.manifest.id,
        projectVersion = pkg.manifest.version,
        language = Language.PYTHON,
        workspace = WorkspaceRef(Workspaces.workspaceKey("p-1"), "0".repeat(64)),
    )

    @Test
    fun `요청에는 스위트 참조가 실리고 스토어에 스위트가 올라간다`() {
        coordinator.onProjectQueued(queued)

        val request = gateway.requests.single()
        assertEquals(pkg.projectVersionId, request.projectVersionId)
        assertEquals(Workspaces.suiteKey(pkg.packageDigest), request.suite.key)
        assertEquals(request.suite.digest, store.digestOf(request.suite.key))
        assertEquals(pkg.hidden, Workspaces.decode(store.get(request.suite.key)!!))
        assertEquals(1, gateway.progress.size)
    }

    @Test
    fun `숨은 테스트는 이름도 사유도 없이 수로만 나간다`() {
        coordinator.onProjectQueued(queued)
        val request = gateway.requests.single()
        val hiddenModule = pkg.hiddenModules.first()

        coordinator.onProjectResult(
            result(
                request,
                Verdict.WRONG_ANSWER,
                listOf(
                    ProjectTestOutcome("tests.test_public", "PublicTests.test_a", true),
                    ProjectTestOutcome("tests.test_public", "PublicTests.test_b", false, "AssertionError: 1 != 2"),
                    ProjectTestOutcome(hiddenModule, "Secret.test_x", true),
                    ProjectTestOutcome(hiddenModule, "Secret.test_y", false, "AssertionError: 42 != 41"),
                ),
            ),
            "corr",
        )

        val completed = gateway.completed.single()
        assertEquals(Verdict.WRONG_ANSWER, completed.verdict)
        assertEquals(50, completed.score, "넷 중 둘 통과")
        assertEquals(listOf("PublicTests.test_a", "PublicTests.test_b"), completed.tests.map { it.name })
        assertEquals("AssertionError: 1 != 2", completed.tests[1].message, "공개 테스트의 사유는 남는다")
        assertEquals(1, completed.hiddenPassed)
        assertEquals(2, completed.hiddenTotal)
        assertTrue(completed.tests.none { it.module == hiddenModule }, "숨은 모듈은 목록에 없다")
    }

    @Test
    fun `낮은 토큰의 결과는 폐기된다`() {
        coordinator.onProjectQueued(queued)
        val first = gateway.requests.single()
        // 다시 임대되면 토큰이 오른다. 첫 실행의 결과는 스테일이다.
        coordinator.onProjectQueued(queued)

        coordinator.onProjectResult(result(first, Verdict.ACCEPTED, emptyList()), "corr")

        assertTrue(gateway.completed.isEmpty())
    }

    @Test
    fun `임대는 두 종류의 원 요청을 JSON 으로 오간다`() {
        val mapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())

        val project: JudgeOrigin = mapper.readValue(mapper.writeValueAsString(queued))
        assertIs<ProjectQueued>(project)
        assertEquals(queued, project)

        val submission: JudgeOrigin = mapper.readValue(
            mapper.writeValueAsString(SubmissionQueued(submissionId = "s", correlationId = "c", problemId = "two-sum", problemVersion = 1, language = Language.KOTLIN)),
        )
        assertIs<SubmissionQueued>(submission)

        // kind 가 없는 옛 JSON 은 알고리즘 제출이다 (N/N-1).
        val legacy: JudgeOrigin = mapper.readValue("""{"submissionId":"s","correlationId":"c","problemId":"two-sum","problemVersion":1,"language":"KOTLIN"}""")
        assertIs<SubmissionQueued>(legacy)
        assertNull((legacy as SubmissionQueued).sourceRef)
    }

    private fun result(request: ProjectRequest, verdict: Verdict, tests: List<ProjectTestOutcome>) = ProjectResult(
        executionId = request.executionId,
        submissionId = request.submissionId,
        attempt = request.attempt,
        fencingToken = request.fencingToken,
        projectVersionId = request.projectVersionId,
        verdict = verdict,
        log = null,
        tests = tests,
        buildMillis = 1,
        testMillis = 1,
        resultDigest = "d-${request.executionId}",
    )

    private class RecordingGateway : ProjectGateway {
        val requests = mutableListOf<ProjectRequest>()
        val progress = mutableListOf<JudgeProgressed>()
        val completed = mutableListOf<ProjectCompleted>()
        override fun requestProject(request: ProjectRequest) { requests += request }
        override fun publishProjectProgress(progress: JudgeProgressed) { this.progress += progress }
        override fun publishProjectCompleted(completed: ProjectCompleted) { this.completed += completed }
    }
}
