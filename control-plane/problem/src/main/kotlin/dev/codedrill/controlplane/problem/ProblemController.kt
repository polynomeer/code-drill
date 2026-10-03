package dev.codedrill.controlplane.problem

import dev.codedrill.platform.common.Principal
import dev.codedrill.platform.problempackage.Competency
import dev.codedrill.platform.problempackage.Difficulty
import dev.codedrill.platform.problempackage.ProblemNumbers
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
     * 목록의 진실 원천은 패키지 디렉터리다. 난이도·태그·역량은 `catalog.yaml` 에서, 번호는
     * `numbers.yaml` 에서 오고, 정답률과 완료 상태는 제출 도메인이 [ProblemProgress] 로 답한다.
     *
     * 필터는 **모두 AND 로 겹치고, 같은 종류 안에서는 OR** 다. `difficulty=EASY,MEDIUM`
     * 은 둘 중 하나, `tags=array&difficulty=EASY` 는 둘 다 — 사람이 필터를 여러 개 켤 때
     * 기대하는 것이 그것이다.
     *
     * ## 페이지를 넘기는 두 방식
     *
     * 기본은 **커서**다. 정렬 키는 id 오름차순으로 고정한다 — 커서가 의미를 가지려면 같은 요청이
     * 늘 같은 순서를 내야 하고 (§9.1), 값이 바뀌는 키(정답률)로 커서를 넘기면 항목이 건너뛰거나
     * 겹친다. 끝까지 빠짐없이 돌아야 하는 쪽(스모크, 도구)이 이것을 쓴다.
     *
     * `sort` 나 `page` 를 주면 **쪽 번호** 방식이다. 사람이 표를 보며 "정답률 낮은 순, 3쪽"으로
     * 가는 화면을 위한 것이다 (docs/ui-overhaul.md §6.1). 쪽을 넘기는 사이 정답률이 바뀌면 한
     * 줄이 밀릴 수 있다 — 사람이 훑는 표에서는 그것이 커서를 못 쓰는 것보다 작은 문제다.
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
        @RequestParam(required = false) sort: Sort?,
        @RequestParam(required = false) order: Order?,
        @RequestParam(required = false) page: Int?,
        @RequestAttribute(name = Principal.ATTRIBUTE, required = false) principal: Principal?,
    ): ProblemPage {
        val size = Cursor.limitOf(limit)
        val (base, matched) = select(query, difficulty, tags, competency, status, principal)

        if (sort != null || page != null) {
            val sorted = sorted(matched, sort ?: Sort.NUMBER, order ?: Order.ASC)
            val pageCount = maxOf(1, (sorted.size + size - 1) / size)
            val current = (page ?: 1).coerceIn(1, pageCount)
            return ProblemPage(
                items = sorted.drop((current - 1) * size).take(size),
                nextCursor = null,
                total = matched.size,
                tags = facets(base),
                page = current,
                pageCount = pageCount,
            )
        }

        val after = Cursor.decode(cursor)?.firstOrNull()
        val rest = matched.filter { after == null || it.id > after }
        val items = rest.take(size)

        return ProblemPage(
            items = items,
            nextCursor = if (rest.size > size) Cursor.encode(items.last().id) else null,
            // **페이지 크기가 아니라 조건에 맞는 수다** (FR-202). 이것을 페이지 크기로
            // 표시하면 20개 넘는 결과가 늘 "20개"로 보이고, 필터를 좁혀도 숫자가 움직이지
            // 않아 사용자는 필터가 듣지 않는다고 읽는다.
            total = matched.size,
            tags = facets(base),
        )
    }

    /**
     * 지금 조건에서 아무 문제 하나 (docs/ui-overhaul.md §6.1 "아무 문제나").
     *
     * 로그인했으면 **안 푼 문제에서** 고른다 — 이미 푼 문제로 데려가면 "아무거나"를 누른 이유가
     * 사라진다. 다 풀었으면 푼 문제에서라도 고른다. 조건에 맞는 문제가 없으면 404 다.
     */
    @GetMapping("/random")
    fun random(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) difficulty: List<Difficulty>?,
        @RequestParam(required = false) tags: List<String>?,
        @RequestParam(required = false) competency: List<Competency>?,
        @RequestParam(required = false) status: Status?,
        @RequestAttribute(name = Principal.ATTRIBUTE, required = false) principal: Principal?,
    ): ResponseEntity<ProblemSummary> {
        val (_, matched) = select(query, difficulty, tags, competency, status, principal)
        val pool = matched.filterNot { it.solved }.ifEmpty { matched }
        return pool.randomOrNull()?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    /** 태그 후보를 셀 집합(태그 필터 전)과 결과(태그 필터 후). */
    private fun select(
        query: String?,
        difficulty: List<Difficulty>?,
        tags: List<String>?,
        competency: List<Competency>?,
        status: Status?,
        principal: Principal?,
    ): Pair<List<ProblemSummary>, List<ProblemSummary>> {
        // 로그인하지 않았으면 완료 상태를 묻지 않는다. 물어봐야 답이 없다.
        val solved = principal?.let(Principal::id)?.let(progress::solvedBy).orEmpty()
        val accuracy = progress.accuracy()
        val numbers = ProblemNumbers.load(Path.of(contentRoot))

        // 태그를 뺀 나머지 조건까지만 좁힌 집합. 태그 후보를 여기서 센다.
        val base = availableProblems()
            .map { id -> ProblemSummary.of(packages.load(id), numbers.of(id), accuracy[id], id in solved) }
            .filter { it.matches(query) }
            .filter { difficulty.isNullOrEmpty() || it.difficulty in difficulty }
            .filter { competency.isNullOrEmpty() || it.competencies.any { c -> c in competency } }
            .filter { matchesStatus(it, status, principal) }

        val matched = base.filter { tags.isNullOrEmpty() || it.tags.any { tag -> tag in tags } }
        return base to matched
    }

    /**
     * 정렬. 값이 같으면 번호로, 번호가 없으면 id 로 가른다 — 같은 요청이 같은 순서를 내야 쪽을
     * 넘겨도 줄이 섞이지 않는다.
     *
     * **정답률이 없는(표본이 적은) 문제는 방향과 상관없이 맨 뒤다.** 모르는 값을 0% 로 쳐서
     * "가장 어려운 문제"로 올리면 그것은 정렬이 아니라 거짓말이다 (No false precision).
     */
    private fun sorted(items: List<ProblemSummary>, sort: Sort, order: Order): List<ProblemSummary> {
        val tie = compareBy<ProblemSummary>({ it.number ?: Int.MAX_VALUE }, { it.id })
        val key: Comparator<ProblemSummary> = when (sort) {
            Sort.NUMBER -> tie
            Sort.TITLE -> compareBy { it.title }
            Sort.DIFFICULTY -> compareBy { it.difficulty }
            Sort.ACCURACY -> compareBy { it.solvedRate }
            Sort.SOLVERS -> compareBy { it.solvedCount }
        }
        val directed = if (order == Order.DESC) key.reversed() else key
        val (known, unknown) = items.partition { sort != Sort.ACCURACY || it.solvedRate != null }
        return known.sortedWith(directed.then(tie)) + unknown.sortedWith(tie)
    }

    /**
     * 지금 조건에서 **결과가 있는** 태그와 그 수.
     *
     * 태그 필터를 적용하기 **전** 집합에서 센다. 적용한 뒤에 세면 태그 하나를 고르는 순간
     * 다른 태그가 전부 사라져 두 번째 태그를 고를 수 없다 — 같은 종류 안에서 OR 로 묶는
     * 필터에서는 그게 맞지 않는다.
     *
     * 어휘 전체를 내려보내지 않는 이유는, 결과가 0인 버튼이 대부분이면 그것은 필터가
     * 아니라 목록이 하나 더 생긴 것이기 때문이다.
     */
    private fun facets(base: List<ProblemSummary>): Map<String, Int> =
        base.flatMap { it.tags }
            .groupingBy { it }
            .eachCount()
            .toSortedMap()

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

    /** 쪽 번호 방식의 정렬 키. */
    enum class Sort { NUMBER, TITLE, DIFFICULTY, ACCURACY, SOLVERS }

    enum class Order { ASC, DESC }

    @GetMapping("/{slug}")
    fun detail(@PathVariable slug: String): ResponseEntity<ProblemDetail> {
        if (slug !in availableProblems()) return ResponseEntity.notFound().build()
        val number = ProblemNumbers.load(Path.of(contentRoot)).of(slug)
        return ResponseEntity.ok(ProblemDetail.of(packages.load(slug), number))
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
    /** 사람이 부르는 번호 (numbers.yaml). 아직 매기지 않았으면 null — 검증이 공개를 막는다. */
    val number: Int?,
    val version: Int,
    val title: String,
    val difficulty: Difficulty,
    val tags: List<String>,
    val competencies: List<Competency>,
    /** 표본이 적으면 null. 이유는 [ProblemProgress.Accuracy.rate] 에 있다. */
    val solvedRate: Double?,
    /** 맞힌 사람 수. 사람 수다 — 제출 수가 아니다 ([ProblemProgress.accuracy]). */
    val solvedCount: Int,
    /** 부르는 사람이 한 번이라도 맞혔는지. 로그인하지 않았으면 항상 false 다. */
    val solved: Boolean,
) {

    /**
     * 제목과 id 에서 부분 일치를 본다. 대소문자는 구분하지 않는다.
     *
     * 숫자만 쳤으면(`1042`, `#1042`) 번호로도 찾는다 — 번호는 사람이 문제를 부르는 이름이다.
     * 번호는 부분 일치가 아니라 일치다. `10` 을 친 사람에게 1000~1099 를 다 보여 주면 찾던
     * 것이 묻힌다.
     */
    fun matches(query: String?): Boolean {
        if (query.isNullOrBlank()) return true
        val needle = query.trim().lowercase()
        needle.removePrefix("#").toIntOrNull()?.let { if (it == number) return true }
        return title.lowercase().contains(needle) || id.lowercase().contains(needle)
    }

    companion object {
        fun of(
            pkg: ProblemPackage,
            number: Int?,
            accuracy: ProblemProgress.Accuracy?,
            solved: Boolean,
        ) = ProblemSummary(
            id = pkg.manifest.id,
            number = number,
            version = pkg.manifest.version,
            title = pkg.manifest.title,
            difficulty = pkg.catalog.difficulty,
            tags = pkg.catalog.tags,
            competencies = pkg.catalog.competencies,
            solvedRate = accuracy?.rate,
            solvedCount = accuracy?.solved ?: 0,
            solved = solved,
        )
    }
}

data class ProblemDetail(
    val id: String,
    val number: Int?,
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
        fun of(pkg: ProblemPackage, number: Int?): ProblemDetail {
            val signature = pkg.manifest.signature
            return ProblemDetail(
                id = pkg.manifest.id,
                number = number,
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

/**
 * 문제 목록 응답 (PRD FR-201~203).
 *
 * 공용 [dev.codedrill.platform.common.Page] 를 쓰지 않는다. 이 화면은 **총 개수와 태그
 * 후보**가 함께 있어야 쓸모가 있고, 그 둘은 다른 목록에는 뜻이 없다. 공용 타입에 화면
 * 하나를 위한 칸을 늘리면 그 칸은 다른 모든 목록에서 늘 null 이 된다.
 */
data class ProblemPage(
    val items: List<ProblemSummary>,
    /** 다음 페이지 커서. null 이면 마지막 페이지다. */
    val nextCursor: String?,
    /** 조건에 맞는 전체 개수. 이번 페이지의 개수가 아니다. */
    val total: Int,
    /** 태그 → 이 조건에서의 문제 수. */
    val tags: Map<String, Int>,
    /** 쪽 번호 방식일 때 지금 쪽(1부터). 커서 방식이면 null. */
    val page: Int? = null,
    /** 쪽 번호 방식일 때 전체 쪽 수. 커서 방식이면 null. */
    val pageCount: Int? = null,
)

/** 공개 예제. 문제 패키지의 케이스는 항상 기대 출력을 갖는다 (TestCase.expected). */
data class SampleCase(val id: String, val args: List<Any>, val expected: Any?)

data class GroupInfo(val id: String, val weight: Int, val aggregation: String, val caseCount: Int)
