package dev.codedrill.controlplane.coaching

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.codedrill.platform.problempackage.Competency
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class CoachingRepository(private val jdbc: JdbcTemplate, private val json: ObjectMapper) {

    fun insert(session: CoachingSession): Int = jdbc.update(
        """
        INSERT INTO coaching_session (id, user_id, problem_id, focus, revealed, started_at)
        VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?)
        """.trimIndent(),
        session.id, session.userId, session.problemId,
        json.writeValueAsString(session.focus.map { it.name }),
        json.writeValueAsString(session.revealed),
        java.sql.Timestamp.from(session.startedAt),
    )

    /**
     * 펼친 도움을 통째로 다시 쓴다.
     *
     * 행 단위로 붙이지 않는 이유는 같은 단계를 두 번 눌러도 한 번으로 남아야 하기
     * 때문이다 — 두 번 세면 가중치가 두 번 깎이고, 사용자는 같은 힌트를 다시 본 것으로
     * 벌을 받는다. 목록을 서비스에서 정규화해 통째로 넘긴다.
     */
    fun replaceRevealed(id: UUID, revealed: List<Assistance>): Int = jdbc.update(
        "UPDATE coaching_session SET revealed = ?::jsonb WHERE id = ? AND ended_at IS NULL",
        json.writeValueAsString(revealed), id,
    )

    fun end(id: UUID): Int = jdbc.update(
        "UPDATE coaching_session SET ended_at = now() WHERE id = ? AND ended_at IS NULL",
        id,
    )

    fun find(id: UUID): CoachingSession? =
        jdbc.query("SELECT * FROM coaching_session WHERE id = ?", mapper(), id).firstOrNull()

    /** 이 문제에서 아직 안 끝난 세션. 문제를 다시 열었을 때 이어 쓰라고 있다. */
    fun open(userId: String, problemId: String): CoachingSession? = jdbc.query(
        """
        SELECT * FROM coaching_session
         WHERE user_id = ? AND problem_id = ? AND ended_at IS NULL
         ORDER BY started_at DESC LIMIT 1
        """.trimIndent(),
        mapper(), userId, problemId,
    ).firstOrNull()

    /**
     * 이 사용자가 이 문제에서 받은 가장 깊은 도움 (끝난 세션까지 포함).
     *
     * 제출의 증거 가중치가 이 값을 본다 (FR-806). **세션을 닫았다고 도움이 없던 일이
     * 되지는 않는다** — 힌트를 3단계까지 보고 세션을 닫은 뒤 제출하면 무게가 온전해지는
     * 구멍이 생긴다.
     */
    fun deepestHelp(userId: String, problemId: String): Int = jdbc.query(
        "SELECT revealed FROM coaching_session WHERE user_id = ? AND problem_id = ?",
        { rs, _ -> json.readValue<List<Assistance>>(rs.getString("revealed")) },
        userId, problemId,
    ).flatten().maxOfOrNull { it.level } ?: 0

    /** 개인 데이터 반출 (§11.3). 무엇을 도움받았는지는 사용자의 기록이다. */
    fun export(userId: String): List<Map<String, Any?>> = jdbc.query(
        """
        SELECT id, problem_id, focus, revealed, started_at, ended_at
          FROM coaching_session WHERE user_id = ? ORDER BY started_at
        """.trimIndent(),
        { rs, _ ->
            mapOf(
                "id" to rs.getString("id"),
                "problemId" to rs.getString("problem_id"),
                "focus" to rs.getString("focus"),
                "revealed" to rs.getString("revealed"),
                "startedAt" to rs.getTimestamp("started_at")?.toInstant(),
                "endedAt" to rs.getTimestamp("ended_at")?.toInstant(),
            )
        },
        userId,
    )

    /**
     * 개인 데이터 삭제 (§11.3).
     *
     * 행을 지운다. 시험 실행과 달리 남길 것이 없다 — 여기서 세는 것은 용량이 아니라
     * 사용자가 무엇을 어려워했는가이고, 그것이 지워 달라고 요청받은 바로 그것이다.
     */
    fun erase(userId: String): Int =
        jdbc.update("DELETE FROM coaching_session WHERE user_id = ?", userId)

    private fun mapper() = RowMapper { rs, _ ->
        CoachingSession(
            id = rs.getObject("id", UUID::class.java),
            userId = rs.getString("user_id"),
            problemId = rs.getString("problem_id"),
            focus = json.readValue<List<String>>(rs.getString("focus"))
                .mapNotNull { name -> Competency.entries.firstOrNull { it.name == name } },
            revealed = json.readValue(rs.getString("revealed")),
            startedAt = rs.getTimestamp("started_at").toInstant(),
            endedAt = rs.getTimestamp("ended_at")?.toInstant(),
        )
    }
}
