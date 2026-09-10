package dev.codedrill.controlplane.submission.trace

import dev.codedrill.controlplane.submission.LearningSignals
import dev.codedrill.controlplane.submission.SubmissionRepository
import dev.codedrill.judge.protocol.TraceEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 다음 상태 예측 (PRD FR-805).
 *
 * **채점은 서버가 한다.** 클라이언트가 맞고 틀림을 정하면 그것은 채점이 아니라 자기
 * 신고이고, 증거로 쓸 수 없다.
 *
 * 다만 트레이스 자체는 이미 클라이언트에 있다 — 자기 실행의 기록이므로 당연히 그렇다.
 * 그래서 마음먹으면 답을 보고 누를 수 있고, **그 사실을 증거 무게에 반영한다.** 감출 수
 * 없는 구멍이면 감추는 척하지 않고 값을 낮춰 세는 것이 맞다.
 */
@Service
class PredictionService(
    private val repository: PredictionRepository,
    private val traces: TraceRepository,
    private val submissions: SubmissionRepository,
    private val learning: LearningSignals = LearningSignals.NONE,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun predict(userId: String, submissionId: UUID, step: Int, predicted: String, rationale: String?): Outcome {
        val submission = submissions.findById(submissionId)?.takeIf { it.userId == userId }
            ?: return Outcome.NotFound

        val event = eventAt(submissionId, step) ?: return Outcome.NoSuchStep
        val actual = event.eventType.name
        val correct = predicted == actual

        val prediction = StatePrediction(
            id = UUID.randomUUID(),
            userId = userId,
            submissionId = submissionId,
            step = step,
            predicted = predicted,
            actual = actual,
            correct = correct,
            rationale = rationale?.trim()?.takeIf { it.isNotEmpty() },
            createdAt = Instant.now(),
        )

        // 이미 맞혀 본 자리다. 두 번째는 예측이 아니라 받아쓰기다.
        if (repository.insert(prediction) == 0) return Outcome.AlreadyAnswered

        runCatching {
            learning.predicted(userId, submission.problemId, prediction.id.toString(), correct)
        }.onFailure { log.warn("예측을 학습 기록에 남기지 못했다: {} ({})", prediction.id, it.message) }

        return Outcome.Graded(prediction)
    }

    /** 이 제출에서 이미 맞혀 본 자리들. 화면이 같은 자리를 다시 묻지 않게 한다. */
    fun answered(userId: String, submissionId: UUID): List<StatePrediction> =
        submissions.findById(submissionId)?.takeIf { it.userId == userId }
            ?.let { repository.of(submissionId) }
            .orEmpty()

    /**
     * seq 로 이벤트를 찾는다.
     *
     * 화면의 위치가 아니라 **이벤트 자신의 번호**로 받는다. 위치는 클라이언트가 몇 개를
     * 불러왔는지에 달려 있어 서버와 어긋날 수 있고, 어긋나면 사용자가 본 자리와 채점한
     * 자리가 달라진다.
     *
     * 요약이 아니라 청크에서 읽는다. 요약은 중요도로 골라 낸 것이라 그 사이에 빠진
     * 이벤트가 있다.
     */
    private fun eventAt(submissionId: UUID, seq: Int): TraceEvent? {
        if (seq < 1) return null
        val manifest = traces.findManifest(submissionId) ?: return null
        val chunk = manifest.chunks.firstOrNull { seq >= it.firstSeq && seq <= it.lastSeq }
            ?: return null
        return traces.findChunk(submissionId, chunk.index)?.events?.firstOrNull { it.seq == seq.toLong() }
    }

    sealed interface Outcome {
        data class Graded(val prediction: StatePrediction) : Outcome
        data object NoSuchStep : Outcome
        data object AlreadyAnswered : Outcome
        data object NotFound : Outcome
    }
}
