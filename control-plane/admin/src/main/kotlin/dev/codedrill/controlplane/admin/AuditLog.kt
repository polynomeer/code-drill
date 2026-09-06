package dev.codedrill.controlplane.admin

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * 감사 로그 (기술 설계서 §13.3).
 *
 * 판정 변경·콘텐츠 공개·재채점·권한 변경을 남긴다. 쓰기 전용이며 수정·삭제 메서드가
 * 없다 — DB 트리거가 막고 있지만, 코드에도 그런 경로를 만들지 않아야 실수로라도
 * 우회하지 못한다 (§3.1 감사 로그 우회 금지).
 *
 * 기록은 행위와 같은 트랜잭션에서 일어난다. 나중에 따로 남기면 그 사이에 죽었을 때
 * 흔적 없는 변경이 생긴다.
 */
@Component
class AuditLog(private val jdbc: JdbcTemplate, private val json: ObjectMapper) {

    fun record(action: AuditAction, subject: String, actor: String, detail: Map<String, Any?>) {
        jdbc.update(
            """
            INSERT INTO audit_log (action, subject, actor, detail)
            VALUES (?, ?, ?, ?::jsonb)
            """.trimIndent(),
            action.name, subject, actor, json.writeValueAsString(detail),
        )
    }

    fun recent(subject: String?, limit: Int): List<AuditEntry> =
        if (subject == null) {
            jdbc.query(
                "SELECT * FROM audit_log ORDER BY id DESC LIMIT ?",
                MAPPER, limit,
            )
        } else {
            jdbc.query(
                "SELECT * FROM audit_log WHERE subject = ? ORDER BY id DESC LIMIT ?",
                MAPPER, subject, limit,
            )
        }

    private companion object {
        val MAPPER = RowMapper { rs, _ ->
            AuditEntry(
                id = rs.getLong("id"),
                action = AuditAction.valueOf(rs.getString("action")),
                subject = rs.getString("subject"),
                actor = rs.getString("actor"),
                detail = rs.getString("detail"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            )
        }
    }
}

/** 감사 대상 행위 (§13.3). 새 행위를 추가하면 여기에 먼저 등록한다. */
enum class AuditAction {
    PROBLEM_VERSION_REGISTERED,
    PROBLEM_PUBLISHED,
    PROBLEM_ARCHIVED,
    REJUDGE_REQUESTED,
    REJUDGE_APPROVED,
    REJUDGE_REJECTED,
}

data class AuditEntry(
    val id: Long,
    val action: AuditAction,
    val subject: String,
    val actor: String,
    val detail: String,
    val createdAt: Instant,
)
