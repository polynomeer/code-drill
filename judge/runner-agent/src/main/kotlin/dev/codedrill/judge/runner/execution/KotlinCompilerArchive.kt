package dev.codedrill.judge.runner.execution

import dev.codedrill.judge.runner.execution.adapter.KotlinAdapter
import dev.codedrill.judge.runner.execution.adapter.RuntimeAdapter
import dev.codedrill.judge.runner.execution.sandbox.ProcessSandbox
import dev.codedrill.judge.runner.execution.sandbox.Sandbox
import dev.codedrill.judge.runner.execution.sandbox.SandboxSpec
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.writeText

/**
 * Kotlin 컴파일러의 클래스 아카이브(AppCDS)를 기동 때 한 번 만든다.
 *
 * 컴파일이 샌드박스 안으로 들어가면서 컴파일마다 JVM 이 새로 뜬다 (§5.5). K2 는 수천
 * 클래스를 읽고 검증하며 시작하는데, 그 시간이 작은 풀이의 컴파일 시간보다 길다. 한 번
 * 돌려 본 클래스를 아카이브로 남기면 다음부터는 매핑만 한다 — 같은 파일이 2.9초에서
 * 1.85초가 된다.
 *
 * **없어도 된다.** 아카이브가 없거나 JVM·클래스패스가 달라 맞지 않으면 JVM 은 조용히
 * 무시하고 그냥 돈다. 그래서 여기서 실패해도 기동은 계속한다. 다만 이미지를 올리면
 * 아카이브도 새로 만들어야 빨라지는데, 기동 때 늘 다시 만드니 그 일은 자동이다.
 *
 * 만드는 것도 샌드박스 안에서 한다. 아카이브는 그것을 만든 JVM 에서만 맞으므로 컴파일이
 * 도는 바로 그 이미지에서 만들어야 한다.
 */
class KotlinCompilerArchive(
    private val adapter: KotlinAdapter,
    private val sandbox: () -> Sandbox,
    private val archive: Path,
    /**
     * 작업 디렉터리를 만들 자리. Runner 가 컨테이너 안에서 돌면 임시 디렉터리는 호스트
     * 데몬에게 보이지 않아 빈 채로 마운트된다 — 실제로 그랬다. 채점과 같은 작업 루트를 쓴다.
     */
    private val workRoot: Path? = null,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun build() {
        val work = (workRoot?.resolve("compiler-archive")?.also { it.createDirectories() }
            ?: createTempDirectory("codedrill-cds-")).toRealPath()
        try {
            val sourceDir = work.resolve("src").also { it.createDirectories() }
            val outputDir = work.resolve("out").also { it.createDirectories() }
            sourceDir.resolve("Solution.kt").writeText(WARM_UP_SOURCE)

            val staging = outputDir.resolve(archive.fileName.toString())
            val step = adapter.archiveStep(sourceDir, outputDir, staging)
            val outcome = sandbox().exec(
                SandboxSpec(
                    command = step.command,
                    workDir = work,
                    readOnlyPaths = step.readOnlyPaths,
                    env = step.env,
                    memoryMb = step.memoryMb,
                    perCaseTimeoutMillis = step.timeoutMillis,
                    outputByteLimit = RuntimeAdapter.MAX_COMPILE_LOG_CHARS.toLong(),
                    writablePaths = listOf(outputDir),
                    pidsLimit = ExecutionEngine.COMPILER_PIDS_LIMIT,
                ),
            )
            if (outcome.exitCode != 0 || !staging.exists()) {
                log.warn(
                    "Kotlin 컴파일러 아카이브를 만들지 못했다 (exit {}). 컴파일은 아카이브 없이 돈다 — 느릴 뿐이다: {}",
                    outcome.exitCode, outcome.output.lines().firstOrNull { it.isNotBlank() },
                )
                return
            }
            archive.parent.createDirectories()
            // 통째로 바꾼다. 쓰다 만 아카이브를 컴파일이 집어 들면 JVM 이 거부하고 느리게 돈다.
            Files.move(staging, archive, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            log.info("Kotlin 컴파일러 아카이브 {} ({}MB)", archive, archive.fileSize() / 1_000_000)
        } catch (e: Exception) {
            log.warn("Kotlin 컴파일러 아카이브를 만들지 못했다: {}. 컴파일은 아카이브 없이 돈다", e.message)
        } finally {
            work.toFile().deleteRecursively()
        }
    }

    companion object {
        const val FILE_NAME = "kotlin-compiler.jsa"

        /**
         * 프로세스 샌드박스로 도는 곳(콘텐츠 검증, 테스트)을 위한 어댑터.
         *
         * [directory] 에 아카이브가 없으면 만들고, 있으면 그대로 쓴다. 낡은 아카이브(다른
         * JDK 나 컴파일러 판)는 JVM 이 무시하므로 틀리지는 않고 느릴 뿐이다 — 그때는
         * 디렉터리를 지우면 된다.
         */
        fun warmedAdapter(directory: Path): KotlinAdapter {
            val archive = directory.resolve(FILE_NAME)
            val adapter = KotlinAdapter(classArchive = archive)
            if (!archive.exists()) KotlinCompilerArchive(adapter, { ProcessSandbox() }, archive).build()
            return adapter
        }

        /**
         * 아카이브에 담길 클래스를 정하는 소스. 채점 풀이가 흔히 쓰는 것 — 클래스, 반복,
         * 컬렉션, 람다, 문자열 템플릿 — 을 한 번씩 지난다. 여기서 안 쓴 클래스는 아카이브에
         * 없을 뿐, 컴파일이 안 되는 것은 아니다.
         */
        val WARM_UP_SOURCE = """
            class Solution {
                fun twoSum(nums: IntArray, target: Int): IntArray {
                    val seen = HashMap<Int, Int>()
                    for ((i, n) in nums.withIndex()) {
                        seen[target - n]?.let { return intArrayOf(it, i) }
                        seen[n] = i
                    }
                    return intArrayOf()
                }
            }

            fun words(text: String): List<String> = text.split(" ").filter { it.isNotBlank() }.sorted()

            fun main() {
                val s = Solution().twoSum(intArrayOf(2, 7, 11, 15), 9)
                println("answer=${'$'}{s.joinToString(",")} words=${'$'}{words("b a")}")
            }
        """.trimIndent()
    }
}
