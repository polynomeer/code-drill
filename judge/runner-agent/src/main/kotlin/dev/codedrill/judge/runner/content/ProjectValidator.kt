package dev.codedrill.judge.runner.content

import dev.codedrill.judge.protocol.BundleRef
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.ProjectRequest
import dev.codedrill.judge.protocol.ProjectResult
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.judge.protocol.WorkspaceRef
import dev.codedrill.judge.protocol.Workspaces
import dev.codedrill.judge.runner.execution.project.ProjectEngine
import dev.codedrill.platform.problempackage.ProjectMutant
import dev.codedrill.platform.problempackage.ProjectPackage
import dev.codedrill.platform.problempackage.ProjectPackageLoader
import dev.codedrill.platform.storage.BlobStore
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * 프로젝트형 문제의 저작 검증 (feature-roadmap 11단계, §6.3 의 규칙 그대로).
 *
 * 산출물의 모양은 알고리즘 문제와 다르지만 규칙은 같다 — **참조는 통과하고 오답은
 * 떨어진다.** 여기에 프로젝트형만의 것이 둘 붙는다: 시작 저장소 그대로는 정답이 아니어야
 * 하고(아니면 문제가 없다), 테스트 기반을 손대는 오답이 잡혀야 한다(두 번째 판정기의 가드).
 *
 * 보고서는 [ContentValidator] 의 것과 같은 모양이다. 공개 흐름(§3.2)은 보고서만 보고,
 * 그 보고서가 어느 판정기에서 나왔는지는 묻지 않는다.
 */
class ProjectValidator(
    private val engine: ProjectEngine,
    private val store: BlobStore,
    private val projectsRoot: Path,
) {

    private val loader = ProjectPackageLoader(projectsRoot)

    fun ids(): List<String> = loader.ids()

    fun validate(projectId: String): ValidationReport {
        val pkg = loader.load(projectId)
        val checks = mutableListOf<Check>()

        checks += structure(pkg)
        checks += catalog(pkg)
        checks += editorial(projectId)

        val reference = loader.reference(projectId)
        if (reference == null) {
            checks += Check.fail("reference", "reference/ 가 없다")
            return report(pkg, checks, emptyList())
        }

        val suite = publishSuite(pkg)
        val judged = judge(pkg, suite, "reference", pkg.starter + reference)
        checks += referencePasses(judged)
        checks += hiddenModulesObserved(pkg, judged)
        checks += timeHeadroom(pkg, judged)

        val starter = judge(pkg, suite, "starter", pkg.starter)
        checks += Check(
            "starter-fails", starter.verdict != Verdict.ACCEPTED,
            if (starter.verdict == Verdict.ACCEPTED) "시작 저장소 그대로 정답이다. 풀 것이 없다" else "시작 저장소는 ${starter.verdict}",
        )

        val mutants = loader.mutants(projectId).map { mutant -> evaluate(pkg, suite, mutant) }
        checks += mutationKillRate(mutants)
        checks += tamperGuard(mutants)

        return report(pkg, checks, mutants)
    }

    private fun structure(pkg: ProjectPackage): List<Check> {
        val problems = mutableListOf<String>()
        if (!projectsRoot.resolve(pkg.manifest.id).resolve(pkg.manifest.statement).exists()) {
            problems += "본문 파일이 없다: ${pkg.manifest.statement}"
        }
        if (pkg.publicModules.isEmpty()) problems += "starter/tests/ 에 공개 테스트가 없다"
        if (pkg.hiddenModules.isEmpty()) problems += "hidden/ 에 테스트 모듈이 없다"
        runCatching { Language.valueOf(pkg.manifest.language) }.onFailure { problems += "모르는 언어: ${pkg.manifest.language}" }
        runCatching { Workspaces.validate(pkg.starter) }.onFailure { problems += "starter: ${it.message}" }
        runCatching { Workspaces.validate(pkg.hidden) }.onFailure { problems += "hidden: ${it.message}" }
        return listOf(
            if (problems.isEmpty()) {
                Check.pass("structure", "starter ${pkg.starter.size}파일 · hidden 모듈 ${pkg.hiddenModules.size} · ${pkg.manifest.language}")
            } else {
                Check.fail("structure", problems.joinToString("; "))
            },
        )
    }

    private fun catalog(pkg: ProjectPackage): List<Check> {
        val unknown = pkg.catalog.tags.filterNot { it in vocabulary }
        // 프로젝트형은 실무군만 단다. 알고리즘 칸에 달리면 프로젝트 증거가 거기 섞인다 (11단계).
        val foreign = pkg.catalog.competencies.filterNot { it.engineering }
        return listOf(
            when {
                unknown.isNotEmpty() -> Check.fail("catalog", "어휘에 없는 태그: ${unknown.joinToString()} (content/tags.yaml)")
                pkg.catalog.summary.isBlank() -> Check.fail("catalog", "summary 가 비어 있다")
                pkg.catalog.competencies.isEmpty() -> Check.fail("catalog", "역량이 없다. 이 문제를 풀어도 숙련도가 움직이지 않는다")
                foreign.isNotEmpty() -> Check.fail("catalog", "프로젝트형은 실무군 역량만 달 수 있다: ${foreign.joinToString()}")
                else -> Check.pass("catalog", "${pkg.catalog.difficulty} · 태그 ${pkg.catalog.tags.size} · 역량 ${pkg.catalog.competencies.size}")
            },
        )
    }

    private fun editorial(projectId: String): List<Check> {
        val text = loader.editorial(projectId)
        return listOf(
            when {
                text == null -> Check.fail("editorial", "editorial.md 가 없다")
                text.trim().length < MIN_EDITORIAL -> Check.fail("editorial", "editorial.md 가 너무 짧다 (${text.trim().length}자)")
                else -> Check.pass("editorial", "${text.trim().length}자")
            },
        )
    }

    private fun referencePasses(judged: ProjectResult): List<Check> {
        val failed = judged.tests.filterNot { it.passed }
        return listOf(
            when {
                judged.verdict == Verdict.ACCEPTED -> Check.pass("reference", "테스트 ${judged.tests.size}개 전부 통과")
                failed.isNotEmpty() -> Check.fail("reference", "참조가 떨어진 테스트: ${failed.joinToString { "${it.module}.${it.name}" }}")
                else -> Check.fail("reference", "참조가 ${judged.verdict}: ${judged.log?.take(300)}")
            },
        )
    }

    /**
     * 숨은 모듈 하나하나가 리포트에 나타나야 한다.
     *
     * 오케스트레이터는 파일 경로에서 만든 모듈 이름으로 숨은 것을 자른다. Kotlin 은 파일 이름과
     * 클래스 이름이 다를 수 있고, 그러면 그 테스트는 "공개"로 새어 나간다 — 이름도 사유도 함께.
     */
    private fun hiddenModulesObserved(pkg: ProjectPackage, judged: ProjectResult): List<Check> {
        val observed = judged.tests.map { it.module }.toSet()
        val missing = pkg.hiddenModules.filterNot { it in observed }
        val stray = observed.filter { it.startsWith("tests.") && it !in pkg.hiddenModules && it !in pkg.publicModules }
        return listOf(
            when {
                missing.isNotEmpty() -> Check.fail("hidden-modules", "리포트에 없는 숨은 모듈: ${missing.joinToString()} — 파일 이름과 클래스 이름이 같아야 한다")
                stray.isNotEmpty() -> Check.fail("hidden-modules", "어느 파일의 것인지 모르는 테스트 모듈: ${stray.joinToString()}")
                else -> Check.pass("hidden-modules", "숨은 모듈 ${pkg.hiddenModules.size}개가 전부 리포트에 있다")
            },
        )
    }

    /** 참조가 스위트 한도의 절반을 넘게 쓰면 다른 머신에서 시간 초과다 — 알고리즘 문제와 같은 기준이다. */
    private fun timeHeadroom(pkg: ProjectPackage, judged: ProjectResult): List<Check> {
        val limit = pkg.manifest.limits.testSeconds * 1000L
        val ratio = judged.testMillis.toDouble() / limit
        return listOf(
            if (ratio <= MAX_REFERENCE_TIME_RATIO) {
                Check.pass("limits-headroom", "참조 스위트 ${judged.testMillis}ms / ${limit}ms, 빌드 ${judged.buildMillis}ms")
            } else {
                Check.fail("limits-headroom", "참조 스위트가 한도의 ${"%.0f".format(ratio * 100)}% 를 쓴다")
            },
        )
    }

    private fun evaluate(pkg: ProjectPackage, suite: BundleRef, mutant: ProjectMutant): MutationResult {
        val judged = judge(pkg, suite, "mutant-${mutant.name}", pkg.starter + mutant.overlay)
        val killedBy = judged.tests.filterNot { it.passed }.map { "${it.module}.${it.name}" }
        val killed = judged.verdict != Verdict.ACCEPTED && judged.verdict.userFailure
        return MutationResult(
            name = mutant.name,
            kind = mutant.kind,
            killed = killed,
            killedBy = if (judged.log?.startsWith("테스트 기반이 바뀌었다") == true) listOf("tamper-guard") else killedBy,
            verdicts = listOf(judged.verdict.name),
        )
    }

    private fun mutationKillRate(mutants: List<MutationResult>): List<Check> {
        if (mutants.isEmpty()) return listOf(Check.fail("mutation-kill-rate", "mutants/ 가 비어 있다. 대표 오답 없이는 스위트가 무엇을 잡는지 모른다"))
        val alive = mutants.filterNot { it.killed }
        return listOf(
            if (alive.isEmpty()) {
                Check.pass("mutation-kill-rate", "${mutants.size}/${mutants.size} 잡힘")
            } else {
                Check.fail("mutation-kill-rate", "살아남은 오답: ${alive.joinToString { it.name }}")
            },
        )
    }

    /**
     * 테스트 기반을 손대는 오답이 하나는 있어야 하고, 그것은 **가드가** 잡아야 한다.
     *
     * 이 검사가 없으면 가드가 조용히 무너져도 아무 보고서가 달라지지 않는다 — 손대는 오답이
     * 우연히 다른 테스트에 떨어져도 "잡힘"이기 때문이다.
     */
    private fun tamperGuard(mutants: List<MutationResult>): List<Check> {
        val tamper = mutants.filter { it.name.startsWith("tamper") }
        return listOf(
            when {
                tamper.isEmpty() -> Check.fail("tamper-guard", "테스트 기반을 손대는 오답(mutants/tamper--*)이 없다")
                tamper.any { "tamper-guard" !in it.killedBy } -> Check.fail("tamper-guard", "가드가 잡지 못했다: ${tamper.filter { "tamper-guard" !in it.killedBy }.joinToString { it.name }}")
                else -> Check.pass("tamper-guard", "${tamper.size}개를 가드가 잡았다")
            },
        )
    }

    private fun publishSuite(pkg: ProjectPackage): BundleRef {
        val bytes = Workspaces.encode(pkg.hidden)
        val ref = BundleRef(Workspaces.suiteKey(pkg.packageDigest), Workspaces.digest(bytes))
        store.put(ref.key, bytes, Workspaces.CONTENT_TYPE, ref.digest)
        return ref
    }

    private fun judge(pkg: ProjectPackage, suite: BundleRef, label: String, files: Map<String, String>): ProjectResult {
        val bytes = Workspaces.encode(files)
        val id = "validate-${pkg.manifest.id}-$label"
        val ref = WorkspaceRef(Workspaces.workspaceKey(id), Workspaces.digest(bytes))
        store.put(ref.key, bytes, Workspaces.CONTENT_TYPE, ref.digest)
        return engine.execute(
            ProjectRequest(
                executionId = "exec-$id",
                submissionId = id,
                attempt = 1,
                fencingToken = FencingToken(1),
                correlationId = id,
                projectVersionId = pkg.projectVersionId,
                packageDigest = pkg.packageDigest,
                language = Language.valueOf(pkg.manifest.language),
                workspace = ref,
                suite = suite,
                limits = pkg.manifest.limits,
            ),
        )
    }

    private fun report(pkg: ProjectPackage, checks: List<Check>, mutations: List<MutationResult>): ValidationReport {
        val digest = MessageDigest.getInstance("SHA-256").apply {
            update(pkg.packageDigest.toByteArray())
            update(ContentValidator.Scope.FULL.name.toByteArray())
            update(ContentValidator.VALIDATOR_VERSION.toByteArray())
            checks.sortedBy { it.stage }.forEach { update("${it.stage}=${it.passed}".toByteArray()) }
        }.digest().joinToString("") { "%02x".format(it) }
        return ValidationReport(
            problemVersionId = pkg.projectVersionId,
            packageDigest = pkg.packageDigest,
            passed = checks.all { it.passed },
            checks = checks,
            mutations = mutations,
            reportDigest = digest,
        )
    }

    private val vocabulary: Set<String> by lazy {
        val file = projectsRoot.parent?.resolve("tags.yaml")
        if (file == null || !file.exists()) emptySet() else TAG_LINE.findAll(file.readText()).map { it.groupValues[1] }.toSet()
    }

    private companion object {
        // 기준은 알고리즘 문제와 같다. 두 판정기가 해설 길이나 시간 여유를 다르게 재면 안 된다.
        val TAG_LINE = ContentValidator.TAG_LINE
        const val MIN_EDITORIAL = ContentValidator.MIN_EDITORIAL
        const val MAX_REFERENCE_TIME_RATIO = ContentValidator.MAX_REFERENCE_TIME_RATIO
    }
}
