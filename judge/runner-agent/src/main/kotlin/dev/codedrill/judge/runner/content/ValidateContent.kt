package dev.codedrill.judge.runner.content

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.runner.execution.ExecutionEngine
import dev.codedrill.judge.runner.execution.KotlinCompilerArchive
import dev.codedrill.judge.runner.execution.RuntimeClasspath
import dev.codedrill.judge.runner.execution.project.JavaProjectAdapter
import dev.codedrill.judge.runner.execution.project.KotlinProjectAdapter
import dev.codedrill.judge.runner.execution.project.PythonProjectAdapter
import dev.codedrill.judge.runner.execution.project.ProjectEngine
import dev.codedrill.judge.runner.execution.sandbox.ProcessSandbox
import dev.codedrill.platform.storage.DirectoryBlobStore
import java.nio.file.Path
import dev.codedrill.platform.problempackage.ProblemNumbers
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readText
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
 *
 * `CODEDRILL_VALIDATION_CACHE` 에 디렉터리를 주면 바뀌지 않은 패키지는 지난번 통과한 보고서를 쓴다
 * ([ValidationCache] — 무엇이 바뀌면 다시 재는지는 거기). CI 와 `up.py --seed` 가 쓴다.
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
                    Language.JAVA to JavaProjectAdapter(),
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
        val cache = System.getenv("CODEDRILL_VALIDATION_CACHE")?.takeIf { it.isNotBlank() && only.isEmpty() }
            ?.let { ValidationCache(Path.of(it), ValidationCache.validatorFingerprint()) }
        val reports = if (only.isEmpty()) {
            val problemIds = contentRoot.listDirectoryEntries()
                .filter { it.resolve("manifest.yaml").exists() }
                .map { it.name }
                .sorted()
            if (cache == null) {
                problemIds.map(validator::validate) + projectIds.map(projects::validate)
            } else {
                val shared = shared(contentRoot, problemIds)
                val tags = shared(contentRoot, emptyList())
                // 번호표는 문제를 더할 때마다 자란다 — 통째로 넣으면 커밋마다 전부 다시 잰다. 검증이 보는 것은
                // 이 문제의 번호가 있는가 하나다. 표가 깨졌으면 그 사실이 키에 들어가고 검증은 떨어진다.
                val numbers = runCatching { ProblemNumbers.load(contentRoot) }
                val found = problemIds.map { id ->
                    val number = numbers.map { it.of(id) }.getOrElse { "읽지 못함: ${it.message}" }
                    cached(cache, "problem", id, contentRoot.resolve(id), "$shared\nnumber=$number") { validator.validate(id) }
                } + projectIds.map { id ->
                    cached(cache, "project", id, contentRoot.resolveSibling("projects").resolve(id), tags) { projects.validate(id) }
                }
                println("캐시: ${found.count { it.second }}/${found.size} 재사용, 지난 항목 ${cache.prune()} 개 지움")
                found.map { it.first }
            }
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

    /** 패키지 밖에서 검증이 읽는 것 — 태그 어휘와 (문제라면) 선수 관계 그래프 */
    private fun shared(contentRoot: Path, problemIds: List<String>): String {
        val tags = contentRoot.parent?.resolve("tags.yaml")?.takeIf { it.exists() }?.readText().orEmpty()
        val loader = ProblemPackageLoader(contentRoot)
        // 읽지 못하는 카탈로그는 빈 것으로 둔다 — 그 문제는 검증에서 떨어지고, 실패는 캐시에 담기지 않는다
        val edges = problemIds.associateWith { id -> runCatching { loader.load(id).catalog.prerequisites }.getOrDefault(emptyList()) }
        return ValidationCache.sharedFingerprint(tags, edges) { contentRoot.resolve(it).resolve("manifest.yaml").exists() }
    }

    private fun cached(
        cache: ValidationCache,
        kind: String,
        id: String,
        dir: Path,
        shared: String,
        validate: () -> ValidationReport,
    ): Pair<ValidationReport, Boolean> {
        val key = cache.key(kind, id, ValidationCache.directoryFingerprint(dir), shared)
        cache.find(key)?.let { return it to true }
        return validate().also { cache.store(key, it) } to false
    }
}
