package dev.codedrill.controlplane.problem

import dev.codedrill.platform.common.Principal
import dev.codedrill.platform.problempackage.Competency
import dev.codedrill.platform.problempackage.Difficulty
import dev.codedrill.platform.problempackage.ProblemPackage
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import dev.codedrill.platform.common.Cursor
import dev.codedrill.platform.common.Page
import org.springframework.beans.factory.annotation.Value
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 문제 조회 API (기술 설계서 §9.2).
 *
 * 응답 DTO 는 공개 그룹의 케이스만 담는다. 숨은 테스트를 API 로 흘리지 않는 책임은
 * 이 경계에 있다 (§9.1).
 */
@RestController
@RequestMapping("/api/v1/problems")
class ProblemController(
    private val packages: ProblemPackageLoader,
    private val published: PublishedProblems,
    private val progress: ProblemProgress,
    @Value("\${codedrill.content.root}") private val contentRoot: String,
    /**
     * 공개 상태를 강제할지.
     *
     * `true` 면 §3.2 의 공개 흐름을 통과한 문제만 목록과 상세에 나온다. 개발 편의로 끌 수
     * 있게 두되, **기본값은 강제**다 — 검증하지 않은 문제가 사용자에게 보이는 경로를
     * 기본값으로 열어 두면 안 된다.
     */
    @Value("\${codedrill.content.require-publish:true}") private val requirePublish: Boolean,
) {

    /**
     * 문제 목록 (기술 설계서 §9.2, PRD FR-201~203).
     *
     * 목록의 진실 원천은 패키지 디렉터리다. 난이도·태그·역량은 `catalog.yaml` 에서 오고,
     * 정답률과 완료 상태는 제출 도메인이 [ProblemProgress] 로 답한다.
     *
     * 필터는 **모두 AND 로 겹치고, 같은 종류 안에서는 OR** 다. `difficulty=EASY,MEDIUM`
     * 은 둘 중 하나, `tags=array&difficulty=EASY` 는 둘 다 — 사람이 필터를 여러 개 켤 때
     * 기대하는 것이 그것이다.
     *
     * 정렬 키는 id 오름차순으로 고정한다. 커서가 의미를 가지려면 같은 요청이 늘 같은
     * 순서를 내야 한다 (§9.1). **정답률 정렬은 붙이지 않았다** — 정렬 키를 늘리려면 커서에
     * 그 키를 실어야 하고, 값이 바뀌는 키로 페이지를 넘기면 항목이 건너뛰거나 겹친다.
     */
    @GetMapping
    fun list(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) difficulty: List<Difficulty>?,
        @RequestParam(required = false) tags: List<String>?,
        @RequestParam(required = false) competency: List<Competency>?,
        @RequestParam(required = false) status: Status?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) limit: Int?,
        @RequestAttribute(name = Principal.ATTRIBUTE, required = false) principal: Principal?,
    ): Page<ProblemSummary> {
        val size = Cursor.limitOf(limit)
        val after = Cursor.decode(cursor)?.firstOrNull()

        // 로그인하지 않았으면 완료 상태를 묻지 않는다. 물어봐야 답이 없고, 그 상태로
        // status 필터를 걸면 조용히 빈 목록이 된다 — 그래서 아래에서 함께 막는다.
        val solved = principal?.let(Principal::id)?.let(progress::solvedBy).orEmpty()
        val accuracy = progress.accuracy()

        val matched = availableProblems()
            .map { id ->
                val pkg = packages.load(id)
                ProblemSummary.of(pkg, accuracy[id], id in solved)
            }
            .filter { it.matches(query) }
            .filter { difficulty.isNullOrEmpty() || it.difficulty in difficulty }
            .filter { tags.isNullOrEmpty() || it.tags.any { tag -> tag in tags } }
            .filter { competency.isNullOrEmpty() || it.competencies.any { c -> c in competency } }
            .filter { matchesStatus(it, status, principal) }
            .filter { after == null || it.id > after }

        val items = matched.take(size)
        val nextCursor = if (matched.size > size) Cursor.encode(items.last().id) else null
        return Page(items, nextCursor)
    }

    /**
     * 완료 상태 필터.
     *
     * 로그인하지 않았으면 **거르지 않는다.** 익명 사용자에게 "안 푼 문제만"은 전부를
     * 뜻하고 "푼 문제만"은 아무것도 아닌데, 후자를 빈 목록으로 돌려주면 사용자는 필터가
     * 고장 난 것으로 읽는다. 그럴 바에는 필터가 없는 것처럼 구는 편이 덜 거짓말이다.
     */
    private fun matchesStatus(summary: ProblemSummary, status: Status?, principal: Principal?): Boolean {
        if (status == null || principal == null) return true
        return when (status) {
            Status.SOLVED -> summary.solved
            Status.UNSOLVED -> !summary.solved
        }
    }

    /** 목록에서 거를 수 있는 완료 상태. */
    enum class Status { SOLVED, UNSOLVED }

    @GetMapping("/{slug}")
    fun detail(@PathVariable slug: String): ResponseEntity<ProblemDetail> {
        if (slug !in availableProblems()) return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ProblemDetail.of(packages.load(slug)))
    }

    /**
     * 사용자에게 보여도 되는 문제.
     *
     * 패키지가 디스크에 있다는 것과 공개됐다는 것은 다르다. 디렉터리에 파일을 놓는 것만으로
     * 문제가 공개되면 §6.3 검증과 §11.2 2인 승인이 모두 우회된다.
     */
    private fun availableProblems(): List<String> {
        val onDisk = Path.of(contentRoot).listDirectoryEntries()
            .filter { it.isDirectory() && it.resolve("manifest.yaml").exists() }
            .map { it.name }
            .sorted()

        if (!requirePublish) return onDisk
        val allowed = published.ids()
        return onDisk.filter { it in allowed }
    }
}

data class ProblemSummary(
    val id: String,
    val version: Int,
    val title: String,
    val difficulty: Difficulty,
    val tags: List<String>,
    val competencies: List<Competency>,
    /** 표본이 적으면 null. 이유는 [ProblemProgress.Accuracy.rate] 에 있다. */
    val solvedRate: Double?,
    /** 부르는 사람이 한 번이라도 맞혔는지. 로그인하지 않았으면 항상 false 다. */
    val solved: Boolean,
) {

    /** 제목과 id 에서 부분 일치를 본다. 대소문자는 구분하지 않는다. */
    fun matches(query: String?): Boolean {
        if (query.isNullOrBlank()) return true
        val needle = query.trim().lowercase()
        return title.lowercase().contains(needle) || id.lowercase().contains(needle)
    }

    companion object {
        fun of(
            pkg: ProblemPackage,
            accuracy: ProblemProgress.Accuracy?,
            solved: Boolean,
        ) = ProblemSummary(
            id = pkg.manifest.id,
            version = pkg.manifest.version,
            title = pkg.manifest.title,
            difficulty = pkg.catalog.difficulty,
            tags = pkg.catalog.tags,
            competencies = pkg.catalog.competencies,
            solvedRate = accuracy?.rate,
            solved = solved,
        )
    }
}

data class ProblemDetail(
    val id: String,
    val version: Int,
    val title: String,
    val statement: String,
    val timeMillis: Long,
    val memoryMb: Int,
    val signature: String,
    val samples: List<SampleCase>,
    /** 그룹별 배점과 채점 방식. 부분 점수 문제는 이게 보여야 전략을 세울 수 있다 (§6.2). */
    val groups: List<GroupInfo>,
) {
    companion object {
        fun of(pkg: ProblemPackage): ProblemDetail {
            val signature = pkg.manifest.signature
            return ProblemDetail(
                id = pkg.manifest.id,
                version = pkg.manifest.version,
                title = pkg.manifest.title,
                statement = pkg.statementMarkdown,
                timeMillis = pkg.manifest.limits.timeMillis,
                memoryMb = pkg.manifest.limits.memoryMb,
                signature = buildString {
                    append("fun ").append(signature.name).append('(')
                    append(signature.parameters.joinToString(", ") { "${it.name}: ${it.type.kotlinType()}" })
                    append("): ").append(signature.returns.kotlinType())
                },
                // 값을 그대로 내려보낸다. Kotlin 의 toString() 을 클라이언트가 되파싱하게
                // 만들면 표현 방식이 바뀔 때마다 조용히 깨진다.
                samples = pkg.publicCases().map { SampleCase(it.id, it.args, it.expected) },
                // 케이스 내용은 싣지 않는다. 배점 구조만 공개해도 전략은 세울 수 있다.
                groups = pkg.groups.map {
                    GroupInfo(it.policy.id, it.policy.weight, it.policy.aggregation.name, it.cases.size)
                },
            )
        }

        private fun dev.codedrill.platform.problempackage.ValueType.kotlinType() = when (this) {
            dev.codedrill.platform.problempackage.ValueType.INT -> "Int"
            dev.codedrill.platform.problempackage.ValueType.INT_ARRAY -> "IntArray"
            dev.codedrill.platform.problempackage.ValueType.STRING -> "String"
            dev.codedrill.platform.problempackage.ValueType.STRING_ARRAY -> "Array<String>"
            dev.codedrill.platform.problempackage.ValueType.INT_MATRIX -> "Array<IntArray>"
        }
    }
}

data class SampleCase(val id: String, val args: List<Any>, val expected: Any)

data class GroupInfo(val id: String, val weight: Int, val aggregation: String, val caseCount: Int)
