package dev.codedrill.controlplane.project

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.codedrill.judge.protocol.ProjectProbeOutcome
import dev.codedrill.judge.protocol.ProjectTestOutcome
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.messaging.OutboxEvent
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

/**
 * 프로젝트형 제출의 저장소. [insertWithOutbox] 는 제출 행·파일·아웃박스 이벤트를 호출부의
 * 트랜잭션 안에서 함께 넣는다 — 도메인 행만 커밋되고 이벤트가 빠지는 경우를 구조적으로 막는다 (§3.2).
 */
@Repository
class ProjectRepository(private val jdbc: JdbcTemplate, private val json: ObjectMapper) {

    fun insertWithOutbox(submission: ProjectSubmission, files: Map<String, String>, event: OutboxEvent): Int {
        val inserted = jdbc.update(
            """
            INSERT INTO project_submission (id, user_id, idempotency_key, project_id, project_version, language, status, version)
            VALUES (?, ?, ?, ?, ?, ?, ?, 0)
            ON CONFLICT (user_id, idempotency_key) DO NOTHING
            """.trimIndent(),
            submission.id, submission.userId, submission.idempotencyKey, submission.projectId,
            submission.projectVersion, submission.language, submission.status.name,
        )
        if (inserted == 0) return 0

        jdbc.batchUpdate(
            "INSERT INTO project_submission_file (submission_id, path, content) VALUES (?, ?, ?)",
            files.entries.toList(),
            files.size,
        ) { ps, (path, content) ->
            ps.setObject(1, submission.id); ps.setString(2, path); ps.setString(3, content)
        }
        jdbc.update(
            """
            INSERT INTO outbox_event (id, aggregate, aggregate_id, type, payload, occurred_at)
            VALUES (?, ?, ?, ?, ?::jsonb, ?)
            """.trimIndent(),
            event.id, event.aggregate, event.aggregateId, event.type, event.payload, Timestamp.from(event.occurredAt),
        )
        return 1
    }

    fun findById(id: UUID): ProjectSubmission? =
        jdbc.query("SELECT * FROM project_submission WHERE id = ?", ::map, id).firstOrNull()

    fun findByIdempotencyKey(userId: String, key: String): ProjectSubmission? =
        jdbc.query("SELECT * FROM project_submission WHERE user_id = ? AND idempotency_key = ?", ::map, userId, key).firstOrNull()

    fun files(id: UUID): Map<String, String> = jdbc.query(
        "SELECT path, content FROM project_submission_file WHERE submission_id = ? ORDER BY path",
        { rs, _ -> rs.getString("path") to rs.getString("content") },
        id,
    ).toMap(linkedMapOf())

    /** 한 사람의 제출, 최근 것부터. 목록은 파일을 싣지 않는다. */
    fun history(userId: String, projectId: String?, limit: Int): List<ProjectSubmission> =
        // 필터를 SQL 로 접지 않는다 — `? IS NULL` 하나만 있는 자리는 Postgres 가 타입을 정하지 못한다.
        if (projectId == null) {
            jdbc.query("SELECT * FROM project_submission WHERE user_id = ? ORDER BY created_at DESC LIMIT ?", ::map, userId, limit)
        } else {
            jdbc.query(
                "SELECT * FROM project_submission WHERE user_id = ? AND project_id = ? ORDER BY created_at DESC LIMIT ?",
                ::map, userId, projectId, limit,
            )
        }

    /** 아직 끝나지 않은 제출 수. 한 사람이 분 단위 실행을 여럿 쌓지 못하게 한다 (§10.2). */
    fun inFlight(userId: String): Int = jdbc.queryForObject(
        "SELECT count(*) FROM project_submission WHERE user_id = ? AND status <> 'COMPLETED'",
        Int::class.java, userId,
    ) ?: 0

    /** 맞힌 프로젝트. 목록의 "완료" 표시가 이것이다. */
    fun solvedBy(userId: String): Set<String> = jdbc.queryForList(
        "SELECT DISTINCT project_id FROM project_submission WHERE user_id = ? AND verdict = 'ACCEPTED'",
        String::class.java, userId,
    ).toSet()

    fun transition(id: UUID, from: ProjectSubmission.Status, to: ProjectSubmission.Status, version: Int): Boolean =
        jdbc.update(
            "UPDATE project_submission SET status = ?, version = version + 1 WHERE id = ? AND status = ? AND version = ?",
            to.name, id, from.name, version,
        ) == 1

    /**
     * 판정을 이력에 남긴다. 같은 실행이 다시 오면 0 — 중복 전달이 revision 을 올리지 못하게 (§4.3).
     */
    fun recordJudgement(
        id: UUID,
        revision: Int,
        executionId: String,
        verdict: Verdict,
        score: Int,
        log: String?,
        tests: List<ProjectTestOutcome>,
        hiddenPassed: Int,
        hiddenTotal: Int,
        rejudgeJobId: UUID?,
        applied: Boolean,
        probe: ProjectProbeOutcome? = null,
    ): Int = jdbc.update(
        """
        INSERT INTO project_judgement (submission_id, revision, execution_id, verdict, score, log, tests, hidden_passed, hidden_total, rejudge_job_id, applied, probe)
        VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?::jsonb)
        ON CONFLICT (execution_id) DO NOTHING
        """.trimIndent(),
        id, revision, executionId, verdict.name, score, log, json.writeValueAsString(tests), hiddenPassed, hiddenTotal, rejudgeJobId, applied,
        probe?.let(json::writeValueAsString),
    )

    /** 현재 판정을 갈아 끼운다. 최초 판정은 종착으로 옮기고, 재채점은 revision 을 올린다. */
    fun applyJudgement(
        id: UUID,
        revision: Int,
        executionId: String,
        verdict: Verdict,
        score: Int,
        log: String?,
        tests: List<ProjectTestOutcome>,
        hiddenPassed: Int,
        hiddenTotal: Int,
        probe: ProjectProbeOutcome? = null,
    ): Boolean = jdbc.update(
        """
        UPDATE project_submission
           SET status = 'COMPLETED', verdict = ?, score = ?, log = ?, tests = ?::jsonb,
               hidden_passed = ?, hidden_total = ?, probe = ?::jsonb, execution_id = ?, revision = ?,
               completed_at = coalesce(completed_at, now()), version = version + 1
         WHERE id = ?
        """.trimIndent(),
        verdict.name, score, log, json.writeValueAsString(tests), hiddenPassed, hiddenTotal, probe?.let(json::writeValueAsString), executionId, revision, id,
    ) == 1

    fun judgements(id: UUID): List<ProjectJudgement> = jdbc.query(
        """
        SELECT revision, execution_id, verdict, score, hidden_passed, hidden_total, rejudge_job_id, applied, created_at
          FROM project_judgement WHERE submission_id = ? ORDER BY created_at
        """.trimIndent(),
        { rs, _ ->
            ProjectJudgement(
                revision = rs.getInt("revision"),
                executionId = rs.getString("execution_id"),
                verdict = Verdict.valueOf(rs.getString("verdict")),
                score = rs.getInt("score"),
                hiddenPassed = rs.getInt("hidden_passed"),
                hiddenTotal = rs.getInt("hidden_total"),
                rejudgeJobId = rs.getObject("rejudge_job_id", UUID::class.java),
                applied = rs.getBoolean("applied"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            )
        },
        id,
    )

    /** 재채점 대상 — 종료된 제출 (§8.1). */
    fun completedFor(projectId: String): List<UUID> =
        jdbc.queryForList("SELECT id FROM project_submission WHERE project_id = ? AND status = 'COMPLETED'", UUID::class.java, projectId)

    fun enqueueOutbox(event: OutboxEvent) {
        jdbc.update(
            """
            INSERT INTO outbox_event (id, aggregate, aggregate_id, type, payload, occurred_at)
            VALUES (?, ?, ?, ?, ?::jsonb, ?)
            """.trimIndent(),
            event.id, event.aggregate, event.aggregateId, event.type, event.payload, Timestamp.from(event.occurredAt),
        )
    }

    // --- 초안 (§8.1) ---

    fun findDraft(userId: String, projectId: String): ProjectDraft? = jdbc.query(
        "SELECT * FROM project_draft WHERE user_id = ? AND project_id = ?",
        { rs, _ ->
            ProjectDraft(
                userId = rs.getString("user_id"),
                projectId = rs.getString("project_id"),
                files = json.readValue<Map<String, String>>(rs.getString("files")),
                version = rs.getLong("version"),
                updatedAt = rs.getTimestamp("updated_at").toInstant(),
            )
        },
        userId, projectId,
    ).firstOrNull()

    /** 첫 저장. 이미 있으면 0 을 돌려주고, 호출부는 CAS 경로로 넘어간다. */
    fun insertDraft(userId: String, projectId: String, files: Map<String, String>): Int = jdbc.update(
        """
        INSERT INTO project_draft (user_id, project_id, files, version)
        VALUES (?, ?, ?::jsonb, 1)
        ON CONFLICT (user_id, project_id) DO NOTHING
        """.trimIndent(),
        userId, projectId, json.writeValueAsString(files),
    )

    /** 기대 버전일 때만 덮어쓴다. 0 이면 그 사이에 다른 곳에서 저장된 것이다. */
    fun compareAndSetDraft(userId: String, projectId: String, files: Map<String, String>, expectedVersion: Long): Int = jdbc.update(
        """
        UPDATE project_draft SET files = ?::jsonb, version = version + 1, updated_at = now()
         WHERE user_id = ? AND project_id = ? AND version = ?
        """.trimIndent(),
        json.writeValueAsString(files), userId, projectId, expectedVersion,
    )

    fun deleteDraft(userId: String, projectId: String): Int =
        jdbc.update("DELETE FROM project_draft WHERE user_id = ? AND project_id = ?", userId, projectId)

    // --- 개인 데이터 (§11.3) ---

    fun export(userId: String): List<Map<String, Any?>> {
        val rows = jdbc.queryForList(
            """
            SELECT id, project_id, project_version, language, status, verdict, score, log, hidden_passed, hidden_total, created_at
              FROM project_submission WHERE user_id = ? ORDER BY created_at
            """.trimIndent(),
            userId,
        )
        return rows.map { row -> row + ("files" to files(row["id"] as UUID)) }
    }

    fun exportDrafts(userId: String): List<Map<String, Any?>> = jdbc.query(
        "SELECT project_id, files, version, updated_at FROM project_draft WHERE user_id = ? ORDER BY updated_at",
        { rs, _ ->
            mapOf(
                "projectId" to rs.getString("project_id"),
                "files" to json.readValue<Map<String, String>>(rs.getString("files")),
                "version" to rs.getLong("version"),
                "updatedAt" to rs.getTimestamp("updated_at").toInstant(),
            )
        },
        userId,
    )

    fun eraseDrafts(userId: String): Int = jdbc.update("DELETE FROM project_draft WHERE user_id = ?", userId)

    fun submissionIds(userId: String): List<String> =
        jdbc.queryForList("SELECT id FROM project_submission WHERE user_id = ?", String::class.java, userId)

    /**
     * 사용자가 쓴 것을 지운다 — 파일과 로그. 제출 행은 남는다: 프로젝트의 통계가 여기서 나오고,
     * "이 문제가 몇 번 풀렸는가"는 그 사람의 것이 아니다. Submission 모듈과 같은 분담이다.
     */
    fun eraseContent(userId: String): Int {
        val files = jdbc.update(
            "DELETE FROM project_submission_file WHERE submission_id IN (SELECT id FROM project_submission WHERE user_id = ?)",
            userId,
        )
        jdbc.update("UPDATE project_submission SET log = NULL, tests = NULL WHERE user_id = ?", userId)
        return files
    }

    private fun map(rs: ResultSet, @Suppress("UNUSED_PARAMETER") row: Int) = ProjectSubmission(
        id = rs.getObject("id", UUID::class.java),
        userId = rs.getString("user_id"),
        idempotencyKey = rs.getString("idempotency_key"),
        projectId = rs.getString("project_id"),
        projectVersion = rs.getInt("project_version"),
        language = rs.getString("language"),
        status = ProjectSubmission.Status.valueOf(rs.getString("status")),
        verdict = rs.getString("verdict")?.let(Verdict::valueOf),
        score = rs.getObject("score") as Int?,
        log = rs.getString("log"),
        tests = rs.getString("tests")?.let { json.readValue<List<ProjectTestOutcome>>(it) }.orEmpty(),
        hiddenPassed = rs.getObject("hidden_passed") as Int?,
        hiddenTotal = rs.getObject("hidden_total") as Int?,
        probe = rs.getString("probe")?.let { json.readValue<ProjectProbeOutcome>(it) },
        createdAt = rs.getTimestamp("created_at").toInstant(),
        completedAt = rs.getTimestamp("completed_at")?.toInstant(),
        version = rs.getInt("version"),
        revision = rs.getInt("revision"),
    )
}
