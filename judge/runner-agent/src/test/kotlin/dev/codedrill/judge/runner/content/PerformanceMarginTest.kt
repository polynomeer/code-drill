package dev.codedrill.judge.runner.content

import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.runner.execution.ExecutionEngine
import dev.codedrill.judge.runner.execution.adapter.KotlinAdapter
import dev.codedrill.judge.runner.execution.sandbox.ProcessSandbox
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 자원 여유 측정 (기술 설계서 §6.3 돌연변이 분석, §12.1 재현성).
 *
 * 시간이나 메모리로 **잡히기는 하는데 아슬아슬하게** 잡히는 오답은 다른 머신에서 통과한다.
 * 그러면 그 문제의 공개 여부를 정한 것은 알고리즘이 아니라 그날의 부하와 힙 크기다.
 * 실제로 `gcd-of-array` 와 `count-primes` 가 그 상태로 공개까지 갔다.
 *
 * **저장소의 문제로는 이 코드가 검사되지 않는다.** 오답 13개가 전부 시간으로 잡히므로
 * 메모리 쪽 분기는 한 번도 실행되지 않는다. 그래서 고정물을 따로 둔다 — 한도를 자릿수로
 * 넘는 오답과 조금만 넘는 오답, 그리고 값으로 틀려서 애초에 잴 필요가 없는 오답이다.
 *
 * 고정물은 `src/test/resources` 에 있다. `content/problems` 아래에 두면 공개 대상이 되고,
 * 일부러 한도를 넘기게 만든 패키지가 사용자에게 보이는 목록에 섞인다.
 */
class PerformanceMarginTest {

    private val validator = ContentValidator(
        engine = ExecutionEngine(
            adapters = mapOf(Language.KOTLIN to KotlinAdapter()),
            sandboxes = { ProcessSandbox() },
        ),
        contentRoot = Path.of("src/test/resources/content-margin"),
    )

    @Test
    fun `한도를 자릿수로 넘는 오답은 통과시킨다`() {
        val report = validator.validate("mem-decisive")
        val mutant = report.mutations.first { it.name == "huge-alloc" }

        assertTrue(mutant.killed, "메모리를 자릿수로 넘겼는데 잡히지 않았다")
        assertEquals(
            true, mutant.memoryClearsProbe,
            "한도를 세 배로 늘려도 넘쳐야 한다 — 그래야 힙이 넉넉한 머신에서도 잡힌다",
        )
        assertTrue(
            report.checks.first { it.stage == "performance-margin" }.passed,
            "자릿수로 지는 오답을 막았다",
        )
    }

    @Test
    fun `한도를 조금만 넘는 오답은 막는다`() {
        val report = validator.validate("mem-thin")
        val mutant = report.mutations.first { it.name == "just-over" }

        // 잡히기는 한다. 그래서 kill rate 만 보면 멀쩡해 보인다 — 이 검사가 필요한 이유다.
        assertTrue(mutant.killed, "고정물이 잡히지도 않는다면 시험 자체가 성립하지 않는다")
        assertEquals(
            false, mutant.memoryClearsProbe,
            "세 배로 늘리면 들어가는 오답이다",
        )

        val check = report.checks.first { it.stage == "performance-margin" }
        assertTrue(!check.passed, "여유가 없는데 통과시켰다")
        assertTrue(
            "메모리를 3배로 주면 통과한다" in check.detail,
            "사유가 무엇을 고쳐야 하는지 말하지 않는다: ${check.detail}",
        )
        assertTrue(!report.passed, "이 보고서로는 공개할 수 없어야 한다")
    }

    @Test
    fun `값으로 틀리는 오답은 재지 않는다`() {
        // 자원 판정이 아니면 머신과 무관하게 늘 같은 결과다. 재는 것은 한도의 세 배씩
        // 실행을 더 돌리는 일이라, 잴 필요가 없는 것을 재면 검증만 느려진다.
        val mutant = validator.validate("mem-decisive")
            .mutations.first { it.name == "off-by-one--skips-last" }

        assertTrue(mutant.killed, "값이 틀린 오답이 잡히지 않았다")
        assertNull(mutant.timeMargin, "값으로 잡힌 오답의 시간 여유를 쟀다")
        assertNull(mutant.memoryClearsProbe, "값으로 잡힌 오답의 메모리 여유를 쟀다")
    }
}
