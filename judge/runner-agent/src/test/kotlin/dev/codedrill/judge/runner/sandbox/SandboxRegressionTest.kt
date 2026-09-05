package dev.codedrill.judge.runner.sandbox

import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.judge.runner.execution.ExecutionEngine
import dev.codedrill.judge.runner.execution.adapter.JavaAdapter
import dev.codedrill.judge.runner.execution.adapter.KotlinAdapter
import dev.codedrill.judge.runner.execution.adapter.PythonAdapter
import dev.codedrill.judge.runner.execution.sandbox.ContainerSandbox
import dev.codedrill.judge.runner.golden.GoldenSources
import dev.codedrill.platform.problempackage.Limits
import dev.codedrill.platform.problempackage.ProblemPackage
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

/**
 * 샌드박스 회귀 스위트 (기술 설계서 §11.4 Sandbox regression, §14.1 Sandbox).
 *
 * 격리가 실제로 서 있는지 **공격 코퍼스로** 확인한다. §11.1 위협 모델의 각 항목이
 * 여기 한 줄씩 대응한다.
 *
 * | §11.1 위협 | 이 스위트의 케이스 |
 * |---|---|
 * | 자원 고갈 — fork bomb | `fork 폭탄은 PID 제한에 막힌다` |
 * | 자원 고갈 — 무한 출력 | `출력 폭주는 한도에서 끊긴다` |
 * | 자원 고갈 — 메모리 | `메모리 폭식은 컨테이너 상한에 막힌다` |
 * | 비밀 유출 — 파일시스템 탐색 | `호스트 파일시스템을 읽지 못한다` |
 * | 비밀 유출 — 쓰기 | `읽기 전용 파일시스템에 쓰지 못한다` |
 * | 샌드박스 탈출 — 권한 | `root 가 아닌 사용자로 실행된다` |
 * | 네트워크 | `외부로 연결하지 못한다` |
 *
 * 컨테이너 런타임이 없으면 통째로 건너뛴다. **건너뛴 것을 통과로 읽으면 안 된다** —
 * 이 스위트가 돌지 않은 빌드는 격리를 검증하지 않은 빌드다 (§14.4 Security 게이트).
 */
class SandboxRegressionTest {

    private val pkg: ProblemPackage =
        ProblemPackageLoader(Path.of("../../content/problems")).load("two-sum")

    private val adapters = listOf(KotlinAdapter(), JavaAdapter(), PythonAdapter())
        .associateBy { it.language }

    /**
     * 로컬에 이미 있는 첫 이미지를 쓴다.
     *
     * 목록의 맨 앞이 운영에서 쓸 이미지다. 개발 머신에 그것이 없을 때 검증을 통째로
     * 건너뛰는 대신, 같은 런타임을 담은 다른 이미지로라도 격리를 확인한다.
     */
    private fun pick(vararg candidates: String): String? =
        candidates.firstOrNull { ContainerSandbox(it).available() }

    private val jvmImage = pick("eclipse-temurin:21-jre", "gradle:8.10.2-jdk21")
    private val pythonImage = pick("python:3.12-alpine", "python:3.12-slim")

    private val engine by lazy {
        val images = mapOf(
            Language.KOTLIN to jvmImage.orEmpty(),
            Language.JAVA to jvmImage.orEmpty(),
            Language.PYTHON to pythonImage.orEmpty(),
        )
        ExecutionEngine(adapters = adapters, sandboxes = { ContainerSandbox(images.getValue(it)) })
    }

    private fun requireContainers() {
        assumeTrue(
            jvmImage != null && pythonImage != null,
            "런타임 이미지가 로컬에 없어 격리를 검증할 수 없다 (JVM=$jvmImage, Python=$pythonImage)",
        )
    }

    @Test
    fun `컨테이너에서도 세 언어의 정답이 그대로 통과한다`() {
        requireContainers()

        for (language in GoldenSources.languages) {
            val result = engine.execute(request(language, GoldenSources.ACCEPTED.getValue(language)))

            assertTrue(
                result.cases.isNotEmpty() && result.cases.all { it.verdict == Verdict.ACCEPTED },
                "$language: ${result.terminalVerdict} ${result.compileLog} ${result.cases}",
            )
        }
    }

    @Test
    fun `외부로 연결하지 못한다`() {
        requireContainers()

        // 네트워크가 살아 있으면 연결에 성공해 오답이 된다. 막혀 있어야 예외가 난다.
        val source = """
            import java.net.Socket

            fun twoSum(nums: IntArray, target: Int): IntArray {
                Socket("1.1.1.1", 53).use { }
                return intArrayOf(0, 1)
            }
        """.trimIndent()

        val verdict = engine.execute(request(Language.KOTLIN, source)).cases.first().verdict

        assertEquals(Verdict.RUNTIME_ERROR, verdict, "네트워크가 열려 있으면 안 된다")
    }

    @Test
    fun `호스트 파일시스템을 읽지 못한다`() {
        requireContainers()

        // 마운트하지 않은 경로는 컨테이너 안에 존재하지 않는다.
        val source = """
            import java.io.File

            fun twoSum(nums: IntArray, target: Int): IntArray {
                val secrets = File("/Users").listFiles()
                if (secrets != null && secrets.isNotEmpty()) return intArrayOf(0, 1)
                throw IllegalStateException("호스트 파일시스템에 접근할 수 없다")
            }
        """.trimIndent()

        val verdict = engine.execute(request(Language.KOTLIN, source)).cases.first().verdict

        assertEquals(Verdict.RUNTIME_ERROR, verdict, "호스트 경로가 보이면 안 된다")
    }

    @Test
    fun `읽기 전용 파일시스템에 쓰지 못한다`() {
        requireContainers()

        val source = """
            import java.io.File

            fun twoSum(nums: IntArray, target: Int): IntArray {
                File("/etc/codedrill-probe").writeText("x")
                return intArrayOf(0, 1)
            }
        """.trimIndent()

        val verdict = engine.execute(request(Language.KOTLIN, source)).cases.first().verdict

        assertEquals(Verdict.RUNTIME_ERROR, verdict, "rootfs 가 읽기 전용이 아니다")
    }

    @Test
    fun `root 가 아닌 사용자로 실행된다`() {
        requireContainers()

        // uid 가 0 이면 오답, 아니면 정답이 되도록 만든다. 판정 하나로 권한을 읽는다.
        val source = """
            def twoSum(nums, target):
                import os
                return [0, 0] if os.getuid() == 0 else [0, 1]
        """.trimIndent()

        val verdict = engine.execute(request(Language.PYTHON, source)).cases.first().verdict

        assertEquals(Verdict.ACCEPTED, verdict, "컨테이너가 root 로 실행되고 있다 (§5.2 비root UID)")
    }

    @Test
    fun `fork 폭탄은 PID 제한에 막힌다`() {
        requireContainers()

        val source = """
            def twoSum(nums, target):
                import os
                for _ in range(10000):
                    try:
                        os.fork()
                    except OSError:
                        return [0, 1]
                return [9, 9]
        """.trimIndent()

        val result = engine.execute(
            request(Language.PYTHON, source, Limits(timeMillis = 15_000, memoryMb = 256, outputBytes = 65_536)),
        )

        // PID 제한에 걸려 OSError 가 나면 [0, 1] 을 반환해 정답이 된다. 제한이 없으면
        // 프로세스가 폭증해 타임아웃이나 시스템 오류로 끝난다.
        assertEquals(
            Verdict.ACCEPTED,
            result.cases.first().verdict,
            "fork 가 제한되지 않았다: ${result.cases.first()}",
        )
    }

    @Test
    fun `메모리 폭식은 컨테이너 상한에 막힌다`() {
        requireContainers()

        for (language in GoldenSources.languages) {
            val result = engine.execute(
                request(
                    language,
                    GoldenSources.MEMORY_LIMIT.getValue(language),
                    Limits(timeMillis = 20_000, memoryMb = 64, outputBytes = 65_536),
                ),
            )

            assertEquals(
                Verdict.MEMORY_LIMIT,
                result.cases.first().verdict,
                "$language: ${result.cases.first()}",
            )
        }
    }

    @Test
    fun `출력 폭주는 한도에서 끊긴다`() {
        requireContainers()

        val result = engine.execute(
            request(
                Language.PYTHON,
                GoldenSources.OUTPUT_LIMIT.getValue(Language.PYTHON),
                Limits(timeMillis = 20_000, memoryMb = 256, outputBytes = 1_024),
            ),
        )

        assertEquals(Verdict.OUTPUT_LIMIT, result.cases.first().verdict)
    }

    private fun request(
        language: Language,
        source: String,
        limits: Limits = pkg.manifest.limits,
    ) = ExecutionRequest(
        executionId = "exec-sandbox",
        submissionId = "sub-sandbox",
        attempt = 1,
        fencingToken = FencingToken(1),
        correlationId = "corr-sandbox",
        problemVersionId = pkg.problemVersionId,
        packageDigest = pkg.packageDigest,
        language = language,
        source = source,
        signature = pkg.manifest.signature,
        limits = limits,
        // 공격 코퍼스는 첫 케이스에서 결론이 난다. 그룹 하나로 충분하다.
        groups = listOf(RequestedGroup(pkg.groups.first().policy, pkg.groups.first().cases.take(1))),
    )


}
