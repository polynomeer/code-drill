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
 *
 * [ContentValidator.Scope.FAST] 로 돈다. 이 스위트가 답하는 질문은 "이 문제를 공개해도
 * 되는가"가 아니라 **"저장소의 문제들이 아직 앞뒤가 맞는가"**다. 전체 검증은 문제 수에
 * 비례해 몇십 분이 걸리는데, 그 시간을 빌드마다 치르면 아무도 빌드를 돌리지 않게 되고
 * 결국 이 빠른 검사도 함께 사라진다.
 *
 * 돌연변이 분석과 성능 그룹은 `./gradlew :judge:runner-agent:validateContent` 가 맡는다.
 * 공개는 그 보고서의 digest 대조를 통과해야만 되므로(§3.2), 오답을 잡지 못하는 문제가
 * 공개될 길은 없다.
 */
class ContentValidationTest {

    private val validator = ContentValidator(
        engine = ExecutionEngine(
            adapters = mapOf(Language.KOTLIN to KotlinAdapter()),
            sandboxes = { ProcessSandbox() },
        ),
        contentRoot = Path.of("../../content/problems"),
        scope = ContentValidator.Scope.FAST,
    )

    /**
     * 파이프라인은 한 번만 돌린다.
     *
     * 검사마다 다시 돌리면 34개 문제를 그 횟수만큼 실행하게 된다. 같은 사실을 두 번
     * 계산할 이유가 없다.
     */
    private val reports by lazy { validator.validateAll() }

    @Test
    fun `모든 문제가 검증 파이프라인을 통과한다`() {
        assertTrue(reports.isNotEmpty(), "검사할 문제를 찾지 못했다")

        val failures = reports.filterNot { it.passed }.map { report ->
            report.problemVersionId + ": " +
                report.checks.filterNot { it.passed }.joinToString { "${it.stage} — ${it.detail}" }
        }

        assertTrue(failures.isEmpty(), "공개할 수 없는 문제:\n" + failures.joinToString("\n"))
    }

    /**
     * 돌연변이 분석 자체가 동작하는지는 한 문제로 확인한다.
     *
     * 저장소 전체의 오답이 전부 잡히는지는 `validateContent` 가 본다. 여기서 확인하는
     * 것은 **파이프라인이 오답을 실제로 죽인다**는 사실이며, 그건 한 문제면 충분하다.
     */
    @Test
    fun `전체 검증은 대표 오답을 잡는다`() {
        val report = fullValidator.validate("two-sum")

        assertTrue(report.mutations.isNotEmpty(), "오답을 하나도 돌리지 않았다")
        assertTrue(
            report.mutations.all { it.killed },
            "살아남은 오답: " + report.mutations.filterNot { it.killed }.map { it.name },
        )
    }

    @Test
    fun `보고서 digest 는 같은 패키지에서 같은 값이다`() {
        val first = validator.validate("two-sum")
        val second = validator.validate("two-sum")

        // 공개 시 대조하는 값이므로 실행마다 달라지면 검증 자체가 무의미해진다 (§3.2).
        assertTrue(first.reportDigest == second.reportDigest, "digest 가 흔들린다")
    }

    @Test
    fun `빠른 검사의 보고서로는 공개할 수 없다`() {
        // 범위가 digest 에 섞이지 않으면, 성능 그룹도 오답도 한 번 돌리지 않은 문제가
        // 공개될 수 있다 (§3.2 공개는 보고서 digest 대조를 통과해야 한다).
        assertTrue(
            validator.validate("two-sum").reportDigest != fullValidator.validate("two-sum").reportDigest,
            "빠른 검사와 전체 검증의 digest 가 같다",
        )
    }

    private val fullValidator by lazy {
        ContentValidator(
            engine = ExecutionEngine(
                adapters = mapOf(Language.KOTLIN to KotlinAdapter()),
                sandboxes = { ProcessSandbox() },
            ),
            contentRoot = Path.of("../../content/problems"),
        )
    }
}
