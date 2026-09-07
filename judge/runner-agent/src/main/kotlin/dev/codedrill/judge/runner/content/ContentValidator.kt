package dev.codedrill.judge.runner.content

import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.TraceManifest
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.judge.runner.execution.ExecutionEngine
import dev.codedrill.platform.problempackage.ProblemPackage
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * 콘텐츠 검증 파이프라인 (기술 설계서 §6.3).
 *
 * 문제 하나를 공개해도 되는지 판단한다. §6.3 의 단계를 그대로 따르고, 각 단계의 결과를
 * 하나의 보고서로 묶어 digest 를 남긴다. 제어 영역은 **이 digest 가 있는 버전만 공개**한다
 * (§3.2) — 검증하지 않은 문제가 공개되는 경로를 아예 만들지 않기 위해서다.
 *
 * 실행이 필요한 검증이므로 실행 영역에서 돈다. 제어 영역이 직접 사용자 코드를 돌리면
 * §2.3 의 신뢰 경계가 무너진다.
 */
class ContentValidator(
    private val engine: ExecutionEngine,
    private val contentRoot: Path,
    /**
     * 무엇까지 검사할지.
     *
     * 성능 그룹은 20만 원소짜리 입력을 정답과 오답 여럿에 대해 돌리므로, 34개 문제를
     * 전부 돌리면 몇십 분이 걸린다. 그 시간을 `./gradlew build` 마다 치르면 아무도
     * 빌드를 돌리지 않게 되고, 그러면 빠른 검사도 함께 사라진다.
     */
    private val scope: Scope = Scope.FULL,
) {

    private val loader = ProblemPackageLoader(contentRoot)

    /**
     * 검사 범위.
     *
     * [FAST] 로 나온 보고서는 **공개에 쓸 수 없다.** digest 에 범위가 섞이므로, 제어
     * 영역이 대조할 때 전체 검증으로 만든 digest 와 절대 같아지지 않는다 (§3.2).
     * 빠른 검사를 통과한 것을 공개 가능으로 오해하는 경로 자체를 없애는 것이 목적이다.
     */
    enum class Scope {
        /** 전부. 공개 전에 반드시 한 번 돌아야 한다 (§6.3). */
        FULL,

        /**
         * 구조와 공식 해답만. 성능 그룹·돌연변이·결정성 재실행·트레이스를 뺀다.
         *
         * 답하는 질문이 다르다. 전체 검증은 "이 문제를 공개해도 되는가"를 묻고, 빠른
         * 검사는 **"저장소의 문제들이 아직 앞뒤가 맞는가"**를 묻는다. 실제로 났던 사고는
         * 후자였다 — 테스트 데이터를 고치다 기대값이 틀어져 정답 풀이가 오답을 받았다.
         *
         * 돌연변이 분석을 여기서 빼도 잃는 것이 없다. 공개는 전체 검증 보고서의 digest
         * 대조를 통과해야만 되므로(§3.2), 오답을 잡지 못하는 문제가 공개될 길은 없다.
         */
        FAST,
    }

    fun validate(problemId: String): ValidationReport {
        val pkg = inScope(loader.load(problemId))
        val checks = mutableListOf<Check>()

        checks += structure(problemId, pkg)
        val reference = referenceSolution(problemId)

        if (reference == null) {
            checks += Check.fail("official-solution", "solutions/reference.kt 가 없다 (§6.1)")
            return report(pkg, checks, emptyList())
        }

        val judged = run(pkg, reference)
        checks += officialSolution(pkg, judged)
        checks += limitsHeadroom(pkg, judged)

        if (scope == Scope.FAST) return report(pkg, checks, emptyList())

        checks += determinism(pkg, reference, judged)

        val mutants = mutants(problemId).map { mutant -> evaluate(pkg, mutant) }
        checks += mutationKillRate(mutants)

        checks += traceBudget(pkg, reference)

        return report(pkg, checks, mutants)
    }

    /** 범위 밖의 그룹을 떼어 낸 패키지. 이후 단계는 이것만 본다. */
    private fun inScope(pkg: ProblemPackage): ProblemPackage = when (scope) {
        Scope.FULL -> pkg
        Scope.FAST -> pkg.copy(groups = pkg.groups.filterNot { it.policy.id == PERFORMANCE_GROUP })
    }

    fun validateAll(): List<ValidationReport> =
        contentRoot.listDirectoryEntries()
            .filter { it.resolve("manifest.yaml").exists() }
            .map { it.name }
            .sorted()
            .map(::validate)

    // --- §6.3 1. 스키마와 참조 무결성 ---

    private fun structure(problemId: String, pkg: ProblemPackage): List<Check> {
        val problems = mutableListOf<String>()
        val dir = contentRoot.resolve(problemId)

        if (!dir.resolve(pkg.manifest.statement).exists()) {
            problems += "본문 파일이 없다: ${pkg.manifest.statement}"
        }
        // 케이스 id 는 그룹 안에서만 유일하면 되지만, 그룹까지 붙인 전역 키는 유일해야
        // 결과를 케이스에 되짚을 수 있다.
        val ids = pkg.groups.flatMap { group -> group.cases.map { "${it.groupId}/${it.id}" } }
        if (ids.size != ids.distinct().size) problems += "케이스 키가 중복된다"

        val arity = pkg.manifest.signature.parameters.size
        pkg.groups.forEach { group ->
            group.cases.forEach { case ->
                if (case.args.size != arity) {
                    problems += "${case.groupId}/${case.id}: 인자 개수가 시그니처와 다르다"
                }
            }
        }

        return listOf(
            if (problems.isEmpty()) {
                Check.pass("structure", "스키마와 참조 무결성 통과")
            } else {
                Check.fail("structure", problems.joinToString("; "))
            },
        )
    }

    // --- §6.3 2. 모든 공식 해답을 전체 테스트에 실행 ---

    private fun officialSolution(pkg: ProblemPackage, result: ExecutionResult): List<Check> {
        val expected = pkg.groups.sumOf { it.cases.size }
        val failed = result.cases.filter { it.verdict != Verdict.ACCEPTED }

        return listOf(
            when {
                result.terminalVerdict != null ->
                    Check.fail("official-solution", "정답 풀이가 ${result.terminalVerdict} 다")

                result.cases.size != expected ->
                    Check.fail("official-solution", "케이스 ${result.cases.size}/$expected 만 실행됐다")

                failed.isNotEmpty() ->
                    Check.fail(
                        "official-solution",
                        "정답 풀이가 통과하지 못한 케이스: " +
                            failed.joinToString { "${it.groupId}/${it.caseId}=${it.verdict}" },
                    )

                else -> Check.pass("official-solution", "${expected}개 케이스 전부 통과")
            },
        )
    }

    // --- §6.3 5. checker 결정성 ---

    private fun determinism(
        pkg: ProblemPackage,
        reference: String,
        first: ExecutionResult,
    ): List<Check> {
        val second = run(pkg, reference)
        return listOf(
            if (first.resultDigest == second.resultDigest) {
                Check.pass("determinism", "같은 입력이 같은 판정을 낸다")
            } else {
                Check.fail("determinism", "같은 입력이 다른 판정을 냈다 (§12.1 재현성)")
            },
        )
    }

    // --- §6.3 4. 제약 경계와 한도 검증 ---

    /**
     * 정답 풀이가 제한 시간에 얼마나 여유를 두는지 본다.
     *
     * 여유가 없으면 채점 서버 부하나 런타임 이미지 교체만으로 정답이 시간 초과가 된다.
     * 문제를 만든 사람의 머신에서 아슬아슬하게 통과하는 문제는 공개하면 안 된다.
     */
    private fun limitsHeadroom(pkg: ProblemPackage, result: ExecutionResult): List<Check> {
        val slowest = result.cases.maxOfOrNull { it.measurements.wallTimeMillis } ?: 0
        val limit = pkg.manifest.limits.timeMillis
        val ratio = if (limit > 0) slowest.toDouble() / limit else 0.0

        return listOf(
            if (ratio <= MAX_REFERENCE_TIME_RATIO) {
                Check.pass("limits", "정답 풀이가 제한의 ${percent(ratio)} 를 쓴다")
            } else {
                Check.fail(
                    "limits",
                    "정답 풀이가 제한의 ${percent(ratio)} 를 쓴다. " +
                        "여유가 ${percent(1 - MAX_REFERENCE_TIME_RATIO)} 미만이면 환경이 조금만 " +
                        "느려져도 정답이 시간 초과가 된다",
                )
            },
        )
    }

    // --- §6.3 3. mutant kill rate ---

    private fun evaluate(pkg: ProblemPackage, mutant: Mutant): MutationResult {
        val result = run(pkg, mutant.source)
        val killers = result.cases.filter { it.verdict != Verdict.ACCEPTED }

        return MutationResult(
            name = mutant.name,
            kind = mutant.kind,
            killed = result.terminalVerdict != null || killers.isNotEmpty(),
            // 어느 그룹이 잡았는지. 특정 그룹만 일하고 있으면 테스트 설계가 치우친 것이다.
            killedBy = killers.map { it.groupId }.distinct().sorted(),
            verdicts = killers.map { it.verdict.name }.distinct().sorted(),
        )
    }

    private fun mutationKillRate(results: List<MutationResult>): List<Check> {
        if (results.isEmpty()) {
            return listOf(Check.fail("mutation", "mutants/ 가 비어 있다. 대표 오답이 있어야 한다 (§6.1)"))
        }
        val survived = results.filterNot { it.killed }
        val rate = results.count { it.killed }.toDouble() / results.size

        return listOf(
            if (survived.isEmpty()) {
                Check.pass(
                    "mutation",
                    "kill rate ${percent(rate)} — 탐지 분포: " +
                        results.flatMap { it.killedBy }.groupingBy { it }.eachCount(),
                )
            } else {
                Check.fail(
                    "mutation",
                    "살아남은 오답: " + survived.joinToString { "${it.name}(${it.kind})" } +
                        ". 이 오답을 잡는 테스트가 없다는 뜻이다",
                )
            },
        )
    }

    // --- §6.3 6. 시각화 이벤트 예산 ---

    private fun traceBudget(pkg: ProblemPackage, reference: String): List<Check> {
        val traced = run(pkg, reference, ExecutionMode.TRACE)
        val events = traced.trace?.events?.size ?: 0

        return listOf(
            when {
                traced.trace == null ->
                    Check.fail("trace-budget", "트레이스 실행이 결과를 내지 못했다")

                events > TraceManifest.EVENT_BUDGET ->
                    Check.fail("trace-budget", "이벤트 ${events}개로 예산을 넘는다")

                events == 0 ->
                    // 계측은 선택이다. 없다고 공개를 막지 않되 사실은 남긴다.
                    Check.pass("trace-budget", "계측 호출이 없다 (리플레이 없이 공개됨)")

                else -> Check.pass("trace-budget", "이벤트 ${events}개, 예산 안")
            },
        )
    }

    // --- 실행 ---

    private fun run(
        pkg: ProblemPackage,
        source: String,
        mode: ExecutionMode = ExecutionMode.JUDGE,
    ): ExecutionResult = engine.execute(
        ExecutionRequest(
            executionId = "validate-${pkg.problemVersionId}",
            submissionId = "validate-${pkg.problemVersionId}",
            attempt = 1,
            fencingToken = FencingToken(1),
            correlationId = "validate",
            problemVersionId = pkg.problemVersionId,
            packageDigest = pkg.packageDigest,
            language = Language.KOTLIN,
            source = source,
            signature = pkg.manifest.signature,
            limits = pkg.manifest.limits,
            groups = pkg.groups.map { RequestedGroup(it.policy, it.cases) },
            mode = mode,
        ),
    )

    private fun referenceSolution(problemId: String): String? =
        contentRoot.resolve(problemId).resolve("solutions/reference.kt")
            .takeIf { it.exists() }?.readText()

    /** `// kind: <KIND>` 첫 줄로 결함 종류를 읽는다. 별도 색인을 두면 파일과 어긋난다. */
    private fun mutants(problemId: String): List<Mutant> {
        val dir = contentRoot.resolve(problemId).resolve("mutants")
        if (!dir.exists()) return emptyList()

        return dir.listDirectoryEntries("*.kt")
            .filter { it.isRegularFile() }
            .sortedBy { it.name }
            .map { file ->
                val source = file.readText()
                val kind = Regex("""//\s*kind:\s*(\w+)""").find(source)?.groupValues?.get(1)
                    ?: "UNSPECIFIED"
                Mutant(file.name.removeSuffix(".kt"), kind, source)
            }
    }

    // --- §6.3 7. 검증 산출물 digest ---

    private fun report(
        pkg: ProblemPackage,
        checks: List<Check>,
        mutations: List<MutationResult>,
    ): ValidationReport {
        val passed = checks.all { it.passed }
        val digest = MessageDigest.getInstance("SHA-256").apply {
            update(pkg.packageDigest.toByteArray())
            // 범위를 섞는다. 빠른 검사로 만든 digest 가 공개 대조를 통과하면, 성능
            // 그룹을 한 번도 돌리지 않은 문제가 공개될 수 있다.
            update(scope.name.toByteArray())
            checks.sortedBy { it.stage }.forEach { update("${it.stage}=${it.passed}".toByteArray()) }
        }.digest().joinToString("") { "%02x".format(it) }

        return ValidationReport(
            problemVersionId = pkg.problemVersionId,
            packageDigest = pkg.packageDigest,
            passed = passed,
            checks = checks,
            mutations = mutations,
            reportDigest = digest,
        )
    }

    private fun percent(ratio: Double) = "%.0f%%".format(ratio * 100)

    private data class Mutant(val name: String, val kind: String, val source: String)

    private companion object {
        /** 정답 풀이가 제한 시간의 이 비율을 넘게 쓰면 공개를 막는다. */
        const val MAX_REFERENCE_TIME_RATIO = 0.5

        const val PERFORMANCE_GROUP = "performance"
    }
}

/**
 * 검증 보고서 (§6.3 7단계).
 *
 * [reportDigest] 는 패키지 내용과 각 단계의 결과에서 계산한다. 제어 영역은 공개할 때
 * 이 digest 를 대조하므로, 패키지를 고치면 보고서가 무효가 되어 다시 검증해야 한다.
 */
data class ValidationReport(
    val problemVersionId: String,
    val packageDigest: String,
    val passed: Boolean,
    val checks: List<Check>,
    val mutations: List<MutationResult>,
    val reportDigest: String,
)

data class Check(val stage: String, val passed: Boolean, val detail: String) {
    companion object {
        fun pass(stage: String, detail: String) = Check(stage, true, detail)
        fun fail(stage: String, detail: String) = Check(stage, false, detail)
    }
}

data class MutationResult(
    val name: String,
    val kind: String,
    val killed: Boolean,
    val killedBy: List<String>,
    val verdicts: List<String>,
)
