package dev.codedrill.controlplane.submission.trace

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class PredictionRepository(private val jdbc: JdbcTemplate) {

    /**
     * 한 자리는 한 번만. 두 번째는 조용히 흡수하고 0 을 돌려준다.
     *
     * 부르는 쪽이 "이미 답한 자리"와 "방금 채점한 자리"를 갈라야 하므로 예외를 던지지
     * 않는다 — 사용자가 두 번 누른 것은 오류가 아니다.
     */
    fun insert(prediction: StatePrediction): Int = jdbc.update(
        """
        INSERT INTO state_prediction
            (id, user_id, submission_id, step, predicted, actual, correct, rationale, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (submission_id, step) DO NOTHING
        """.trimIndent(),
        prediction.id, prediction.userId, prediction.submissionId, prediction.step,
        prediction.predicted, prediction.actual, prediction.correct, prediction.rationale,
        java.sql.Timestamp.from(prediction.createdAt),
    )

    fun of(submissionId: UUID): List<StatePrediction> = jdbc.query(
        "SELECT * FROM state_prediction WHERE submission_id = ? ORDER BY step",
        MAPPER, submissionId,
    )

    /** 개인 데이터 반출 (§11.3). 근거는 사용자가 쓴 것이다. */
    fun export(userId: String): List<Map<String, Any?>> = jdbc.query(
        """
        SELECT submission_id, step, predicted, actual, correct, rationale, created_at
          FROM state_prediction WHERE user_id = ? ORDER BY created_at
        """.trimIndent(),
        { rs, _ ->
            mapOf(
                "submissionId" to rs.getString("submission_id"),
                "step" to rs.getInt("step"),
                "predicted" to rs.getString("predicted"),
                "actual" to rs.getString("actual"),
                "correct" to rs.getBoolean("correct"),
                "rationale" to rs.getString("rationale"),
                "createdAt" to rs.getTimestamp("created_at")?.toInstant(),
            )
        },
        userId,
    )

    /**
     * 개인 데이터 삭제 (§11.3).
     *
     * 근거만 비운다. 맞고 틀림은 역량 증거의 근거라, 지우면 지도가 무엇에 기대고 있는지
     * 말할 수 없게 된다 — 사전 질문이 같은 이유로 같은 모양을 쓴다.
     */
    fun erase(userId: String): Int =
        jdbc.update("UPDATE state_prediction SET rationale = NULL WHERE user_id = ?", userId)

    private companion object {
        val MAPPER = RowMapper { rs, _ ->
            StatePrediction(
                id = rs.getObject("id", UUID::class.java),
                userId = rs.getString("user_id"),
                submissionId = rs.getObject("submission_id", UUID::class.java),
                step = rs.getInt("step"),
                predicted = rs.getString("predicted"),
                actual = rs.getString("actual"),
                correct = rs.getBoolean("correct"),
                rationale = rs.getString("rationale"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            )
        }
    }
}
