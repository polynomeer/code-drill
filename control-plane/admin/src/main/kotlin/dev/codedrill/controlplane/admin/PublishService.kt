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
     *
     * **패키지는 그대로인데 파이프라인만 바뀐 경우는 예외로 다시 받는다** (§15.3). 그때
     * 필요한 것은 새 버전이 아니라 새 보고서다 — 문제는 하나도 바뀌지 않았고, 달라진 것은
     * 그 문제를 무엇으로 검사했는가뿐이다. 새 버전을 강요하면 바뀐 것 없는 버전 번호가
     * 파이프라인을 고칠 때마다 하나씩 늘어난다.
     */
    @Transactional
    fun registerVersion(
        problemId: String,
        version: Int,
        packageDigest: String,
        reportDigest: String,
        validatorVersion: String,
        actor: String,
    ): RegisterOutcome {
        jdbc.update(
            "INSERT INTO problem (id) VALUES (?) ON CONFLICT (id) DO NOTHING",
            problemId,
        )

        val versionId = "$problemId@$version"
        existing(versionId)?.let { row ->
            return revalidate(row, versionId, packageDigest, reportDigest, validatorVersion, actor)
        }

        return try {
            jdbc.update(
                """
                INSERT INTO problem_version
                    (id, problem_id, version, package_digest, report_digest,
                     validator_version, registered_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                versionId, problemId, version, packageDigest, reportDigest, validatorVersion, actor,
            )
            audit.record(
                AuditAction.PROBLEM_VERSION_REGISTERED,
                subject = versionId,
                actor = actor,
                detail = mapOf(
                    "packageDigest" to packageDigest,
                    "reportDigest" to reportDigest,
                    "validatorVersion" to validatorVersion,
                ),
            )
            RegisterOutcome.Registered(versionId)
        } catch (e: DuplicateKeyException) {
            RegisterOutcome.Rejected(
                "이미 등록된 버전이거나 같은 내용의 패키지가 있다: ${e.message?.take(160)}",
            )
        }
    }

    /**
     * 같은 버전이 이미 등록돼 있을 때의 처리.
     *
     * 패키지가 조금이라도 다르면 거절한다 — 그건 새 버전이어야 한다 (§6.1 공개 후 불변).
     * 같은 패키지를 새 파이프라인으로 다시 검증한 경우에만 보고서를 갈아 끼우고, 그
     * 사실을 감사 로그에 별도 행위로 남긴다.
     */
    private fun revalidate(
        row: RegisteredVersion,
        versionId: String,
        packageDigest: String,
        reportDigest: String,
        validatorVersion: String,
        actor: String,
    ): RegisterOutcome {
        if (row.packageDigest != packageDigest) {
            return RegisterOutcome.Rejected(
                "이미 등록된 버전인데 패키지가 다르다. 고친 패키지는 새 버전이어야 한다 (§6.1)",
            )
        }
        if (row.validatorVersion == validatorVersion && row.reportDigest == reportDigest) {
            // 시딩이 여러 번 돌아도 같은 결과여야 한다.
            return RegisterOutcome.Registered(versionId)
        }

        // 파이프라인이 그대로인데 보고서가 달라졌으면 검증 입력이 바뀐 것이다. 패키지
        // digest 는 manifest 와 tests 만 덮으므로(§6.1 — 사용자에게 나가는 것이 그 둘이다),
        // 참조 풀이와 오답을 고치면 **패키지는 그대로인 채 보고서만 달라진다.**
        //
        // 그래서 이 상태를 "검증이 결정적이지 않다"로 읽을 수 없다. 제어 영역이 보는 것은
        // 표본 하나뿐이라, 재실행마다 흔들리는 것과 입력이 바뀐 것을 가릴 수 없다. 가릴 수
        // 없는 것을 가린다고 말하면 오답 하나를 고칠 때마다 사용자에게 보이는 버전 번호가
        // 올라간다.
        jdbc.update(
            "UPDATE problem_version SET report_digest = ?, validator_version = ?, registered_by = ? WHERE id = ?",
            reportDigest, validatorVersion, actor, versionId,
        )
        audit.record(
            AuditAction.PROBLEM_VERSION_REVALIDATED,
            subject = versionId,
            actor = actor,
            detail = mapOf(
                "packageDigest" to packageDigest,
                "reportDigest" to "${row.reportDigest} -> $reportDigest",
                "validatorVersion" to if (row.validatorVersion == validatorVersion) {
                    validatorVersion
                } else {
                    "${row.validatorVersion} -> $validatorVersion"
                },
            ),
        )
        return RegisterOutcome.Registered(versionId)
    }

    private fun existing(versionId: String): RegisteredVersion? = jdbc.query(
        """
        SELECT package_digest, report_digest, validator_version, registered_by
          FROM problem_version WHERE id = ?
        """.trimIndent(),
        { rs, _ ->
            RegisteredVersion(
                packageDigest = rs.getString("package_digest"),
                reportDigest = rs.getString("report_digest"),
                validatorVersion = rs.getString("validator_version"),
                registeredBy = rs.getString("registered_by"),
            )
        },
        versionId,
    ).firstOrNull()

    private data class RegisteredVersion(
        val packageDigest: String,
        val reportDigest: String,
        val validatorVersion: String,
        val registeredBy: String,
    )

    /**
     * 공개 (§3.2 published_version_id 포인터 교체).
     *
     * [approver] 는 등록자와 달라야 한다. 같으면 2인 승인이 이름만 남는다.
     */
    @Transactional
    fun publish(
        problemId: String,
        version: Int,
        reportDigest: String,
        validatorVersion: String,
        approver: String,
    ): PublishOutcome {
        val versionId = "$problemId@$version"
        val row = existing(versionId)
            ?: return PublishOutcome.Rejected("등록되지 않은 버전이다: $versionId")

        // 파이프라인 버전을 먼저 본다. digest 가 어긋나는 원인이 둘인데, 이쪽이면
        // 패키지는 손댈 필요가 없다 — 뒤집힌 순서로 물으면 바꾼 적 없는 패키지를
        // 뒤지게 된다 (§15.3).
        if (row.validatorVersion != validatorVersion) {
            return PublishOutcome.Rejected(
                "검증 파이프라인이 바뀌었다: 등록은 ${row.validatorVersion}, 지금은 $validatorVersion. " +
                    "패키지는 그대로여도 보고서는 다시 만들어야 한다. " +
                    "validateContent 를 돌려 같은 버전으로 다시 등록한다 (§6.3, §15.3)",
            )
        }
        if (row.reportDigest != reportDigest) {
            return PublishOutcome.Rejected(
                "검증 보고서 digest 가 다르다. 파이프라인은 같으므로($validatorVersion) " +
                    "패키지가 바뀐 것이다. 고친 패키지는 새 버전이어야 한다 (§6.1, §6.3)",
            )
        }
        val registeredBy = row.registeredBy
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
            detail = mapOf(
                "registeredBy" to registeredBy,
                "reportDigest" to reportDigest,
                "validatorVersion" to validatorVersion,
            ),
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
