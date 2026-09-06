package dev.codedrill.judge.runner.execution.sandbox

import dev.codedrill.judge.runner.execution.CaseOutcome

/**
 * 별도 프로세스로만 격리하는 샌드박스. **개발 편의용이다.**
 *
 * 강제하는 것은 프로세스 분리, 런타임이 지원하는 메모리 상한, 벽시계 데드라인, 출력
 * 한도뿐이다. §5.2 가 요구하는 다음 통제가 전부 빠져 있다.
 *
 * | 통제 | 상태 |
 * |---|---|
 * | 비root UID, capability 제거 | 없음 — Runner 와 같은 사용자로 돈다 |
 * | read-only rootfs | 없음 — 호스트 파일시스템에 쓸 수 있다 |
 * | 네트워크 격리 | 없음 — 외부로 연결할 수 있다 |
 * | seccomp allowlist | 없음 |
 * | PID 제한 | 없음 — fork 폭탄을 막지 못한다 |
 *
 * 그래서 이 구현은 신뢰할 수 없는 코드를 받는 환경에서 절대 쓰면 안 된다.
 * [SandboxSelector] 가 운영 기본값을 [ContainerSandbox] 로 두고, 이 구현은 컨테이너
 * 런타임이 없을 때만 — 그것도 경고와 함께 — 선택되게 한다.
 */
class ProcessSandbox : Sandbox {

    override fun available() = true

    /** JVM 이나 인터프리터 기동 시간만 감안한다. */
    override fun startupGraceMillis() = 5_000L

    override fun run(
        spec: SandboxSpec,
        caseIds: List<String>,
        onEvent: (String, String) -> Unit,
        shouldContinue: (String, CaseOutcome) -> Boolean,
    ): SandboxRun {
        val process = ProcessBuilder(spec.command)
            .directory(spec.workDir.toFile())
            .apply { environment().putAll(spec.env) }
            .start()

        return SandboxStream.consume(
            process = process,
            caseIds = caseIds,
            perCaseTimeoutMillis = spec.perCaseTimeoutMillis,
            startupGraceMillis = startupGraceMillis(),
            outputByteLimit = spec.outputByteLimit,
            onEvent = onEvent,
            shouldContinue = shouldContinue,
        )
    }
}
