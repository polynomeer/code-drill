package dev.codedrill.judge.protocol

/**
 * Runner 가 Orchestrator 로 돌려주는 결과 봉투 (기술 설계서 §4.1, §11.1).
 *
 * Orchestrator 는 수신 시 서명·스키마·버전을 확인한 뒤 결정적으로 집계한다.
 * 늦게 도착한 결과는 [fencingToken] 이 현재 attempt 와 어긋나면 감사 기록만 남기고
 * 폐기한다 (§4.3). 같은 [executionId] 와 [resultDigest] 가 다시 오면 no-op 이다.
 */
data class ExecutionResult(
    val schemaVersion: String,
    val executionId: String,
    val submissionId: String,
    val attempt: Int,
    val fencingToken: FencingToken,
    val verdict: Verdict,
    val measurements: Measurements,
    val resultDigest: String,
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

/** cgroup 과 monotonic clock 으로 측정한 실행 자원 (§5.2). */
data class Measurements(
    val cpuTimeMillis: Long,
    val wallTimeMillis: Long,
    val peakMemoryBytes: Long,
    val outputBytes: Long,
)
