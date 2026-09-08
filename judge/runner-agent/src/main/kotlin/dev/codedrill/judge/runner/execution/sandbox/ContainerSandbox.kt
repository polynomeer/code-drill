package dev.codedrill.judge.runner.execution.sandbox

import dev.codedrill.judge.runner.execution.CaseOutcome
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.UUID
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
 * | 언어별 seccomp allowlist | `--security-opt seccomp=<프로파일>` |
 * | 실행이 끝나면 흔적 제거 | `--rm` 과 [forceRemove] |
 *
 * **메모리는 두 겹이다.** 언어 런타임의 상한(JVM `-Xmx`, Python `RLIMIT_AS`)이 먼저
 * 걸리고, 컨테이너 상한은 그것을 빠져나간 경우의 backstop 이다. 그래서 컨테이너 몫은
 * 사용자 한도보다 [MEMORY_HEADROOM_MB] 만큼 크다. 순서가 반대면 모든 초과가 exit 137
 * 로만 보여 어디서 샌 메모리인지 알 수 없게 된다.
 *
 * **컨테이너 수명은 두 겹이다.** `--rm` 은 컨테이너가 *스스로 끝났을 때*만 동작하므로,
 * 무한 루프처럼 끝나지 않는 풀이에는 아무 효과가 없다. 그리고 데드라인에 걸려 죽는 것은
 * `docker run` **클라이언트 프로세스**일 뿐, 컨테이너는 데몬 아래 그대로 남아 계속 돈다.
 * 그래서 [run] 은 컨테이너에 이름을 붙이고 끝날 때 [forceRemove] 로 직접 지운다.
 * Runner 자체가 죽어 그 정리마저 못 도는 경우는 [reapOrphans] 가 받는다.
 */
class ContainerSandbox(
    private val image: String,
    private val runtimeBinary: String = DEFAULT_RUNTIME,
    private val cpus: String = DEFAULT_CPUS,
    private val pidsLimit: Int = DEFAULT_PIDS_LIMIT,
    /**
     * 언어별 seccomp allowlist (§5.2 시스템 호출).
     *
     * null 이면 런타임 기본 프로파일을 쓴다. 기본 프로파일도 위험한 호출을 상당수
     * 막지만 allowlist 가 아니므로, 공개 환경에서는 반드시 지정해야 한다 —
     * [SandboxSelector] 가 그것을 강제한다.
     */
    private val seccompProfile: Path? = null,
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

    /**
     * 컨테이너 생성 + 런타임 기동을 함께 기다린다.
     *
     * 데몬이 바쁠 때 `docker run` 이 수 초씩 걸린다. 유예가 짧으면 채점 서버가 바쁠 때만
     * 멀쩡한 풀이가 시간 초과로 떨어진다.
     */
    override fun startupGraceMillis() = 20_000L

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
        // 이름을 미리 정해 둬야 컨테이너를 지울 수 있다. `docker run` 의 출력에서 id 를
        // 읽는 방법은 쓸 수 없다 — 그 출력은 사용자 코드의 프로토콜 스트림이고, 애초에
        // 컨테이너가 매달린 채 아무것도 내보내지 않는 경우가 지워야 하는 바로 그 경우다.
        val containerName = "$CONTAINER_PREFIX${UUID.randomUUID()}"
        val process = ProcessBuilder(buildCommand(spec, containerName)).start()

        return SandboxStream.consume(
            process = process,
            caseIds = caseIds,
            perCaseTimeoutMillis = spec.perCaseTimeoutMillis,
            startupGraceMillis = startupGraceMillis(),
            outputByteLimit = spec.outputByteLimit,
            onEvent = onEvent,
            shouldContinue = shouldContinue,
            onTeardown = { forceRemove(containerName) },
        )
    }

    /**
     * 컨테이너를 확실히 없앤다. 실행 경로가 어떻게 끝났든 마지막에 반드시 한 번 돈다.
     *
     * 정상 종료한 실행에서는 `--rm` 이 이미 지운 뒤라 "No such container" 로 실패하는데,
     * 그것이 정상이므로 종료 코드를 보지 않는다. 여기서 확인할 것은 오직 **호출이 걸려
     * 있지 않은가**뿐이다 — 데몬이 응답하지 않을 때 이 정리가 채점 스레드를 붙잡으면
     * 컨테이너 하나가 새는 대신 Runner 전체가 멈춘다.
     */
    private fun forceRemove(containerName: String) {
        runCatching {
            val process = ProcessBuilder(runtimeBinary, "rm", "--force", containerName)
                .redirectErrorStream(true)
                .start()
            if (!process.waitFor(REMOVE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                log.warn("샌드박스 컨테이너 제거가 시간 안에 끝나지 않았다: {}", containerName)
            }
        }.onFailure { error ->
            log.warn("샌드박스 컨테이너를 제거하지 못했다: {} ({})", containerName, error.message)
        }
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

    private fun buildCommand(spec: SandboxSpec, containerName: String): List<String> {
        val containerMemory = spec.memoryMb + MEMORY_HEADROOM_MB
        val command = mutableListOf(
            runtimeBinary, "run", "--rm",
            // 스스로 끝난 실행은 여기서 정리된다. 끝나지 않는 실행은 forceRemove 가 맡는다.
            "--name", containerName,
            // 이름을 잃어버려도 라벨로는 찾을 수 있다. reapOrphans 가 이것으로 훑는다.
            "--label", "$OWNER_LABEL=$OWNER_LABEL_VALUE",
            "--label", "$STARTED_AT_LABEL=${System.currentTimeMillis()}",
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

        // 프로파일이 없으면 옵션을 붙이지 않는다. 잘못된 경로를 넘기면 컨테이너 런타임이
        // 실행을 거부하는데, 그 실패는 사용자 코드 탓처럼 보인다.
        seccompProfile?.let { command += listOf("--security-opt", "seccomp=${it.absolutePathString()}") }

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

        /** 컨테이너 이름 접두사. 호스트에서 사람이 봤을 때 출처가 드러나야 한다. */
        const val CONTAINER_PREFIX = "codedrill-sandbox-"

        const val OWNER_LABEL = "dev.codedrill.sandbox"
        const val OWNER_LABEL_VALUE = "runner-agent"
        const val STARTED_AT_LABEL = "dev.codedrill.sandbox.started-at"

        /**
         * 고아 컨테이너로 판정하는 나이.
         *
         * 케이스 하나의 제한이 수 초, 한 판이 길어야 수 분이다. 한 시간을 넘겨 살아 있는
         * 샌드박스는 정의상 아무도 기다리지 않는 것이다. 넉넉히 잡는 이유는 이 판단이
         * 틀리면 채점 중인 실행을 죽여 멀쩡한 제출이 오판을 받기 때문이다.
         */
        const val ORPHAN_MAX_AGE_MILLIS = 3_600_000L

        const val REMOVE_TIMEOUT_SECONDS = 15L

        private val reaperLog = LoggerFactory.getLogger(ContainerSandbox::class.java)

        /**
         * Runner 가 죽으면서 두고 간 샌드박스 컨테이너를 치운다 (기동 시 한 번).
         *
         * [forceRemove] 는 채점 스레드가 살아 있을 때만 돈다. Runner 가 SIGKILL 을 받거나
         * 호스트가 재부팅되면 그 정리는 아예 실행되지 않고, 남은 컨테이너는 CPU 를 계속
         * 태운다 — 끝나지 않는 풀이라면 무한히. 그래서 정리 경로가 둘이어야 한다.
         *
         * 나이로만 거른다. 지금 이 Runner 가 채점 중인 컨테이너를 죽이면 멀쩡한 제출이
         * 오판을 받으므로, 판단 근거는 "누가 띄웠나"가 아니라 "아무도 기다릴 수 없을 만큼
         * 오래됐나"여야 한다 ([ORPHAN_MAX_AGE_MILLIS]).
         *
         * @return 제거한 컨테이너 수.
         */
        fun reapOrphans(
            runtimeBinary: String = DEFAULT_RUNTIME,
            maxAgeMillis: Long = ORPHAN_MAX_AGE_MILLIS,
            now: Long = System.currentTimeMillis(),
        ): Int = runCatching {
            val listing = ProcessBuilder(
                runtimeBinary, "ps", "--all", "--no-trunc",
                "--filter", "label=$OWNER_LABEL=$OWNER_LABEL_VALUE",
                "--format", "{{.ID}}\t{{.Label \"$STARTED_AT_LABEL\"}}",
            ).start()
            val rows = listing.inputStream.bufferedReader().readText().trim()
            if (!listing.waitFor(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                listing.destroyForcibly()
                return@runCatching 0
            }
            if (rows.isEmpty()) return@runCatching 0

            val stale = rows.lineSequence().mapNotNull { row ->
                val (id, startedAt) = row.split('\t').let { it.getOrNull(0) to it.getOrNull(1) }
                // 라벨을 읽지 못한 컨테이너는 건드리지 않는다. 나이를 모르면 채점 중인지도
                // 모르고, 확신 없이 죽이는 쪽이 두고 보는 쪽보다 나쁘다.
                val age = startedAt?.toLongOrNull()?.let { now - it } ?: return@mapNotNull null
                id?.takeIf { it.isNotBlank() && age > maxAgeMillis }
            }.toList()

            for (id in stale) {
                ProcessBuilder(runtimeBinary, "rm", "--force", id)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor(REMOVE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            }
            if (stale.isNotEmpty()) {
                reaperLog.warn(
                    "이전 Runner 가 두고 간 샌드박스 컨테이너 {}개를 제거했다. " +
                        "Runner 가 비정상 종료했다는 뜻이다",
                    stale.size,
                )
            }
            stale.size
        }.getOrElse { error ->
            reaperLog.warn("고아 컨테이너를 훑지 못했다: {}", error.message)
            0
        }

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
