package dev.codedrill.judge.runner.content

import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.runner.execution.RuntimeClasspath
import dev.codedrill.judge.runner.execution.project.JavaProjectAdapter
import dev.codedrill.judge.runner.execution.project.KotlinProjectAdapter
import dev.codedrill.judge.runner.execution.project.ProjectEngine
import dev.codedrill.judge.runner.execution.project.PythonProjectAdapter
import dev.codedrill.judge.runner.execution.sandbox.ProcessSandbox
import dev.codedrill.platform.storage.DirectoryBlobStore
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 저장소의 모든 프로젝트형 문제가 공개 가능한 상태인지 (feature-roadmap 11단계, §6.3).
 *
 * [ContentValidationTest] 의 짝이다. 다른 점은 **전체를 돈다**는 것 — 프로젝트형은 스위트
 * 한 번이 100ms 라 오답까지 다 돌려도 문제당 1초다. 빠른 검사와 전체 검사를 가를 이유가
 * 없고, 그래서 여기서 걸리면 곧 공개할 수 없는 문제다.
 */
class ProjectValidationTest {

    private val store = DirectoryBlobStore(createTempDirectory("project-validation"))
    private val validator = ProjectValidator(
        engine = ProjectEngine(
            adapters = mapOf(
                Language.PYTHON to PythonProjectAdapter(),
                Language.KOTLIN to KotlinProjectAdapter(RuntimeClasspath.all, RuntimeClasspath.kotlinCompiler),
                Language.JAVA to JavaProjectAdapter(),
            ),
            sandboxes = { ProcessSandbox() },
            store = store,
        ),
        store = store,
        projectsRoot = Path.of("../../content/projects"),
    )

    private val reports by lazy { validator.ids().map(validator::validate) }

    @Test
    fun `모든 프로젝트형 문제가 검증을 통과한다`() {
        assertTrue(reports.isNotEmpty(), "검사할 프로젝트를 찾지 못했다")
        val failures = reports.filterNot { it.passed }.map { report ->
            report.problemVersionId + ": " + report.checks.filterNot { it.passed }.joinToString { "${it.stage} — ${it.detail}" }
        }
        assertTrue(failures.isEmpty(), "공개할 수 없는 프로젝트:\n" + failures.joinToString("\n"))
    }

    @Test
    fun `모든 프로젝트의 오답이 잡히고 가드가 손댄 오답을 잡는다`() {
        for (report in reports) {
            assertTrue(report.mutations.size >= 4, "${report.problemVersionId}: 오답이 ${report.mutations.size}개뿐이다")
            assertTrue(report.mutations.all { it.killed }, "${report.problemVersionId}: 살아남은 오답 " + report.mutations.filterNot { it.killed }.map { it.name })
            val tamper = report.mutations.single { it.name.startsWith("tamper") }
            assertEquals(listOf("tamper-guard"), tamper.killedBy, report.problemVersionId)
        }
    }

    @Test
    fun `보고서 digest 는 같은 패키지에서 같은 값이다`() {
        val id = validator.ids().first()
        assertEquals(validator.validate(id).reportDigest, validator.validate(id).reportDigest)
    }
}
