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

    /**
     * 프로토콜 없이 명령 하나를 돌리고 종료 코드와 출력을 받는다. **컴파일이 여기서 돈다.**
     *
     * 컴파일러는 사용자 코드를 읽는 첫 프로그램이고, 그래서 공격 표면이다 (§5.5).
     * 실행만 격리하고 컴파일을 Runner 프로세스에서 돌리면 격리가 반쪽이다. 같은 이미지,
     * 같은 통제 아래서 돌리되, [SandboxSpec.writablePaths] 에 적은 곳에만 산출물을 쓸 수
     * 있다.
     *
     * 출력의 경로는 호스트 경로로 되돌려 준다. 컨테이너 안의 경로를 그대로 주면 호출부가
     * 어느 샌드박스로 돌았는지에 따라 다른 문자열을 다듬어야 한다.
     */
    fun exec(spec: SandboxSpec): ExecOutcome
}

/**
 * [Sandbox.exec] 의 결과.
 *
 * [exitCode] 가 null 이면 [SandboxSpec.perCaseTimeoutMillis] 안에 끝나지 않아 죽인 것이다.
 */
data class ExecOutcome(val exitCode: Int?, val output: String)

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
    /**
     * [workDir] 아래에서 쓸 수 있는 곳. 컴파일 산출물이 여기 남는다.
     *
     * 실행에는 비어 있다 — 사용자 코드는 아무 데도 쓰지 못한다.
     */
    val writablePaths: List<Path> = emptyList(),
    /**
     * 프로세스 수 상한. 컴파일러는 JVM 이라 스레드가 수십 개고, 실행의 상한으로는
     * 뜨지도 못한다.
     */
    val pidsLimit: Int? = null,
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
