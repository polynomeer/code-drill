package dev.codedrill.controlplane.problem

import dev.codedrill.platform.problempackage.ProblemPackage
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 문제 조회 API (기술 설계서 §9.2).
 *
 * 응답 DTO 는 공개 그룹의 케이스만 담는다. 숨은 테스트를 API 로 흘리지 않는 책임은
 * 이 경계에 있다 (§9.1).
 */
@RestController
@RequestMapping("/api/v1/problems")
class ProblemController(private val packages: ProblemPackageLoader) {

    @GetMapping
    fun list(): List<ProblemSummary> = SLICE_PROBLEMS.map { ProblemSummary.of(packages.load(it)) }

    @GetMapping("/{slug}")
    fun detail(@PathVariable slug: String): ResponseEntity<ProblemDetail> {
        if (slug !in SLICE_PROBLEMS) return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ProblemDetail.of(packages.load(slug)))
    }

    private companion object {
        /**
         * 슬라이스는 문제 하나만 다룬다 (§16.2). 문제 목록·검색은 Problem 모듈이 DB 를
         * 갖게 되는 단계에서 붙인다.
         */
        val SLICE_PROBLEMS = listOf("two-sum")
    }
}

data class ProblemSummary(val id: String, val version: Int, val title: String) {
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
                samples = pkg.publicCases().map { SampleCase(it.id, it.args.toString(), it.expected.toString()) },
            )
        }

        private fun dev.codedrill.platform.problempackage.ValueType.kotlinType() = when (this) {
            dev.codedrill.platform.problempackage.ValueType.INT -> "Int"
            dev.codedrill.platform.problempackage.ValueType.INT_ARRAY -> "IntArray"
        }
    }
}

data class SampleCase(val id: String, val args: String, val expected: String)
