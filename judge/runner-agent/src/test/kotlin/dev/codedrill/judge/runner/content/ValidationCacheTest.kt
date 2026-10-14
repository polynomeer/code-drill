package dev.codedrill.judge.runner.content

import dev.codedrill.platform.problempackage.DefectKind
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/** 검증 캐시 — 바뀐 것은 반드시 다시 재고, 바뀌지 않은 것만 재사용한다. */
class ValidationCacheTest {

    private fun report(passed: Boolean) = ValidationReport(
        problemVersionId = "two-sum@1",
        packageDigest = "p",
        passed = passed,
        checks = listOf(Check("structure", passed, "x")),
        mutations = listOf(MutationResult("m", DefectKind.WRONG_BRANCH, true, listOf("t"), listOf("WRONG_ANSWER"))),
        reportDigest = "r",
    )

    private fun pkg(root: Path): Path = root.resolve("two-sum").also {
        it.resolve("mutants").createDirectories()
        it.resolve("manifest.yaml").writeText("id: two-sum")
        it.resolve("mutants/off-by-one.kt").writeText("fun a() = 1")
    }

    @Test
    fun `패키지의 어느 파일이 바뀌어도 키가 바뀐다 — packageDigest 밖의 오답까지`() {
        val dir = pkg(Files.createTempDirectory("pkg"))
        val before = ValidationCache.directoryFingerprint(dir)
        assertEquals(before, ValidationCache.directoryFingerprint(dir))
        dir.resolve("mutants/off-by-one.kt").writeText("fun a() = 2")
        assertNotEquals(before, ValidationCache.directoryFingerprint(dir))
    }

    @Test
    fun `선수가 없는 문제를 더해도 공용 지문은 그대로이고, 선수 관계나 어휘가 바뀌면 바뀐다`() {
        val base = mapOf("a" to emptyList<String>(), "b" to listOf("a"))
        val all = { _: String -> true }
        val before = ValidationCache.sharedFingerprint("tags: [array]", base, all)
        assertEquals(before, ValidationCache.sharedFingerprint("tags: [array]", base + ("c" to emptyList()), all))
        assertNotEquals(before, ValidationCache.sharedFingerprint("tags: [array]", base + ("c" to listOf("a")), all))
        assertNotEquals(before, ValidationCache.sharedFingerprint("tags: [array, graph]", base, all))
        // 가리키던 선수 문제가 사라지면 그 문제의 검증 결과가 달라진다
        assertNotEquals(before, ValidationCache.sharedFingerprint("tags: [array]", base) { it != "a" })
    }

    @Test
    fun `검증기나 툴체인이 바뀌면 같은 패키지도 다시 잰다`() {
        val dir = Files.createTempDirectory("cp")
        dir.resolve("A.class").writeText("v1")
        val before = ValidationCache.validatorFingerprint(dir.toString(), "jdk 21 | Python 3.12")
        assertNotEquals(before, ValidationCache.validatorFingerprint(dir.toString(), "jdk 21 | Python 3.13"))
        dir.resolve("A.class").writeText("v2")
        assertNotEquals(before, ValidationCache.validatorFingerprint(dir.toString(), "jdk 21 | Python 3.12"))
    }

    @Test
    fun `통과한 보고서만 담고, 이번에 쓰지 않은 항목은 지운다`() {
        val dir = Files.createTempDirectory("cache")
        val cache = ValidationCache(dir, "v")
        val pass = cache.key("problem", "two-sum", "f", "s")
        val fail = cache.key("problem", "move-zeros", "f", "s")
        cache.store(pass, report(passed = true))
        cache.store(fail, report(passed = false))
        assertEquals(report(passed = true), cache.find(pass))
        assertNull(cache.find(fail))

        dir.resolve("old.json").writeText("{}")
        val next = ValidationCache(dir, "v")
        next.find(pass)
        assertEquals(1, next.prune())
        assertEquals(listOf("$pass.json"), dir.listDirectoryEntries("*.json").map { it.fileName.toString() })
    }

    @Test
    fun `다시 묶은 jar 는 내용이 같으면 같은 지문이다 — 묶은 시각은 보지 않는다`() {
        fun jar(time: Long, content: String): Path {
            val file = Files.createTempFile("lib", ".jar")
            ZipOutputStream(Files.newOutputStream(file)).use { zip ->
                zip.putNextEntry(ZipEntry("A.class").apply { this.time = time })
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
            return file
        }
        val first = jar(1_000_000_000_000, "v1")
        val rebuilt = jar(1_700_000_000_000, "v1")
        assertNotEquals(Files.readAllBytes(first).toList(), Files.readAllBytes(rebuilt).toList())
        assertEquals(ValidationCache.jarFingerprint(first), ValidationCache.jarFingerprint(rebuilt))
        assertNotEquals(ValidationCache.jarFingerprint(first), ValidationCache.jarFingerprint(jar(1_000_000_000_000, "v2")))
    }
}
