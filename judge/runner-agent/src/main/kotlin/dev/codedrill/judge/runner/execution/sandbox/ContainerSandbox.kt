package dev.codedrill.judge.runner.execution.sandbox

import dev.codedrill.judge.runner.execution.CaseOutcome
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString

/**
 * 컨테이너 런타임에 격리를 위임하는 샌드박스 (기술 설계서 §5.2).
 *
 * §5.2 의 통제를 컨테이너 옵션으로 하나씩 대응시킨다.
 *
 * | §5.2 통제 | 옵션 |
 * |---|---|
 * | 비root UID/GID, capability 전체 제거 | `--user 65534:65534 --cap-drop ALL` |
 * | 권한 상승 차단 | `--security-opt no-new-privileges` |
 * | read-only rootfs, 작업 tmpfs, 크기 quota | `--read-only --tmpfs /tmp:...,size=...` |
 * | network namespace 분리, egress/ingress 없음 | `--network none` |
 * | PID 제한, fork 폭탄 방어 | `--pids-limit` |
 * | CPU quota | `--cpus` |
 * | memory.max + swap 비활성 | `--memory` 와 같은 값의 `--memory-swap` |
 *
 * **메모리는 두 겹이다.** 언어 런타임의 상한(JVM `-Xmx`, Python `RLIMIT_AS`)이 먼저
 * 걸리고, 컨테이너 상한은 그것을 빠져나간 경우의 backstop 이다. 그래서 컨테이너 몫은
 * 사용자 한도보다 [MEMORY_HEADROOM_MB] 만큼 크다. 순서가 반대면 모든 초과가 exit 137
 * 로만 보여 어디서 샌 메모리인지 알 수 없게 된다.
 *
 * 남은 공백: seccomp allowlist 는 아직 언어별 프로파일이 없어 런타임 기본값을 쓴다.
 * §11.4 의 Sandbox regression 게이트는 이 프로파일이 생긴 뒤에야 완전해진다.
 */
class ContainerSandbox(
    private val image: String,
    private val runtimeBinary: String = DEFAULT_RUNTIME,
    private val cpus: String = DEFAULT_CPUS,
    private val pidsLimit: Int = DEFAULT_PIDS_LIMIT,
) : Sandbox {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 런타임이 살아 있고 **이미지가 이미 로컬에 있는지**.
     *
     * 이미지 존재까지 확인하는 이유가 있다. 없는 이미지로 실행하면 컨테이너 런타임이
     * 그 자리에서 pull 을 시작하고, 그 수십 초가 사용자 코드의 TIME_LIMIT 으로 둔갑한다.
     * 이미지는 채점 전에 승격되어 있어야 한다 (§5.5 canary 승격).
     */
    override fun available(): Boolean = daemonReachable() && imagePresent()

    private fun daemonReachable(): Boolean = runCatching {
        val probe = ProcessBuilder(runtimeBinary, "version", "--format", "{{.Server.Version}}")
            .redirectErrorStream(true)
            .start()
        probe.waitFor(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS) && probe.exitValue() == 0
    }.getOrDefault(false)

    private fun imagePresent(): Boolean = runCatching {
        val probe = ProcessBuilder(runtimeBinary, "image", "inspect", image)
            .redirectErrorStream(true)
            .start()
        val present = probe.waitFor(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS) && probe.exitValue() == 0
        if (!present) log.warn("런타임 이미지가 로컬에 없다: {}. 미리 pull 해야 한다", image)
        present
    }.getOrDefault(false)

    /**
     * 실행에 쓰인 이미지 digest. 판정 근거를 재현할 때 필요하다 (§5.5, §12.4).
     *
     * 태그는 같은 이름으로 내용이 바뀔 수 있으므로, 실제로 무엇을 돌렸는지는 digest 로만
     * 말할 수 있다.
     */
    fun imageDigest(): String? = runCatching {
        val process = ProcessBuilder(
            runtimeBinary, "image", "inspect", image, "--format", "{{index .RepoDigests 0}}",
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        output.takeIf { process.exitValue() == 0 && it.isNotBlank() }
    }.getOrNull()

    override fun run(
        spec: SandboxSpec,
        caseIds: List<String>,
        onEvent: (String, String) -> Unit,
        shouldContinue: (String, CaseOutcome) -> Boolean,
    ): SandboxRun {
        val process = ProcessBuilder(buildCommand(spec)).start()

        return SandboxStream.consume(
            process = process,
            caseIds = caseIds,
            perCaseTimeoutMillis = spec.perCaseTimeoutMillis,
            outputByteLimit = spec.outputByteLimit,
            onEvent = onEvent,
            shouldContinue = shouldContinue,
        )
    }

    /**
     * 컨테이너를 돌릴 비특권 사용자.
     *
     * 호스트 uid 를 그대로 쓴다. 샌드박스 디렉터리는 `700` 으로 만들어지므로 다른 uid 로
     * 들어가면 자기 소스조차 읽지 못한다. 권한을 `755` 로 넓혀 맞추는 방법도 있지만,
     * 그러면 숨은 테스트 데이터가 호스트의 다른 로컬 사용자에게도 열린다.
     *
     * Runner 가 root 로 돌고 있으면 그 값을 쓸 수 없다. §5.2 의 비root 요구가 우선이므로
     * nobody 로 내려가고, 그 경우에는 마운트 권한을 호출부가 맞춰야 한다.
     */
    private fun unprivilegedUser(): String {
        val uid = hostId("-u")
        val gid = hostId("-g")
        return if (uid > 0) "$uid:$gid" else "$UNPRIVILEGED_UID:$UNPRIVILEGED_GID"
    }

    private fun hostId(flag: String): Int = runCatching {
        val process = ProcessBuilder("id", flag).redirectErrorStream(true).start()
        val value = process.inputStream.bufferedReader().readText().trim()
        process.waitFor(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        value.toInt()
    }.getOrDefault(0)

    private fun buildCommand(spec: SandboxSpec): List<String> {
        val containerMemory = spec.memoryMb + MEMORY_HEADROOM_MB
        val command = mutableListOf(
            runtimeBinary, "run", "--rm",
            // 실행이 끝나면 흔적을 남기지 않는다.
            "--network", "none",
            "--read-only",
            // 쓰기가 필요한 곳은 여기 하나뿐이고, 실행 권한도 주지 않는다.
            "--tmpfs", "/tmp:rw,noexec,nosuid,size=${TMPFS_SIZE_MB}m",
            "--user", unprivilegedUser(),
            "--cap-drop", "ALL",
            "--security-opt", "no-new-privileges",
            "--pids-limit", pidsLimit.toString(),
            "--memory", "${containerMemory}m",
            // swap 을 메모리와 같은 값으로 두면 스왑이 비활성화된다.
            "--memory-swap", "${containerMemory}m",
            "--cpus", cpus,
            "--workdir", "/tmp",
            // 실행 시점에 이미지를 내려받지 않는다. 없으면 곧바로 실패하는 편이,
            // 사용자 코드가 느린 것처럼 보이는 것보다 낫다.
            "--pull", "never",
        )

        // 들여보내는 경로는 이 목록이 전부다. 여기 없는 것은 실행 중인 코드가 볼 수 없다.
        val mapping = containerPaths(spec)
        for ((host, inside) in mapping) {
            command += listOf("--volume", "$host:$inside:ro")
        }

        for ((key, value) in spec.env) {
            command += listOf("--env", "$key=$value")
        }

        command += image
        // 어댑터는 호스트 경로로 명령을 만든다. 컨테이너 안의 경로로 바꿔 준다.
        command += spec.command.map { token -> rewrite(token, mapping) }
        log.debug("샌드박스 실행: {}", command.joinToString(" "))
        return command
    }

    /**
     * 호스트 경로 → 컨테이너 경로.
     *
     * 호스트와 같은 경로에 마운트하지 않는다. macOS 의 컨테이너 런타임은 `/private/...`
     * 같은 호스트 실제 경로를 목적지로 주면 마운트를 보여주지 않아, 조용히 빈 디렉터리가
     * 된다. 고정된 컨테이너 경로로 옮기면 호스트 레이아웃과도 분리된다.
     */
    private fun containerPaths(spec: SandboxSpec): Map<String, String> =
        buildMap {
            put(spec.workDir.absolutePathString(), SANDBOX_ROOT)
            spec.readOnlyPaths.forEachIndexed { index, path ->
                put(path.absolutePathString(), "$RUNTIME_ROOT/$index-${path.fileName}")
            }
        }

    /** 긴 경로부터 바꿔야 상위 경로가 하위 경로를 가로채지 않는다. */
    private fun rewrite(token: String, mapping: Map<String, String>): String =
        mapping.entries
            .sortedByDescending { it.key.length }
            .fold(token) { acc, (host, inside) -> acc.replace(host, inside) }

    companion object {
        const val DEFAULT_RUNTIME = "docker"
        const val DEFAULT_CPUS = "1"
        const val DEFAULT_PIDS_LIMIT = 64

        /** nobody:nogroup. 이미지에 관계없이 존재하는 비특권 계정이다. */
        const val UNPRIVILEGED_UID = 65534
        const val UNPRIVILEGED_GID = 65534

        /**
         * 컨테이너 메모리에서 언어 런타임 상한 위에 더 주는 여유.
         *
         * JVM 은 힙 밖에도 메타스페이스·스레드 스택·코드 캐시를 쓴다. 여유가 없으면
         * 사용자 힙을 다 쓰기 전에 컨테이너가 먼저 죽어, 판정 사유가 흐려진다.
         */
        const val MEMORY_HEADROOM_MB = 320

        const val SANDBOX_ROOT = "/sandbox"
        const val RUNTIME_ROOT = "/runtime"
        const val TMPFS_SIZE_MB = 64
        const val PROBE_TIMEOUT_SECONDS = 10L
    }
}
