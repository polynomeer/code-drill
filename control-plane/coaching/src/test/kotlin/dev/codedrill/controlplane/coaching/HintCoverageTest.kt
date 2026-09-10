package dev.codedrill.controlplane.coaching

import dev.codedrill.platform.problempackage.ProblemPackageLoader
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 모든 문제가 코칭할 수 있는 상태인가 (FR-802).
 *
 * 세션은 **도울 수 있는 역량 중에서** 초점을 고른다. 그러니 사다리가 전부 비면 세션이
 * 열리기는 하는데 아무것도 못 한다 — 실패하지 않고 조용히 쓸모없어지는 종류의 구멍이라,
 * 문제를 추가할 때 여기서 걸리게 해 둔다.
 */
class HintCoverageTest {

    private val root = Path.of("../../content/problems")
    private val loader = ProblemPackageLoader(root)
    private val ladder = HintLadder(loader)

    @Test
    fun `모든 문제가 자기 역량 중 최소 하나는 코칭할 수 있다`() {
        val problems = root.listDirectoryEntries().filter { it.isDirectory() }.map { it.name }.sorted()
        assertTrue(problems.size >= 38, "문제가 ${problems.size}개뿐이다")

        val uncoachable = problems.filter { id ->
            ladder.coachable(id, loader.load(id).catalog.competencies).isEmpty()
        }
        assertTrue(uncoachable.isEmpty(), "도울 것이 없는 문제: $uncoachable")
    }

    @Test
    fun `어느 힌트도 코드를 주지 않는다`() {
        // 주석으로만 적어 둔 규칙은 다음 사람이 문제를 추가할 때 지켜지지 않는다.
        // 코드를 주는 순간 그것은 도움이 아니라 정답이고, 그 뒤의 증거는 아무것도 재지 못한다.
        val code = Regex("""\bfun\s+\w+\s*\(|\bfor\s*\(|\bwhile\s*\(|\breturn\b|[{};]""")
        val offenders = root.listDirectoryEntries().filter { it.isDirectory() }
            .flatMap { dir -> loader.hints(dir.name).values.flatten().map { dir.name to it } }
            .filter { (_, text) -> code.containsMatchIn(text) }
            .map { (id, text) -> "$id: $text" }

        assertTrue(offenders.isEmpty(), "코드가 섞인 힌트: $offenders")
    }

    @Test
    fun `평문이다 — 화면이 마크다운을 그리지 않는다`() {
        // 별표를 그대로 보여 준 적이 있다. 강조하려고 쓴 것이 화면에서는 잡음이 된다.
        val marked = root.listDirectoryEntries().filter { it.isDirectory() }
            .flatMap { dir -> loader.hints(dir.name).values.flatten().map { dir.name to it } }
            .filter { (_, text) -> text.contains("**") || text.contains("`") }
            .map { (id, text) -> "$id: $text" }

        assertTrue(marked.isEmpty(), "마크다운이 섞인 힌트: $marked")
    }

    @Test
    fun `저작한 사다리는 카탈로그가 선언한 역량에만 붙는다`() {
        val stray = root.listDirectoryEntries().filter { it.isDirectory() }.mapNotNull { dir ->
            val declared = loader.load(dir.name).catalog.competencies.toSet()
            // 선언하지 않은 역량에 힌트를 쓰면 그 힌트는 영영 보이지 않는다 — 초점은
            // 카탈로그가 선언한 역량 중에서만 고르기 때문이다.
            loader.hints(dir.name).keys.minus(declared).takeIf { it.isNotEmpty() }
                ?.let { "${dir.name}: $it" }
        }
        assertTrue(stray.isEmpty(), "닿지 않는 힌트: $stray")
    }
}
