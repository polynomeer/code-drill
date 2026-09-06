package dev.codedrill.controlplane.problem

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
    @Value("\${codedrill.content.root}") private val contentRoot: String,
) {

    /**
     * 문제 목록 (기술 설계서 §9.2 검색·cursor 목록).
     *
     * 목록의 진실 원천은 아직 패키지 디렉터리다. 역량 필터는 Problem 모듈이 DB 와 역량
     * 태그를 갖게 되는 단계에 붙는다. 정렬 키는 id 오름차순으로 고정한다 — 커서가 의미를
     * 가지려면 같은 요청이 늘 같은 순서를 내야 한다 (§9.1).
     */
    @GetMapping
    fun list(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) limit: Int?,
    ): Page<ProblemSummary> {
        val size = Cursor.limitOf(limit)
        val after = Cursor.decode(cursor)?.firstOrNull()

        val matched = availableProblems()
            .map { ProblemSummary.of(packages.load(it)) }
            .filter { it.matches(query) }
            .filter { after == null || it.id > after }

        val items = matched.take(size)
        val nextCursor = if (matched.size > size) Cursor.encode(items.last().id) else null
        return Page(items, nextCursor)
    }

    @GetMapping("/{slug}")
    fun detail(@PathVariable slug: String): ResponseEntity<ProblemDetail> {
        if (slug !in availableProblems()) return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ProblemDetail.of(packages.load(slug)))
    }

    private fun availableProblems(): List<String> =
        Path.of(contentRoot).listDirectoryEntries()
            .filter { it.isDirectory() && it.resolve("manifest.yaml").exists() }
            .map { it.name }
            .sorted()
}

data class ProblemSummary(val id: String, val version: Int, val title: String) {

    /** 제목과 id 에서 부분 일치를 본다. 대소문자는 구분하지 않는다. */
    fun matches(query: String?): Boolean {
        if (query.isNullOrBlank()) return true
        val needle = query.trim().lowercase()
        return title.lowercase().contains(needle) || id.lowercase().contains(needle)
    }

    companion object {
        fun of(pkg: ProblemPackage) = ProblemSummary(
            id = pkg.manifest.id,
            version = pkg.manifest.version,
            title = pkg.manifest.title,
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
        }
    }
}

data class SampleCase(val id: String, val args: List<Any>, val expected: Any)

data class GroupInfo(val id: String, val weight: Int, val aggregation: String, val caseCount: Int)
