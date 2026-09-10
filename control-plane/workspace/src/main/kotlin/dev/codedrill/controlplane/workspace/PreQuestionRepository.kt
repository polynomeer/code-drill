package dev.codedrill.controlplane.workspace

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * 풀이 전 응답 저장소 (PRD FR-803).
 *
 * 덮어쓰지 않고 쌓는다. 마지막 답만 남기면 **바뀌었다는 사실**이 사라지는데, 처음엔
 * O(n²)라 했다가 나중에 O(n log n)이라 하는 것은 그 사이에 배운 것이 있다는 뜻이다.
 */
@Repository
class PreQuestionRepository(private val jdbc: JdbcTemplate) {

    fun insert(response: PreQuestionResponse): Int = jdbc.update(
        """
        INSERT INTO prequestion_response
            (id, user_id, problem_id, kind, answer, correct, rationale, misconception)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent(),
        response.id, response.userId, response.problemId, response.kind.name,
        response.answer, response.correct, response.rationale, response.misconception?.name,
    )

    /** 이 문제에 대한 이 사용자의 최근 응답. 질문마다 마지막 것 하나씩. */
    fun latest(userId: String, problemId: String): List<PreQuestionResponse> = jdbc.query(
        """
        SELECT DISTINCT ON (kind) *
          FROM prequestion_response
         WHERE user_id = ? AND problem_id = ?
         ORDER BY kind, answered_at DESC
        """.trimIndent(),
        MAPPER, userId, problemId,
    )

    /** 개인 데이터 반출 (§11.3). */
    fun export(userId: String): List<Map<String, Any?>> = jdbc.query(
        """
        SELECT problem_id, kind, answer, correct, rationale, misconception, answered_at
          FROM prequestion_response WHERE user_id = ? ORDER BY answered_at
        """.trimIndent(),
        { rs, _ ->
            mapOf(
                "problemId" to rs.getString("problem_id"),
                "kind" to rs.getString("kind"),
                "answer" to rs.getString("answer"),
                "correct" to rs.getBoolean("correct"),
                "rationale" to rs.getString("rationale"),
                "misconception" to rs.getString("misconception"),
                "answeredAt" to rs.getTimestamp("answered_at")?.toInstant(),
            )
        },
        userId,
    )

    /**
     * 개인 데이터 삭제 (§11.3).
     *
     * 사용자가 적은 근거만 지운다. 정답 여부와 오개념은 문제의 통계이지 그 사람을 가리키는
     * 값이 아니고, 계정 삭제는 익명화이지 이력 말소가 아니다 (IdentityService.deleteAccount).
     */
    fun erase(userId: String): Int = jdbc.update(
        "UPDATE prequestion_response SET rationale = NULL WHERE user_id = ?",
        userId,
    )

    private companion object {
        val MAPPER = RowMapper { rs, _ ->
            PreQuestionResponse(
                id = rs.getObject("id", UUID::class.java),
                userId = rs.getString("user_id"),
                problemId = rs.getString("problem_id"),
                kind = QuestionKind.valueOf(rs.getString("kind")),
                answer = rs.getString("answer"),
                correct = rs.getBoolean("correct"),
                rationale = rs.getString("rationale"),
                misconception = rs.getString("misconception")?.let(Misconception::valueOf),
                answeredAt = rs.getTimestamp("answered_at").toInstant(),
            )
        }
    }
}
