package dev.codedrill.judge.runner.execution.sandbox

import dev.codedrill.judge.protocol.Language
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * 언어마다 어느 샌드박스로 실행할지 고른다 (기술 설계서 §5.5 런타임 이미지 릴리스).
 *
 * 운영 기본값은 [ContainerSandbox] 다. 컨테이너 런타임이 없으면 [ProcessSandbox] 로
 * 내려가되 **경고를 남긴다** — 조용히 내려가면 격리 없이 신뢰할 수 없는 코드를 돌리는
 * 상태가 눈에 띄지 않는다.
 *
 * [requireIsolation] 이 켜져 있으면 내려가지 않고 실패한다. 공개 환경 배포에서는 반드시
 * 켜야 한다.
 *
 * seccomp 프로파일은 기동할 때 한 번 파일로 내보낸다 (§5.2). 프로파일 없이 컨테이너를
 * 띄우면 시스템 호출 통제만 빠진 채 나머지가 다 서 있는 상태가 되는데, 로그만 보고는
 * 그것을 알아채기 어렵다 — 그래서 어떤 프로파일을 어떤 digest 로 걸었는지 함께 남긴다.
 */
class SandboxSelector(
    private val images: Map<Language, String>,
    private val requireIsolation: Boolean,
    /** 컨테이너 없이 메모리 상한을 강제할 수 없는 언어. 경고에 쓴다. */
    private val needsContainerForMemory: Set<Language> = emptySet(),
    private val fallback: Sandbox = ProcessSandbox(),
    /** seccomp 프로파일을 내보낼 곳. 기동 시 한 번 쓰고 계속 쓴다. */
    profileDirectory: Path = Path.of(System.getProperty("java.io.tmpdir"), "codedrill-seccomp"),
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val profiles: Map<Language, Path> = runCatching {
        SeccompProfile.materialize(profileDirectory)
    }.getOrElse { error ->
        // 프로파일을 못 쓰면 통제 하나가 통째로 빠진다. 조용히 넘어가지 않는다.
        check(!requireIsolation) {
            "seccomp 프로파일을 내보내지 못했다: ${error.message}. " +
                "codedrill.sandbox.require-isolation=true 이므로 기동을 중단한다"
        }
        log.warn("seccomp 프로파일을 내보내지 못했다: {}. 런타임 기본값으로 돌린다", error.message)
        emptyMap()
    }

    private val containers = images.mapValues { (language, image) ->
        ContainerSandbox(image, seccompProfile = profiles[language])
    }

    /**
     * 기동 시 한 번 호출해 실제 격리 상태를 로그로 남긴다.
     *
     * 이전 Runner 가 두고 간 샌드박스 컨테이너도 여기서 치운다. 기동 시점은 이 Runner 가
     * 아직 아무것도 채점하지 않은 유일한 순간이라, 살아 있는 실행을 잘못 죽일 위험이
     * 가장 낮다.
     */
    fun report() {
        if (containers.isNotEmpty()) ContainerSandbox.reapOrphans()

        for ((language, sandbox) in containers) {
            if (sandbox.available()) {
                log.info(
                    "격리 실행: {} → 컨테이너 {} (digest {}), seccomp {} (허용 {}개, profile {})",
                    language,
                    images[language],
                    sandbox.imageDigest() ?: "확인 불가",
                    if (profiles.containsKey(language)) "allowlist" else "런타임 기본값",
                    SeccompProfile.forLanguage(language).size,
                    SeccompProfile.digest(language),
                )
            } else {
                val message = "컨테이너 런타임을 찾지 못했다: $language"
                check(!requireIsolation) {
                    "$message. codedrill.sandbox.require-isolation=true 이므로 기동을 중단한다"
                }
                log.warn("$message. 프로세스 샌드박스로 내려간다 — 신뢰할 수 없는 코드를 받는 환경에서는 쓰면 안 된다")
                if (language in needsContainerForMemory) {
                    log.warn(
                        "{} 는 컨테이너 없이 메모리 상한을 강제하지 못한다. MEMORY_LIMIT 이 " +
                            "TIME_LIMIT 이나 시스템 정지로 나타날 수 있다",
                        language,
                    )
                }
            }
        }
    }

    fun forLanguage(language: Language): Sandbox {
        val container = containers[language]
        if (container != null && container.available()) return container

        check(!requireIsolation) { "격리 실행을 강제했는데 컨테이너 런타임이 없다: $language" }
        return fallback
    }
}
