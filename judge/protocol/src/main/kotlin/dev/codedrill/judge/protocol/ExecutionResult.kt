package dev.codedrill.judge.protocol

/**
 * Runner 가 Orchestrator 로 돌려주는 결과 봉투 (기술 설계서 §4.1, §11.1).
 *
 * Orchestrator 는 수신 시 스키마·버전·[fencingToken] 을 확인한 뒤 결정적으로 집계한다.
 * 늦게 도착한 결과는 토큰이 현재 attempt 와 어긋나면 감사 기록만 남기고 폐기한다 (§4.3).
 * 같은 [executionId] 와 [resultDigest] 가 다시 오면 no-op 이다.
 */
data class ExecutionResult(
    val schemaVersion: String = SCHEMA_VERSION,
    val executionId: String,
    val submissionId: String,
    val attempt: Int,
    val fencingToken: FencingToken,
    /**
     * 무엇을 채점했는지. 집계는 이 버전의 그룹 정책으로 해야 한다.
     *
     * 오케스트레이터가 제출과 문제를 따로 기억하면 둘이 어긋날 수 있다. 결과 봉투가
     * 스스로 말하게 해 두면 재채점과 감사에서도 근거가 하나로 남는다 (§8.1 판정 근거 고정).
     */
    val problemVersionId: String = "",
    /** 컴파일 실패나 플랫폼 장애처럼 케이스 실행 전에 끝난 경우에만 채운다. */
    val terminalVerdict: Verdict?,
    val compileLog: String?,
    val cases: List<TestCaseResult>,
    val resultDigest: String,
    val mode: ExecutionMode = ExecutionMode.JUDGE,
    /** TRACE 모드에서만 채워진다. 트레이스 실패가 판정을 흔들면 안 된다 (§12.2). */
    val trace: TraceCapture? = null,
) {
    companion object {
        const val SCHEMA_VERSION = "1.0"
    }
}

/** 케이스 하나의 판정과 측정치. */
data class TestCaseResult(
    val caseId: String,
    val groupId: String,
    val verdict: Verdict,
    val measurements: Measurements,
    /** 사용자에게 보여줄 짧은 사유. 숨은 그룹에서는 입력을 담지 않는다 (§8.3). */
    val message: String? = null,
)

/**
 * 워커 유실 후 재할당을 안전하게 만드는 단조 증가 토큰 (§4.3).
 *
 * 이전 워커가 살아 돌아와 결과를 보내도, 토큰이 현재 임대보다 낮으면 거절된다.
 */
@JvmInline
value class FencingToken(val value: Long) : Comparable<FencingToken> {
    override fun compareTo(other: FencingToken): Int = value.compareTo(other.value)
}

/**
 * 실행 자원 측정치.
 *
 * 슬라이스의 Runner 는 자식 JVM 이 보고하는 값을 쓴다. 운영에서는 cgroup v2 의
 * `memory.peak` 과 monotonic clock 이 진실의 원천이다 (§5.2).
 */
data class Measurements(
    val cpuTimeMillis: Long,
    val wallTimeMillis: Long,
    val peakMemoryBytes: Long,
    val outputBytes: Long,
) {
    companion object {
        val NONE = Measurements(0, 0, 0, 0)
    }
}
