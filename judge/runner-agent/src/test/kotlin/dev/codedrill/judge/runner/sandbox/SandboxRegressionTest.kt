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
import dev.codedrill.judge.runner.execution.sandbox.SeccompProfile
import dev.codedrill.judge.runner.golden.GoldenSources
import dev.codedrill.platform.problempackage.Limits
import dev.codedrill.platform.problempackage.ProblemPackage
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.TimeUnit
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
 * | 시스템 호출 — allowlist 밖 | `허용 목록에 없는 시스템 호출은 막힌다` |
 * | 샌드박스 탈출 — 네임스페이스·ptrace | `새 사용자 네임스페이스를 만들지 못한다` |
 * | 자원 고갈 — 컨테이너 누수 | `무한 루프는 컨테이너를 남기지 않는다` |
 * | 자원 고갈 — Runner 사망 | `죽은 Runner 가 두고 간 컨테이너를 치운다` |
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

    /**
     * 실행에 쓰는 seccomp 프로파일 (§5.2 시스템 호출).
     *
     * 운영과 같은 것을 건다. 회귀 스위트가 프로파일 없이 돌면 격리를 검증한 것이
     * 아니라 **프로파일 없는 격리**를 검증한 것이 된다.
     */
    private val profiles = SeccompProfile.materialize(Path.of("build/seccomp"))

    private val engine by lazy {
        val images = mapOf(
            Language.KOTLIN to jvmImage.orEmpty(),
            Language.JAVA to jvmImage.orEmpty(),
            Language.PYTHON to pythonImage.orEmpty(),
        )
        ExecutionEngine(
            adapters = adapters,
            sandboxes = { ContainerSandbox(images.getValue(it), seccompProfile = profiles[it]) },
        )
    }

    /**
     * 런타임 이미지가 없으면 이 스위트는 아무것도 검증하지 못한다.
     *
     * 개발 머신에서는 건너뛴다 — 이미지를 받아 두지 않았다고 빌드를 세울 이유가 없다.
     * **CI 에서는 실패한다.** 건너뛴 검사는 초록으로 보이고, 초록으로 보이는 빈 검사는
     * 검사가 없는 것보다 나쁘다. 없으면 없는 줄이라도 안다.
     *
     * 실행 영역이 `REQUIRE_ISOLATION` 으로 기동을 막는 것과 같은 자리에 있는 스위치다.
     */
    private fun requireContainers() {
        val ready = jvmImage != null && pythonImage != null
        val why = "런타임 이미지가 로컬에 없어 격리를 검증할 수 없다 (JVM=$jvmImage, Python=$pythonImage)"

        if (System.getenv(REQUIRE) == "true") assertTrue(ready, "$REQUIRE=true 인데 $why")
        assumeTrue(ready, why)
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
    fun `허용 목록에 없는 시스템 호출은 막힌다`() {
        requireContainers()

        // 소켓 **생성**은 `--network none` 아래서도 성공한다. 연결할 곳이 없을 뿐이다.
        // 그래서 이 호출이 실패한다는 것은 seccomp 가 실제로 걸려 있다는 뜻이며,
        // 다른 통제로는 이 결과를 만들 수 없다.
        val source = """
            def twoSum(nums, target):
                import socket
                socket.socket()
                return [0, 1]
        """.trimIndent()

        val result = engine.execute(request(Language.PYTHON, source))

        assertEquals(
            Verdict.RUNTIME_ERROR, result.cases.first().verdict,
            "seccomp 프로파일이 걸려 있지 않다 — 소켓 생성이 성공했다",
        )
    }

    @Test
    fun `새 사용자 네임스페이스를 만들지 못한다`() {
        requireContainers()

        // 프로파일이 없으면 이 호출은 **성공한다**. 새 user namespace 안에서는 자기가
        // root 가 되고, 거기서부터 커널 표면을 넓게 두드릴 수 있다 (§11.1 샌드박스 탈출).
        // ptrace 도 같은 이유로 막는다 — 같은 컨테이너의 다른 프로세스를 들여다볼 수 있다.
        val source = """
            def twoSum(nums, target):
                import ctypes
                libc = ctypes.CDLL(None, use_errno=True)
                if libc.unshare(0x10000000) == 0:
                    raise AssertionError("user namespace 를 만들 수 있다")
                if libc.ptrace(0, 0, 0, 0) == 0:
                    raise AssertionError("ptrace 를 쓸 수 있다")
                seen = {}
                for i, value in enumerate(nums):
                    j = seen.get(target - value)
                    if j is not None:
                        return [j, i]
                    seen.setdefault(value, i)
                raise AssertionError("정답은 항상 존재한다")
        """.trimIndent()

        val result = engine.execute(request(Language.PYTHON, source))

        assertTrue(
            result.cases.all { it.verdict == Verdict.ACCEPTED },
            "탈출 경로가 열려 있다: ${result.cases.map { it.verdict to it.message }}",
        )
    }

    @Test
    fun `허용 목록 안의 시스템 호출은 그대로 통과한다`() {
        requireContainers()

        // 목록을 좁히다 보면 멀쩡한 코드를 막기 쉽다. 파일·스레드·시간은 풀이가 흔히
        // 쓰는 것들이라, 이것이 막히면 사용자에게는 원인 모를 실패로 보인다.
        val source = """
            import threading
            import time
            import tempfile

            def twoSum(nums, target):
                done = []
                worker = threading.Thread(target=lambda: done.append(time.monotonic()))
                worker.start()
                worker.join()
                with tempfile.TemporaryFile() as handle:
                    handle.write(b"ok")
                seen = {}
                for i, value in enumerate(nums):
                    j = seen.get(target - value)
                    if j is not None:
                        return [j, i]
                    seen.setdefault(value, i)
                raise AssertionError("정답은 항상 존재한다")
        """.trimIndent()

        val result = engine.execute(request(Language.PYTHON, source))

        assertTrue(
            result.cases.all { it.verdict == Verdict.ACCEPTED },
            "허용 목록이 너무 좁다: ${result.cases.map { it.verdict to it.message }}",
        )
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

    /**
     * 데드라인에 걸린 실행이 컨테이너를 남기지 않는지 (§11.1 자원 고갈).
     *
     * 이것이 깨졌을 때 증상은 오판이 아니라 **호스트가 조용히 죽는 것**이다. 판정은
     * TIME_LIMIT 으로 정확히 나오고 사용자에게는 아무 문제가 없어 보이는데, 무한 루프는
     * 컨테이너 안에서 계속 돌면서 CPU 한 코어씩을 영구히 가져간다. 실제로 이 누수로
     * 개발 머신에 컨테이너 64개가 24시간 동안 살아 있었다.
     *
     * `--rm` 은 이것을 막지 못한다. 컨테이너가 스스로 끝났을 때만 도는 옵션이고, 여기서
     * 문제가 되는 실행은 정의상 스스로 끝나지 않는다.
     */
    @Test
    fun `무한 루프는 컨테이너를 남기지 않는다`() {
        requireContainers()

        val before = sandboxContainerIds()

        val result = engine.execute(
            request(
                Language.KOTLIN,
                GoldenSources.TIME_LIMIT.getValue(Language.KOTLIN),
                Limits(timeMillis = 2_000, memoryMb = 256, outputBytes = 65_536),
            ),
        )

        assertEquals(Verdict.TIME_LIMIT, result.cases.first().verdict)

        // 판정이 맞는 것으로는 부족하다. 누수는 판정이 정확할 때도 일어난다.
        val leaked = sandboxContainerIds() - before
        assertTrue(
            leaked.isEmpty(),
            "데드라인 뒤에도 샌드박스 컨테이너가 남았다: $leaked",
        )
    }

    /**
     * Runner 가 죽어 teardown 이 돌지 못한 경우 (§11.1 자원 고갈).
     *
     * 나이로만 거르므로, 이 판단이 헐거우면 **채점 중인 실행을 죽여** 멀쩡한 제출이
     * 오판을 받는다. 그래서 오래된 것이 지워지는지와 함께 최근 것이 살아남는지를 본다 —
     * 뒤쪽이 없으면 리퍼가 무차별로 도는 것을 잡지 못한다.
     */
    @Test
    fun `죽은 Runner 가 두고 간 컨테이너를 치운다`() {
        requireContainers()

        val image = jvmImage!!
        val orphan = "${ContainerSandbox.CONTAINER_PREFIX}reap-orphan-${UUID.randomUUID()}"
        val live = "${ContainerSandbox.CONTAINER_PREFIX}reap-live-${UUID.randomUUID()}"

        try {
            startDetached(image, orphan, System.currentTimeMillis() - 7_200_000)
            startDetached(image, live, System.currentTimeMillis())

            ContainerSandbox.reapOrphans()

            val remaining = sandboxContainerNames()
            assertTrue(orphan !in remaining, "한 시간 넘게 남은 고아를 치우지 못했다")
            assertTrue(live in remaining, "이제 막 뜬 컨테이너를 죽였다 — 채점 중인 실행이었다면 오판이다")
        } finally {
            forceRemove(orphan)
            forceRemove(live)
        }
    }

    /** 리퍼가 보게 될 것과 같은 라벨을 달아 컨테이너를 띄운다. */
    private fun startDetached(image: String, name: String, startedAt: Long) {
        val process = ProcessBuilder(
            "docker", "run", "--detach", "--name", name,
            "--label", "${ContainerSandbox.OWNER_LABEL}=${ContainerSandbox.OWNER_LABEL_VALUE}",
            "--label", "${ContainerSandbox.STARTED_AT_LABEL}=$startedAt",
            "--network", "none", "--entrypoint", "sleep", image, "300",
        ).redirectErrorStream(true).start()
        process.inputStream.bufferedReader().readText()
        process.waitFor(60, TimeUnit.SECONDS)
    }

    private fun forceRemove(name: String) {
        ProcessBuilder("docker", "rm", "--force", name)
            .redirectErrorStream(true)
            .start()
            .waitFor(60, TimeUnit.SECONDS)
    }

    private fun sandboxContainerNames(): Set<String> = dockerPs("{{.Names}}")

    /** 지금 이 호스트에 남아 있는 샌드박스 컨테이너. */
    private fun sandboxContainerIds(): Set<String> = dockerPs("{{.ID}}")

    private fun dockerPs(format: String): Set<String> {
        val process = ProcessBuilder(
            "docker", "ps", "--all", "--no-trunc",
            "--filter", "name=${ContainerSandbox.CONTAINER_PREFIX}",
            "--format", format,
        ).start()
        val rows = process.inputStream.bufferedReader().readText()
        process.waitFor(30, TimeUnit.SECONDS)
        return rows.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
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

    private companion object {
        /** CI 에서 건너뛰기를 실패로 바꾸는 스위치. */
        const val REQUIRE = "CODEDRILL_REQUIRE_SANDBOX"
    }
}
