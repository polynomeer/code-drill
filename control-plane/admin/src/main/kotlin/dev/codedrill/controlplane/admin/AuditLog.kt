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

    /**
     * 같은 패키지를 새 검증 파이프라인으로 다시 검증해 보고서를 갈아 끼웠다 (§15.3).
     *
     * 등록과 구분해 남긴다. 문제는 하나도 바뀌지 않았고 검사 기준만 바뀐 것이라,
     * 둘을 같은 행위로 적으면 "언제 무엇이 바뀌었나"를 로그에서 되짚을 수 없다.
     */
    PROBLEM_VERSION_REVALIDATED,

    PROBLEM_PUBLISHED,
    PROBLEM_ARCHIVED,
    REJUDGE_REQUESTED,
    REJUDGE_APPROVED,
    REJUDGE_REJECTED,

    /** 승인된 재채점을 실제로 실행에 걸었다. 여기서부터 판정이 움직인다. */
    REJUDGE_DISPATCHED,

    /** 재채점이 판정을 바꿨다 (§13.3 판정 변경). 바뀐 것만 남긴다. */
    JUDGEMENT_REVISED,

    /**
     * 관리자 역할 부여를 요청했다 (§11.2).
     *
     * 아직 권한은 늘지 않았다. 부여와 갈라 남기는 이유는, 승인되지 않고 남은 요청과
     * 반려된 요청이 그 자체로 신호이기 때문이다 — 누가 무엇을 얻으려 했는가는 얻지
     * 못했을 때에도 감사가 묻는다.
     */
    ADMIN_ROLE_GRANT_REQUESTED,

    /** 역할 부여 요청을 반려했다 (§11.2). */
    ADMIN_ROLE_GRANT_REJECTED,

    /**
     * 관리자 역할을 줬다 (§11.2).
     *
     * 권한이 바뀐 순간이다. 설정 파일에 적힌 역할은 누가 언제 바꿨는지 남기지 않는데,
     * 감사에서 가장 먼저 묻는 것이 그것이다.
     */
    ADMIN_ROLE_GRANTED,

    /** 관리자 역할을 회수했다 (§11.2). */
    ADMIN_ROLE_REVOKED,

    /**
     * 관리자 API 접근이 거부됐다 (§11.4).
     *
     * 성공만 남기면 공격의 앞부분 — 토큰을 찾아 두드리는 구간 — 이 통째로 비어 있다.
     */
    ADMIN_ACCESS_DENIED,
}

data class AuditEntry(
    val id: Long,
    val action: AuditAction,
    val subject: String,
    val actor: String,
    val detail: String,
    val createdAt: Instant,
)
