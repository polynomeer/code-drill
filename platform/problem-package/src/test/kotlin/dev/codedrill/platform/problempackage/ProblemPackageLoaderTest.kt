package dev.codedrill.platform.problempackage

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProblemPackageLoaderTest {

    private val contentRoot: Path = Path.of("../../content/problems")
    private val loader = ProblemPackageLoader(contentRoot)

    @Test
    fun `two-sum 패키지를 읽는다`() {
        val pkg = loader.load("two-sum")

        assertEquals("two-sum@1", pkg.problemVersionId)
        assertEquals(3, pkg.groups.size)
        assertEquals(listOf("sample", "boundary", "hidden"), pkg.groups.map { it.policy.id })
        assertEquals(2, pkg.group("sample").cases.size)
    }

    @Test
    fun `digest 는 같은 내용에서 같은 값이 나온다`() {
        assertEquals(loader.load("two-sum").packageDigest, loader.load("two-sum").packageDigest)
    }

    @Test
    fun `공개 케이스만 노출된다`() {
        val pkg = loader.load("two-sum")
        val public = pkg.publicCases()

        assertEquals(2, public.size)
        assertTrue(public.all { it.groupId == "sample" }, "숨은 그룹 입력이 새어나가면 안 된다")
    }

    @Test
    fun `weight 합이 100 이 아니면 거부한다`() {
        val broken = createTempDirectory("pkg")
        val dir = broken.resolve("bad").also { it.createDirectories() }
        dir.resolve("tests").resolve("only").createDirectories()
        dir.resolve("statement.md").writeText("x")
        dir.resolve("tests/only/01.json").writeText("""{"args":[[1,1],2],"expected":[0,1]}""")
        dir.resolve("manifest.yaml").writeText(
            """
            id: bad
            version: 1
            title: 잘못된 패키지
            statement: statement.md
            limits: { timeMillis: 1000, memoryMb: 128, outputBytes: 1024 }
            signature:
              name: solve
              parameters: [{ name: nums, type: INT_ARRAY }, { name: target, type: INT }]
              returns: INT_ARRAY
            groups:
              - { id: only, weight: 40, visibility: PUBLIC, aggregation: ALL_OR_NOTHING, stopPolicy: CONTINUE }
            """.trimIndent(),
        )

        assertFailsWith<IllegalArgumentException> { ProblemPackageLoader(broken).load("bad") }
    }
}
