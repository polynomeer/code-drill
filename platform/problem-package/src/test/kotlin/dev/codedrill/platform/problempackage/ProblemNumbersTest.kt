package dev.codedrill.platform.problempackage

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProblemNumbersTest {

    @Test
    fun `번호와 slug 를 양쪽으로 찾는다`() {
        val numbers = ProblemNumbers.parse(
            """
            # 주석
            1000: two-sum

            1001: max-subarray   # 줄 끝 주석
            """.trimIndent(),
        )
        assertEquals(1000, numbers.of("two-sum"))
        assertEquals("max-subarray", numbers.slugOf(1001))
        assertNull(numbers.of("missing"))
    }

    @Test
    fun `겹친 번호는 읽을 때 막는다`() {
        val error = assertFailsWith<IllegalArgumentException> {
            ProblemNumbers.parse("1000: two-sum\n1000: max-subarray")
        }
        assertTrue("겹친다" in error.message.orEmpty())
    }

    @Test
    fun `한 문제에 번호 둘은 막는다`() {
        assertFailsWith<IllegalArgumentException> { ProblemNumbers.parse("1000: two-sum\n1001: two-sum") }
    }

    @Test
    fun `모양이 다른 줄은 몇 번째 줄인지 말한다`() {
        val error = assertFailsWith<IllegalArgumentException> { ProblemNumbers.parse("1000: two-sum\ntwo-sum: 1001") }
        assertTrue("2번 줄" in error.message.orEmpty())
    }

    @Test
    fun `저장소의 모든 문제에 번호가 있다`() {
        val root = Path.of("../../content/problems")
        val numbers = ProblemNumbers.load(root)
        val problems = root.listDirectoryEntries()
            .filter { it.isDirectory() && it.resolve("manifest.yaml").exists() }
            .map { it.name }
            .toSet()
        assertEquals(emptySet(), problems - numbers.slugs, "번호가 없는 문제")
        assertEquals(emptySet(), numbers.slugs - problems, "없는 문제를 가리키는 번호")
    }
}
