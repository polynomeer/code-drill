package dev.codedrill.controlplane.learning

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
class LearningRepository(private val jdbc: JdbcTemplate) {

    fun skip(userId: String, problemId: String, on: LocalDate): Int = jdbc.update(
        """
        INSERT INTO prescription_skip (user_id, problem_id, skipped_on) VALUES (?, ?, ?)
        ON CONFLICT DO NOTHING
        """.trimIndent(),
        userId, problemId, java.sql.Date.valueOf(on),
    )

    fun skipped(userId: String, on: LocalDate): Set<String> = jdbc.query(
        "SELECT problem_id FROM prescription_skip WHERE user_id = ? AND skipped_on = ?",
        { rs, _ -> rs.getString("problem_id") },
        userId, java.sql.Date.valueOf(on),
    ).toSet()

    // --- 문제집 (FR-205) ---

    fun createCollection(id: UUID, userId: String, name: String): Int = jdbc.update(
        "INSERT INTO collection (id, user_id, name) VALUES (?, ?, ?)", id, userId, name,
    )

    fun collections(userId: String): List<Collection> = jdbc.query(
        """
        SELECT c.id, c.name, c.created_at,
               coalesce(array_agg(i.problem_id ORDER BY i.added_at) FILTER (WHERE i.problem_id IS NOT NULL), '{}') AS problems
          FROM collection c
          LEFT JOIN collection_item i ON i.collection_id = c.id
         WHERE c.user_id = ?
         GROUP BY c.id
         ORDER BY c.created_at
        """.trimIndent(),
        { rs, _ ->
            @Suppress("UNCHECKED_CAST")
            Collection(
                id = rs.getObject("id", UUID::class.java),
                name = rs.getString("name"),
                problems = (rs.getArray("problems").array as Array<String>).toList(),
            )
        },
        userId,
    )

    fun owns(userId: String, collectionId: UUID): Boolean = jdbc.queryForObject(
        "SELECT count(*) FROM collection WHERE id = ? AND user_id = ?", Int::class.java, collectionId, userId,
    ) == 1

    /** 이미 있으면 0 이다 — 중복 저장은 오류가 아니라 무시다 (FR-205). */
    fun addItem(collectionId: UUID, problemId: String): Int = jdbc.update(
        "INSERT INTO collection_item (collection_id, problem_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
        collectionId, problemId,
    )

    fun removeItem(collectionId: UUID, problemId: String): Int = jdbc.update(
        "DELETE FROM collection_item WHERE collection_id = ? AND problem_id = ?", collectionId, problemId,
    )

    fun deleteCollection(collectionId: UUID): Int =
        jdbc.update("DELETE FROM collection WHERE id = ?", collectionId)

    /** 개인 데이터 (§11.3). 문제집도 처방 조정도 사용자의 것이다. */
    fun export(userId: String): Map<String, Any?> = mapOf(
        "collections" to collections(userId),
        "skips" to jdbc.query(
            "SELECT problem_id, skipped_on FROM prescription_skip WHERE user_id = ? ORDER BY skipped_on",
            { rs, _ -> mapOf("problemId" to rs.getString(1), "on" to rs.getDate(2).toLocalDate().toString()) },
            userId,
        ),
    )

    fun erase(userId: String): Map<String, Int> = mapOf(
        "collections" to jdbc.update("DELETE FROM collection WHERE user_id = ?", userId),
        "skips" to jdbc.update("DELETE FROM prescription_skip WHERE user_id = ?", userId),
    )
}

data class Collection(val id: UUID, val name: String, val problems: List<String>)
