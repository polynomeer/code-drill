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
