package dev.codedrill.judge.runner.execution.sandbox

import dev.codedrill.judge.protocol.Language
import org.slf4j.LoggerFactory

/**
 * 언어마다 어느 샌드박스로 실행할지 고른다 (기술 설계서 §5.5 런타임 이미지 릴리스).
 *
 * 운영 기본값은 [ContainerSandbox] 다. 컨테이너 런타임이 없으면 [ProcessSandbox] 로
 * 내려가되 **경고를 남긴다** — 조용히 내려가면 격리 없이 신뢰할 수 없는 코드를 돌리는
 * 상태가 눈에 띄지 않는다.
 *
 * [requireIsolation] 이 켜져 있으면 내려가지 않고 실패한다. 공개 환경 배포에서는 반드시
 * 켜야 한다.
 */
class SandboxSelector(
    private val images: Map<Language, String>,
    private val requireIsolation: Boolean,
    /** 컨테이너 없이 메모리 상한을 강제할 수 없는 언어. 경고에 쓴다. */
    private val needsContainerForMemory: Set<Language> = emptySet(),
    private val fallback: Sandbox = ProcessSandbox(),
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val containers = images.mapValues { (_, image) -> ContainerSandbox(image) }

    /** 기동 시 한 번 호출해 실제 격리 상태를 로그로 남긴다. */
    fun report() {
        for ((language, sandbox) in containers) {
            if (sandbox.available()) {
                log.info(
                    "격리 실행: {} → 컨테이너 {} (digest {})",
                    language,
                    images[language],
                    sandbox.imageDigest() ?: "확인 불가",
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
