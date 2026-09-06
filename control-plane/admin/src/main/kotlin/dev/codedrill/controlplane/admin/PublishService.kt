package dev.codedrill.controlplane.admin

import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 콘텐츠 수명주기 (기술 설계서 §3.1 Admin, §3.2 공개).
 *
 * 두 가지를 지킨다.
 *
 * 1. **검증하지 않은 버전은 공개할 수 없다.** 등록 시 §6.3 보고서 digest 를 함께 받고,
 *    공개 요청은 그 digest 를 다시 제시해야 한다. 패키지를 고치면 digest 가 달라지므로
 *    "검증 후 몰래 바꿔치기"가 통하지 않는다.
 * 2. **작성자와 승인자를 분리한다** (§11.2). 문제 공개는 사용자에게 보이는 것을 바꾸는
 *    행위라 한 사람이 혼자 끝낼 수 없어야 한다.
 *
 * 공개 자체는 포인터 교체 한 번이다 (§3.2). 버전 행을 고치지 않으므로 공개 도중에
 * 조회하는 요청은 이전 버전이나 새 버전 중 하나를 보게 되고, 그 중간 상태는 없다.
 */
@Service
class PublishService(private val jdbc: JdbcTemplate, private val audit: AuditLog) {

    /**
     * 검증을 통과한 버전을 등록한다. 아직 공개되지는 않는다.
     *
     * 같은 패키지 digest 가 이미 등록돼 있으면 거절한다. 내용이 같은데 버전만 다른 것이
     * 여럿 있으면 어느 것을 공개했는지 근거가 흐려진다.
     */
    @Transactional
    fun registerVersion(
        problemId: String,
        version: Int,
        packageDigest: String,
        reportDigest: String,
        actor: String,
    ): RegisterOutcome {
        jdbc.update(
            "INSERT INTO problem (id) VALUES (?) ON CONFLICT (id) DO NOTHING",
            problemId,
        )

        val versionId = "$problemId@$version"
        return try {
            jdbc.update(
                """
                INSERT INTO problem_version
                    (id, problem_id, version, package_digest, report_digest, registered_by)
                VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                versionId, problemId, version, packageDigest, reportDigest, actor,
            )
            audit.record(
                AuditAction.PROBLEM_VERSION_REGISTERED,
                subject = versionId,
                actor = actor,
                detail = mapOf("packageDigest" to packageDigest, "reportDigest" to reportDigest),
            )
            RegisterOutcome.Registered(versionId)
        } catch (e: DuplicateKeyException) {
            RegisterOutcome.Rejected(
                "이미 등록된 버전이거나 같은 내용의 패키지가 있다: ${e.message?.take(160)}",
            )
        }
    }

    /**
     * 공개 (§3.2 published_version_id 포인터 교체).
     *
     * [approver] 는 등록자와 달라야 한다. 같으면 2인 승인이 이름만 남는다.
     */
    @Transactional
    fun publish(problemId: String, version: Int, reportDigest: String, approver: String): PublishOutcome {
        val versionId = "$problemId@$version"
        val row = jdbc.query(
            "SELECT report_digest, registered_by FROM problem_version WHERE id = ?",
            { rs, _ -> rs.getString("report_digest") to rs.getString("registered_by") },
            versionId,
        ).firstOrNull() ?: return PublishOutcome.Rejected("등록되지 않은 버전이다: $versionId")

        val (registeredDigest, registeredBy) = row

        if (registeredDigest != reportDigest) {
            return PublishOutcome.Rejected(
                "검증 보고서 digest 가 다르다. 패키지가 바뀌었으면 다시 검증해야 한다 (§6.3)",
            )
        }
        if (registeredBy == approver) {
            return PublishOutcome.Rejected(
                "등록자와 승인자가 같다. 문제 공개는 두 사람이 필요하다 (§11.2)",
            )
        }

        jdbc.update("UPDATE problem SET published_version_id = ? WHERE id = ?", versionId, problemId)
        audit.record(
            AuditAction.PROBLEM_PUBLISHED,
            subject = versionId,
            actor = approver,
            detail = mapOf("registeredBy" to registeredBy, "reportDigest" to reportDigest),
        )
        return PublishOutcome.Published(versionId)
    }

    /**
     * 논리 삭제 (§8.1).
     *
     * 이미 채점된 제출이 이 문제를 참조하므로 행을 지우지 않는다. 목록에서 감추기만 한다.
     */
    @Transactional
    fun archive(problemId: String, actor: String, reason: String) {
        jdbc.update("UPDATE problem SET archived = true WHERE id = ?", problemId)
        audit.record(
            AuditAction.PROBLEM_ARCHIVED,
            subject = problemId,
            actor = actor,
            detail = mapOf("reason" to reason),
        )
    }

    fun status(problemId: String): ProblemStatus? =
        jdbc.query(
            """
            SELECT p.id, p.published_version_id, p.archived,
                   (SELECT count(*) FROM problem_version v WHERE v.problem_id = p.id) AS versions
              FROM problem p WHERE p.id = ?
            """.trimIndent(),
            { rs, _ ->
                ProblemStatus(
                    problemId = rs.getString("id"),
                    publishedVersionId = rs.getString("published_version_id"),
                    archived = rs.getBoolean("archived"),
                    versionCount = rs.getInt("versions"),
                )
            },
            problemId,
        ).firstOrNull()

    /** 공개된 문제만. 목록 API 가 이 결과로 걸러야 미공개 문제가 새지 않는다. */
    fun publishedProblemIds(): Set<String> =
        jdbc.query(
            "SELECT id FROM problem WHERE published_version_id IS NOT NULL AND NOT archived",
            { rs, _ -> rs.getString(1) },
        ).toSet()

    sealed interface RegisterOutcome {
        data class Registered(val versionId: String) : RegisterOutcome
        data class Rejected(val reason: String) : RegisterOutcome
    }

    sealed interface PublishOutcome {
        data class Published(val versionId: String) : PublishOutcome
        data class Rejected(val reason: String) : PublishOutcome
    }
}

data class ProblemStatus(
    val problemId: String,
    val publishedVersionId: String?,
    val archived: Boolean,
    val versionCount: Int,
)
