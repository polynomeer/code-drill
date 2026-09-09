package dev.codedrill.controlplane.workspace

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

/**
 * 초안 저장소.
 *
 * 저장은 전부 조건부다. 무조건 덮어쓰는 경로를 두지 않아야, 실수로 CAS 를 우회하는
 * 코드가 생기지 않는다.
 */
@Repository
class WorkspaceRepository(private val jdbc: JdbcTemplate) {

    fun find(userId: String, problemId: String, language: String): WorkspaceDraft? =
        jdbc.query(
            """
            SELECT * FROM workspace_draft
             WHERE user_id = ? AND problem_id = ? AND language = ?
            """.trimIndent(),
            MAPPER, userId, problemId, language,
        ).firstOrNull()

    /** 첫 저장. 이미 있으면 0 을 돌려주고, 호출부는 CAS 경로로 넘어간다. */
    fun insert(userId: String, problemId: String, language: String, code: String): Int =
        jdbc.update(
            """
            INSERT INTO workspace_draft (user_id, problem_id, language, code, version)
            VALUES (?, ?, ?, ?, 1)
            ON CONFLICT (user_id, problem_id, language) DO NOTHING
            """.trimIndent(),
            userId, problemId, language, code,
        )

    /**
     * 기대 버전일 때만 덮어쓴다.
     *
     * 0 을 돌려주면 그 사이에 누군가 저장했다는 뜻이다. 조용히 이기지 않고 호출부가
     * 충돌로 알린다.
     */
    fun compareAndSet(
        userId: String,
        problemId: String,
        language: String,
        code: String,
        expectedVersion: Long,
    ): Int = jdbc.update(
        """
        UPDATE workspace_draft
           SET code = ?, version = version + 1, updated_at = now()
         WHERE user_id = ? AND problem_id = ? AND language = ? AND version = ?
        """.trimIndent(),
        code, userId, problemId, language, expectedVersion,
    )

    /** 최근에 손댄 초안. "이어서 풀기" 진입점이 된다. */
    fun recent(userId: String, limit: Int): List<WorkspaceDraft> =
        jdbc.query(
            "SELECT * FROM workspace_draft WHERE user_id = ? ORDER BY updated_at DESC LIMIT ?",
            MAPPER, userId, limit,
        )

    private companion object {
        val MAPPER = RowMapper { rs, _ ->
            WorkspaceDraft(
                userId = rs.getString("user_id"),
                problemId = rs.getString("problem_id"),
                language = rs.getString("language"),
                code = rs.getString("code"),
                version = rs.getLong("version"),
                updatedAt = rs.getTimestamp("updated_at").toInstant(),
            )
        }
    }
}

/** 초안의 개인 데이터 (기술 설계서 §11.3). 초안은 통째로 사용자가 쓴 것이라 그냥 지운다. */
@org.springframework.stereotype.Repository
class DraftPersonalData(private val jdbc: org.springframework.jdbc.core.JdbcTemplate) {

    fun export(userId: String): List<Map<String, Any?>> = jdbc.queryForList(
        "SELECT problem_id, language, code, version, updated_at FROM workspace_draft WHERE user_id = ?",
        userId,
    )

    fun erase(userId: String): Map<String, Int> =
        mapOf("drafts" to jdbc.update("DELETE FROM workspace_draft WHERE user_id = ?", userId))
}
