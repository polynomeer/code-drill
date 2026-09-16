package dev.codedrill.judge.runner.content

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.runner.execution.ExecutionEngine
import dev.codedrill.judge.runner.execution.KotlinCompilerArchive
import dev.codedrill.judge.runner.execution.RuntimeClasspath
import dev.codedrill.judge.runner.execution.project.KotlinProjectAdapter
import dev.codedrill.judge.runner.execution.project.PythonProjectAdapter
import dev.codedrill.judge.runner.execution.project.ProjectEngine
import dev.codedrill.judge.runner.execution.sandbox.ProcessSandbox
import dev.codedrill.platform.storage.DirectoryBlobStore
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeText
import kotlin.system.exitProcess

/**
 * 콘텐츠 검증 CLI (기술 설계서 §6.3, §15.1 파이프라인).
 *
 * 문제를 만든 사람이 손으로 돌리고, CI 가 같은 명령을 돌린다. 보고서는 파일로 남겨
 * 공개 요청에 첨부한다 — 제어 영역은 보고서 digest 가 맞는 버전만 공개한다 (§3.2).
 *
 * ```
 * ./gradlew :judge:runner-agent:validateContent
 * ./gradlew :judge:runner-agent:validateContent --args='content/problems content/reports two-sum,move-zeros'
 * ```
 *
 * 셋째 인자는 **저작 중에만** 쓴다. 전체는 문제당 10~30초라, 새 문제 하나를 고치며 매번 전체를
 * 돌리면 손이 멈춘다. 고른 문제의 보고서만 다시 쓰고 나머지는 그대로 둔다 — CI 와 공개 앞에는
 * 언제나 전체다.
 *
 * 프로젝트형 문제(`content/projects`, 첫 인자의 형제 디렉터리)도 같은 명령이 검증하고 같은
 * 디렉터리에 보고서를 남긴다. 공개 흐름은 보고서만 보므로 둘을 가르지 않는다.
 */
object ValidateContent {

    @JvmStatic
    fun main(args: Array<String>) {
        val contentRoot = Path.of(args.getOrElse(0) { "content/problems" })
        val reportRoot = Path.of(args.getOrElse(1) { "content/reports" })

        val validator = ContentValidator(
            engine = ExecutionEngine(
                // 컴파일마다 JVM 이 뜬다 (§5.5). 아카이브가 있으면 절반쯤 빠르다.
                adapters = mapOf(Language.KOTLIN to KotlinCompilerArchive.warmedAdapter(reportRoot.resolve(".cds"))),
                sandboxes = { ProcessSandbox() },
            ),
            contentRoot = contentRoot,
        )

        val json = ObjectMapper().registerKotlinModule()
            .enable(SerializationFeature.INDENT_OUTPUT)

        val only = args.getOrNull(2)?.split(',')?.map(String::trim)?.filter(String::isNotEmpty).orEmpty()

        // 프로젝트형 (11단계). 스토어는 검증 동안만 사는 디렉터리다 — 판정기가 스토어를 거치는
        // 길 그대로 검증해야 같은 코드가 같은 판정을 받는다.
        reportRoot.createDirectories()
        val store = DirectoryBlobStore(reportRoot.resolve(".store").also { it.createDirectories() })
        val projects = ProjectValidator(
            engine = ProjectEngine(
                adapters = mapOf(
                    Language.PYTHON to PythonProjectAdapter(),
                    Language.KOTLIN to KotlinProjectAdapter(RuntimeClasspath.all, RuntimeClasspath.kotlinCompiler),
                ),
                sandboxes = { ProcessSandbox() },
                store = store,
            ),
            store = store,
            projectsRoot = contentRoot.resolveSibling("projects"),
        )

        // 지난 실행이 남긴 보고서를 지우고 시작한다. 문제의 version 을 올리면 옛 버전의
        // 보고서가 그대로 남아, 시딩이 **이미 대체된 버전을 다시 공개**한다. 보고서는
        // 검증의 산출물이지 쌓아 두는 기록이 아니다.
        if (only.isEmpty()) reportRoot.listDirectoryEntries("*.json").forEach { it.deleteExisting() }

        val projectIds = projects.ids()
        val reports = if (only.isEmpty()) {
            validator.validateAll() + projectIds.map(projects::validate)
        } else {
            only.map { id -> if (id in projectIds) projects.validate(id) else validator.validate(id) }
        }

        for (report in reports) {
            val marker = if (report.passed) "PASS" else "FAIL"
            println("$marker  ${report.problemVersionId}  (report ${report.reportDigest.take(12)})")
            report.checks.forEach { check ->
                println("        ${if (check.passed) "·" else "✗"} ${check.stage}: ${check.detail}")
            }
            report.mutations.forEach { mutation ->
                val killed = if (mutation.killed) "잡힘 (${mutation.killedBy.joinToString()})" else "살아남음"
                println("        · mutant ${mutation.name} [${mutation.kind}]: $killed")
            }

            reportRoot.resolve("${report.problemVersionId.replace('@', '-')}.json")
                .writeText(json.writeValueAsString(report))
        }

        val failed = reports.count { !it.passed }
        println()
        println("${reports.size - failed}/${reports.size} 통과")
        if (failed > 0) exitProcess(1)
    }
}
