package dev.codedrill.controlplane.project

import dev.codedrill.platform.problempackage.ProjectPackageLoader
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.io.TempDir

/**
 * 키트 (로컬에서 풀기 1단계). **숨은 테스트가 새지 않는 것**이 첫째고, 받은 키트로 공개 테스트가 실제로 도는 것이
 * 둘째다 — 실행기는 저장소의 진짜 프로젝트로 돌려 본다. 시작 저장소는 정답이 아니므로(starter-fails) 실패로 끝나야 한다.
 */
class ProjectKitTest {

    private val loader = ProjectPackageLoader(Path.of("../../content/projects"))

    @Test
    fun `숨은 테스트는 어떤 경로로도 키트에 들어가지 않는다`() {
        for (id in loader.ids()) {
            val pkg = loader.load(id)
            val files = ProjectKit.files(pkg)
            // 시작 저장소에도 똑같이 있는 것(빈 `__init__.py` 같은)은 이미 공개다 — 숨은 것에만 있는 내용을 본다
            val hiddenContents = pkg.hidden.filter { (path, content) -> content.isNotBlank() && pkg.starter[path] != content }.values.toSet()
            for ((path, content) in files) {
                assertFalse(content in hiddenContents, "$id: $path 가 숨은 테스트의 내용이다")
            }
            for (hiddenPath in pkg.hidden.keys) {
                if (hiddenPath in pkg.starter) continue // 같은 경로의 공개 파일은 시작 저장소의 것이다
                assertFalse("$id/$hiddenPath" in files, "$id: 숨은 경로 $hiddenPath 가 키트에 있다")
            }
        }
    }

    @Test
    fun `시작 저장소 전부와 판 정보·하네스·안내가 프로젝트 폴더 하나에 든다`() {
        val pkg = loader.load("job-queue")
        val files = ProjectKit.files(pkg)
        for (path in pkg.starter.keys) assertTrue("job-queue/$path" in files, path)
        assertTrue("job-queue/.codedrill/project.json" in files)
        assertTrue("job-queue/.codedrill/harness/CodedrillHarness.kt" in files)
        assertTrue("job-queue/build.gradle.kts" in files)
        assertTrue("job-queue/CODEDRILL.md" in files)
        assertTrue(files.getValue("job-queue/.codedrill/project.json").contains(pkg.packageDigest))
        assertFalse(files.values.any { it.contains("@@") }, "채우지 않은 자리표시가 남았다")
    }

    @Test
    fun `같은 패키지의 키트는 같은 바이트다`() {
        val pkg = loader.load("cart-pricing")
        assertContentEquals(ProjectKit.zip(pkg), ProjectKit.zip(pkg))
        val names = ZipInputStream(ByteArrayInputStream(ProjectKit.zip(pkg))).use { zip ->
            generateSequence { zip.nextEntry }.map { it.name }.toList()
        }
        assertEquals(names.sorted(), names)
    }

    @Test
    fun `Python 키트 — 받은 그대로 공개 테스트가 돌고, 시작 저장소라 실패로 끝난다`(@TempDir dir: Path) {
        assumeTrue(has("python3"), "python3 가 없다")
        val root = unpack("cart-pricing", dir)
        val run = exec(root, "python3", ".codedrill/run.py")
        assertTrue("공개 테스트" in run.output && "통과" in run.output, run.output)
        assertEquals(1, run.exit, run.output)
    }

    @Test
    fun `Java 키트 — 받은 그대로 컴파일하고 공개 테스트가 돈다`(@TempDir dir: Path) {
        val root = unpack("account-transfers", dir)
        val java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        val run = exec(root, java, ".codedrill/Run.java")
        assertTrue("빌드 ✓" in run.output && "통과" in run.output, run.output)
        assertEquals(1, run.exit, run.output)
    }

    private fun unpack(id: String, dir: Path): Path {
        for ((path, content) in ProjectKit.files(loader.load(id))) {
            val file = dir.resolve(path)
            file.parent.createDirectories()
            file.writeText(content)
        }
        return dir.resolve(id)
    }

    private data class Run(val exit: Int, val output: String)

    private fun exec(root: Path, vararg command: String): Run {
        val process = ProcessBuilder(*command).directory(root.toFile()).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        assertTrue(process.waitFor(120, TimeUnit.SECONDS), "끝나지 않았다")
        return Run(process.exitValue(), output)
    }

    private fun has(command: String): Boolean =
        runCatching { ProcessBuilder(command, "--version").start().waitFor() == 0 }.getOrDefault(false)
}
