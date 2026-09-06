package dev.codedrill.judge.runner.execution.sandbox

import dev.codedrill.judge.runner.execution.CaseOutcome
import java.nio.file.Path

/**
 * 신뢰할 수 없는 코드를 격리해 실행하는 경계 (기술 설계서 §5).
 *
 * 구현이 둘이다.
 * - [ProcessSandbox]: 별도 프로세스까지만. 개발 편의용이며 §5.2 의 통제를 거의 갖추지
 *   못한다.
 * - [ContainerSandbox]: §5.2 의 통제를 컨테이너 런타임에 위임한다. 운영 기본값이다.
 *
 * 어느 구현이든 기대 출력은 받지 않는다. 정답 비교가 샌드박스 안에서 일어나면 숨은
 * 테스트가 사용자 프로세스로 새기 때문이다 (§8.3).
 */
interface Sandbox {

    /** 이 샌드박스가 지금 이 머신에서 쓸 수 있는지. */
    fun available(): Boolean

    /**
     * 첫 케이스를 기다릴 때 더 주는 시간.
     *
     * 런타임을 띄우는 데 걸리는 시간은 사용자 코드의 실행 시간이 아니다. 이 유예가
     * 모자라면 멀쩡한 풀이가 TIME_LIMIT 을 받는다 — 그것도 채점 서버가 바쁠 때만
     * 재현되는, 가장 나쁜 종류의 오판이다.
     *
     * 유예를 늘리는 것은 대증요법이다. 근본 해법은 §0.3 이 말하는 **상시 워커 풀**이며,
     * 실행마다 컨테이너를 새로 만드는 지금 구조는 그 지연을 피할 수 없다.
     */
    fun startupGraceMillis(): Long

    fun run(
        spec: SandboxSpec,
        caseIds: List<String>,
        onEvent: (String, String) -> Unit = { _, _ -> },
        shouldContinue: (String, CaseOutcome) -> Boolean = { _, _ -> true },
    ): SandboxRun
}

/**
 * 실행 한 판의 명세.
 *
 * [readOnlyPaths] 는 샌드박스 안으로 들여보낼 경로다. 컨테이너 구현은 이 목록만
 * 마운트하므로, 여기에 없는 것은 실행 중인 코드가 볼 수 없다.
 */
data class SandboxSpec(
    val command: List<String>,
    val workDir: Path,
    val readOnlyPaths: List<Path>,
    val env: Map<String, String>,
    val memoryMb: Int,
    val perCaseTimeoutMillis: Long,
    val outputByteLimit: Long,
)

/**
 * 실행 결과.
 *
 * [fatal] 은 케이스를 하나도 시작하지 못한 경우의 사유다. 컴파일 단계가 잡지 못한
 * 같은 종류의 실패이므로 호출부가 COMPILE_ERROR 로 분류한다.
 */
data class SandboxRun(
    val outcomes: Map<String, CaseOutcome>,
    val fatal: String? = null,
)
