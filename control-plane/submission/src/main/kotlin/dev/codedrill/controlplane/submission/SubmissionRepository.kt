package dev.codedrill.controlplane.submission

import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.messaging.OutboxEvent
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * 제출과 아웃박스 저장소.
 *
 * [insertWithOutbox] 는 이름 그대로 둘을 함께 넣는다. 호출부의 트랜잭션 안에서만
 * 의미가 있으며, 도메인 행만 커밋되고 이벤트가 빠지는 경우를 구조적으로 막는다 (§3.2).
 */
@Repository
class SubmissionRepository(private val jdbc: JdbcTemplate) {

    fun insertWithOutbox(submission: Submission, event: OutboxEvent): Int {
        val inserted = jdbc.update(
            """
            INSERT INTO submission (
                id, user_id, idempotency_key, problem_id, problem_version,
                language, source, status, version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
            ON CONFLICT (user_id, idempotency_key) DO NOTHING
            """.trimIndent(),
            submission.id, submission.userId, submission.idempotencyKey,
            submission.problemId, submission.problemVersion,
            submission.language, "", submission.status.name,
        )
        // 멱등 키가 겹쳤다면 이미 만들어진 제출이 있다. 이벤트를 또 넣지 않는다.
        if (inserted == 0) return 0

        jdbc.update(
            """
            INSERT INTO outbox_event (id, aggregate, aggregate_id, type, payload, occurred_at)
            VALUES (?, ?, ?, ?, ?::jsonb, ?)
            """.trimIndent(),
            event.id, event.aggregate, event.aggregateId, event.type, event.payload, java.sql.Timestamp.from(event.occurredAt),
        )
        return 1
    }

    fun updateSource(id: UUID, source: String) {
        jdbc.update("UPDATE submission SET source = ? WHERE id = ?", source, id)
    }

    fun findById(id: UUID): Submission? =
        jdbc.query("SELECT * FROM submission WHERE id = ?", MAPPER, id).firstOrNull()

    fun findSource(id: UUID): String? =
        jdbc.query("SELECT source FROM submission WHERE id = ?", { rs, _ -> rs.getString(1) }, id)
            .firstOrNull()

    fun findByIdempotencyKey(userId: String, key: String): Submission? =
        jdbc.query(
            "SELECT * FROM submission WHERE user_id = ? AND idempotency_key = ?",
            MAPPER, userId, key,
        ).firstOrNull()

    /**
     * 제출 기록 (기술 설계서 §9.1 cursor pagination, §8.2 INDEX(user_id, created_at desc)).
     *
     * 정렬 키를 `(created_at desc, id desc)` 로 고정한다. created_at 만으로는 같은 밀리초에
     * 들어온 제출의 순서가 흔들려, 커서로 이어볼 때 항목을 건너뛰거나 중복해서 보게 된다.
     */
    fun page(
        userId: String,
        problemId: String?,
        after: Pair<Instant, UUID>?,
        limit: Int,
    ): List<Submission> {
        val conditions = mutableListOf("user_id = ?")
        val args = mutableListOf<Any>(userId)

        problemId?.let {
            conditions += "problem_id = ?"
            args += it
        }
        after?.let { (createdAt, id) ->
            conditions += "(created_at, id) < (?, ?)"
            args += java.sql.Timestamp.from(createdAt)
            args += id
        }
        args += limit

        return jdbc.query(
            """
            SELECT * FROM submission
             WHERE ${conditions.joinToString(" AND ")}
             ORDER BY created_at DESC, id DESC
             LIMIT ?
            """.trimIndent(),
            MAPPER, *args.toTypedArray(),
        )
    }

    /**
     * 상태를 낙관적으로 옮긴다 (§3.2).
     *
     * 현재 상태와 version 을 함께 조건에 넣으므로, 두 소비자가 같은 전이를 시도하면
     * 하나만 성공한다. 0 을 돌려주면 호출부는 이미 처리된 것으로 보고 조용히 끝낸다.
     */
    fun transition(id: UUID, from: SubmissionStatus, to: SubmissionStatus, version: Long): Int =
        jdbc.update(
            """
            UPDATE submission
               SET status = ?, version = version + 1, updated_at = now()
             WHERE id = ? AND status = ? AND version = ?
            """.trimIndent(),
            to.name, id, from.name, version,
        )

    /**
     * 판정을 이력에 남긴다 (§4.2 INV-02).
     *
     * `execution_id` 가 유일하므로 같은 결과가 다시 와도 두 번 쌓이지 않는다. 0 을
     * 돌려주면 이미 기록된 실행이라는 뜻이고, 호출부는 거기서 멈춰야 한다 — 그러지
     * 않으면 중복 전달 한 번에 revision 이 하나씩 오른다 (§4.3).
     */
    fun recordJudgement(
        id: UUID,
        revision: Int,
        executionId: String,
        verdict: Verdict,
        score: Int,
        compileLog: String?,
        groupsJson: String,
        rejudgeJobId: UUID?,
        applied: Boolean,
    ): Int = jdbc.update(
        """
        INSERT INTO submission_judgement (
            submission_id, revision, execution_id, verdict, score,
            compile_log, groups, rejudge_job_id, applied
        ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)
        ON CONFLICT (execution_id) DO NOTHING
        """.trimIndent(),
        id, revision, executionId, verdict.name, score,
        compileLog, groupsJson, rejudgeJobId, applied,
    )

    fun judgements(id: UUID): List<Judgement> = jdbc.query(
        """
        SELECT revision, execution_id, verdict, score, rejudge_job_id, applied, created_at
          FROM submission_judgement WHERE submission_id = ?
         ORDER BY created_at
        """.trimIndent(),
        { rs, _ ->
            Judgement(
                revision = rs.getInt("revision"),
                executionId = rs.getString("execution_id"),
                verdict = Verdict.valueOf(rs.getString("verdict")),
                score = rs.getInt("score"),
                rejudgeJobId = rs.getObject("rejudge_job_id", UUID::class.java)?.toString(),
                applied = rs.getBoolean("applied"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            )
        },
        id,
    )

    fun complete(
        id: UUID,
        verdict: Verdict,
        score: Int,
        compileLog: String?,
        groupsJson: String,
    ): Int = jdbc.update(
        """
        UPDATE submission
           SET status = ?, verdict = ?, score = ?, compile_log = ?, groups = ?::jsonb,
               version = version + 1, updated_at = now()
         WHERE id = ? AND status <> ?
        """.trimIndent(),
        SubmissionStatus.COMPLETED.name, verdict.name, score, compileLog, groupsJson,
        id, SubmissionStatus.COMPLETED.name,
    )

    /**
     * 재채점 결과를 현재 판정으로 반영한다 (§8.1 revision).
     *
     * [complete] 와 달리 이미 COMPLETED 인 행을 대상으로 한다. 상태는 그대로 두고
     * 판정만 갈아 끼우며 revision 을 올린다 — 종료는 불변이고, 바뀌는 것은 "무엇으로
     * 판정됐는가"이지 "끝났는가"가 아니다 (§4.2).
     */
    fun revise(
        id: UUID,
        verdict: Verdict,
        score: Int,
        compileLog: String?,
        groupsJson: String,
    ): Int = jdbc.update(
        """
        UPDATE submission
           SET verdict = ?, score = ?, compile_log = ?, groups = ?::jsonb,
               revision = revision + 1, version = version + 1, updated_at = now()
         WHERE id = ? AND status = ?
        """.trimIndent(),
        verdict.name, score, compileLog, groupsJson, id, SubmissionStatus.COMPLETED.name,
    )

    /** 문제 하나의 종료된 제출 전부. 재채점 대상 산출에 쓴다. */
    fun completedIds(problemId: String): List<UUID> = jdbc.query(
        "SELECT id FROM submission WHERE problem_id = ? AND status = ? ORDER BY created_at",
        { rs, _ -> rs.getObject(1, UUID::class.java) },
        problemId, SubmissionStatus.COMPLETED.name,
    )

    fun completedId(id: UUID): List<UUID> = jdbc.query(
        "SELECT id FROM submission WHERE id = ? AND status = ?",
        { rs, _ -> rs.getObject(1, UUID::class.java) },
        id, SubmissionStatus.COMPLETED.name,
    )

    /** 아웃박스에 이벤트만 넣는다. 재채점은 제출 행을 새로 만들지 않는다. */
    fun enqueueOutbox(event: OutboxEvent) {
        jdbc.update(
            """
            INSERT INTO outbox_event (id, aggregate, aggregate_id, type, payload, occurred_at)
            VALUES (?, ?, ?, ?, ?::jsonb, ?)
            """.trimIndent(),
            event.id, event.aggregate, event.aggregateId, event.type, event.payload,
            java.sql.Timestamp.from(event.occurredAt),
        )
    }

    private companion object {
        val MAPPER = RowMapper { rs, _ ->
            Submission(
                id = rs.getObject("id", UUID::class.java),
                userId = rs.getString("user_id"),
                idempotencyKey = rs.getString("idempotency_key"),
                problemId = rs.getString("problem_id"),
                problemVersion = rs.getInt("problem_version"),
                language = rs.getString("language"),
                status = SubmissionStatus.valueOf(rs.getString("status")),
                verdict = rs.getString("verdict")?.let(Verdict::valueOf),
                score = rs.getObject("score") as? Int,
                compileLog = rs.getString("compile_log"),
                groupsJson = rs.getString("groups"),
                revision = rs.getInt("revision"),
                version = rs.getLong("version"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            )
        }
    }
}

/**
 * 개인 데이터 반출·삭제용 질의 (기술 설계서 §11.3).
 *
 * 저장소에 두는 이유는 이 SQL 이 표 구조를 알아야 하기 때문이다. **어느 열이 개인
 * 데이터인지는 표를 가진 쪽만 안다** — 밖에서 지우게 하면 열이 늘어날 때 조용히 빠진다.
 */
@org.springframework.stereotype.Repository
class SubmissionPersonalData(private val jdbc: org.springframework.jdbc.core.JdbcTemplate) {

    fun export(userId: String): List<Map<String, Any?>> = jdbc.queryForList(
        """
        SELECT id, problem_id, problem_version, language, source, status, verdict,
               score, compile_log, created_at
          FROM submission WHERE user_id = ? ORDER BY created_at
        """.trimIndent(),
        userId,
    )

    /**
     * 사용자가 쓴 내용을 지우고, 집계에 쓰이는 사실만 남긴다.
     *
     * 제출 행 자체는 지우지 않는다. 판정 이력이 이 행을 참조하고(§8.1), 문제별 통계와
     * 정답률이 여기서 나온다. 지워야 할 것은 **그 사람이 쓴 코드**이지 "이 문제가 몇 번
     * 풀렸는가"가 아니다.
     */
    fun erase(userId: String): Map<String, Int> {
        val traces = jdbc.update(
            """
            DELETE FROM trace WHERE submission_id IN (SELECT id FROM submission WHERE user_id = ?)
            """.trimIndent(),
            userId,
        )
        val sources = jdbc.update(
            "UPDATE submission SET source = NULL, compile_log = NULL WHERE user_id = ? AND source IS NOT NULL",
            userId,
        )
        return mapOf("submissionSources" to sources, "traces" to traces)
    }
}
