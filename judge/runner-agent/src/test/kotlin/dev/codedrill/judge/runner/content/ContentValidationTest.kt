package dev.codedrill.judge.runner.content

import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.runner.execution.ExecutionEngine
import dev.codedrill.judge.runner.execution.adapter.KotlinAdapter
import dev.codedrill.judge.runner.execution.sandbox.ProcessSandbox
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 콘텐츠 검증 파이프라인 (기술 설계서 §6.3).
 *
 * 저장소의 모든 문제가 공개 가능한 상태인지 상시로 확인한다. 문제를 추가하면 자동으로
 * 그 문제도 검사한다 — 목록을 따로 관리하지 않는다.
 *
 * 이 검사가 없던 동안 `island-count` 의 공개 샘플 하나가 틀린 기대값을 갖고 있었고,
 * 정답 풀이가 오답 판정을 받았다. **틀린 테스트 데이터는 사용자가 자기 코드를 의심하게
 * 만든다.**
 */
class ContentValidationTest {

    private val validator = ContentValidator(
        engine = ExecutionEngine(
            adapters = mapOf(Language.KOTLIN to KotlinAdapter()),
            sandboxes = { ProcessSandbox() },
        ),
        contentRoot = Path.of("../../content/problems"),
    )

    @Test
    fun `모든 문제가 검증 파이프라인을 통과한다`() {
        val reports = validator.validateAll()

        assertTrue(reports.isNotEmpty(), "검사할 문제를 찾지 못했다")

        val failures = reports.filterNot { it.passed }.map { report ->
            report.problemVersionId + ": " +
                report.checks.filterNot { it.passed }.joinToString { "${it.stage} — ${it.detail}" }
        }

        assertTrue(failures.isEmpty(), "공개할 수 없는 문제:\n" + failures.joinToString("\n"))
    }

    @Test
    fun `대표 오답이 전부 잡힌다`() {
        val survived = validator.validateAll().flatMap { report ->
            report.mutations.filterNot { it.killed }.map { "${report.problemVersionId}/${it.name}" }
        }

        assertTrue(
            survived.isEmpty(),
            "살아남은 오답은 그것을 잡는 테스트가 없다는 뜻이다: $survived",
        )
    }

    @Test
    fun `보고서 digest 는 같은 패키지에서 같은 값이다`() {
        val first = validator.validate("two-sum")
        val second = validator.validate("two-sum")

        // 공개 시 대조하는 값이므로 실행마다 달라지면 검증 자체가 무의미해진다 (§3.2).
        assertTrue(first.reportDigest == second.reportDigest, "digest 가 흔들린다")
    }
}
