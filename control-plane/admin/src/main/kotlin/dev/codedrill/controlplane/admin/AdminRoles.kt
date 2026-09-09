package dev.codedrill.controlplane.admin

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 계정에 무엇을 허용하는지 (기술 설계서 §11.2).
 *
 * **신원은 여기서 정하지 않는다.** 누가 호출했는지는 Identity 모듈이 토큰으로 확인하고,
 * 이 모듈은 확인된 계정에 어떤 역할이 붙어 있는지만 본다. 둘을 갈라 두면 관리자 API 가
 * 별도의 장기 비밀을 갖지 않아도 되고, 감사 로그의 actor 가 실제 계정과 이어진다.
 *
 * 역할을 설정이 아니라 데이터로 두는 이유는 **누가 언제 줬는지가 남아야** 하기 때문이다.
 * 환경변수에 적힌 역할은 바꾼 사람도, 바꾼 시점도 남기지 않는다 (§13.3).
 *
 * 부여는 요청과 승인으로 나뉜다 ([requestGrant], [approveGrant]). 권한이 늘어나는
 * 순간은 다른 모든 2인 승인이 서 있는 바닥이라, 그 바닥을 한 사람이 혼자 움직일 수
 * 있으면 위에 쌓은 것이 전부 함께 내려앉는다 — V15 마이그레이션에 자세히 적었다.
 *
 * **회수는 즉시다.** 권한을 줄이는 일까지 승인을 기다리면, 사고가 났을 때 가장 급한
 * 조치가 가장 느려진다.
 */
@Service
class AdminRoles(
    private val jdbc: JdbcTemplate,
    private val audit: AuditLog,
    private val accounts: AdminAccounts,
    private val properties: AdminProperties,
) : GrantedRoles {

    /**
     * 이 계정이 가진 역할.
     *
     * 아무도 역할을 갖지 않은 상태에서 [AdminProperties.bootstrapEmail] 의 주인이
     * 호출하면 그 자리에서 SECURITY_ADMIN 을 준다 — 아래 [bootstrap] 참고.
     */
    @Transactional
    override fun of(userId: String): Set<AdminRole> {
        val roles = read(userId)
        if (roles.isNotEmpty()) return roles
        return bootstrap(userId)
    }

    /**
     * 첫 역할을 만드는 유일한 길 (§11.2).
     *
     * 역할을 줄 수 있는 사람이 아무도 없으면 아무도 역할을 받을 수 없다. 그 매듭을
     * 푸는 것이 여기다: **역할 표가 완전히 비어 있을 때만**, 설정에 적힌 이메일의
     * 주인에게 SECURITY_ADMIN 하나를 준다.
     *
     * 설정에 담기는 것은 이메일이지 비밀이 아니다. 프로세스 목록에서 읽어도 그것만으로는
     * 아무것도 못 한다 — 그 계정의 비밀번호가 따로 있어야 한다. 옛 `ADMIN_OPERATORS`
     * 토큰은 읽는 순간 곧바로 관리자였다.
     *
     * 한 번 성공하면 표가 비지 않으므로 다시 열리지 않는다.
     */
    private fun bootstrap(userId: String): Set<AdminRole> {
        val email = properties.bootstrapEmail.takeIf { it.isNotBlank() } ?: return emptySet()
        if (count() > 0) return emptySet()
        if (accounts.findIdByEmail(email) != userId) return emptySet()

        grantNow(userId, AdminRole.SECURITY_ADMIN, BOOTSTRAP_ACTOR)
        return setOf(AdminRole.SECURITY_ADMIN)
    }

    /**
     * 역할 부여를 요청한다. **이 시점에는 아무 권한도 늘지 않는다.**
     *
     * 예외 하나는 부트스트랩 구간이다 ([soloGrant]). SECURITY_ADMIN 이 한 명뿐이면
     * 승인해 줄 사람이 없으므로, 그 한 명은 SECURITY_ADMIN 만 혼자 줄 수 있다.
     */
    @Transactional
    fun requestGrant(userId: String, role: AdminRole, actor: String, reason: String): RoleOutcome {
        require(reason.isNotBlank()) { "역할 부여에는 사유가 필요하다 (§11.2)" }
        selfGrant(userId, actor)?.let { return it }
        if (role in read(userId)) return RoleOutcome.Unchanged

        if (soloGrant(role, holdersOf(AdminRole.SECURITY_ADMIN))) {
            return grantNow(userId, role, actor, detail = mapOf("reason" to reason, "solo" to true))
        }

        pending(userId, role)?.let {
            // 대기 중인 같은 요청이 이미 있다. 새로 만들면 승인해야 할 건이 둘이 되고,
            // 하나를 반려해도 다른 하나가 살아 있다.
            return RoleOutcome.Requested(it.id)
        }

        val id = UUID.randomUUID()
        jdbc.update(
            """
            INSERT INTO role_grant_request (id, user_id, role, reason, status, requested_by)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            id, UUID.fromString(userId), role.name, reason,
            GrantStatus.REQUESTED.name, actor,
        )
        audit.record(
            AuditAction.ADMIN_ROLE_GRANT_REQUESTED,
            subject = userId,
            actor = actor,
            detail = mapOf("role" to role.name, "reason" to reason, "requestId" to id.toString()),
        )
        return RoleOutcome.Requested(id)
    }

    /**
     * 요청을 승인하고 그 자리에서 역할을 준다.
     *
     * 승인이 곧 부여다. 재채점처럼 승인과 실행을 또 나누지 않는 이유는, 역할 부여에는
     * "승인했지만 아직 돌지 않은" 구간이 뜻하는 바가 없기 때문이다.
     */
    @Transactional
    fun approveGrant(requestId: UUID, approver: String): RoleOutcome {
        val request = findRequest(requestId) ?: return RoleOutcome.Refused("없는 요청이다")
        if (request.status != GrantStatus.REQUESTED) {
            return RoleOutcome.Refused("이미 ${request.status} 인 요청이다")
        }
        approvalConflict(request.userId, request.requestedBy, approver)?.let { return it }

        decide(requestId, GrantStatus.APPROVED, approver)
        return grantNow(
            request.userId, request.role, approver,
            detail = mapOf(
                "reason" to request.reason,
                "requestId" to requestId.toString(),
                "requestedBy" to request.requestedBy,
            ),
        )
    }

    @Transactional
    fun rejectGrant(requestId: UUID, actor: String, reason: String): RoleOutcome {
        val request = findRequest(requestId) ?: return RoleOutcome.Refused("없는 요청이다")
        if (request.status != GrantStatus.REQUESTED) {
            return RoleOutcome.Refused("이미 ${request.status} 인 요청이다")
        }
        // 반려에는 승인자 제약을 걸지 않는다. 요청을 거둬들이는 것은 권한을 늘리지
        // 않으므로, 요청자 자신이 물릴 수 있어야 한다.
        jdbc.update(
            """
            UPDATE role_grant_request SET status = ?, decided_at = now()
             WHERE id = ? AND status = ?
            """.trimIndent(),
            GrantStatus.REJECTED.name, requestId, GrantStatus.REQUESTED.name,
        )
        audit.record(
            AuditAction.ADMIN_ROLE_GRANT_REJECTED,
            subject = request.userId,
            actor = actor,
            detail = mapOf("role" to request.role.name, "reason" to reason, "requestId" to requestId.toString()),
        )
        return RoleOutcome.Changed
    }

    /** 아직 승인도 반려도 되지 않은 요청. 승인자가 볼 목록이다. */
    fun pendingGrants(): List<GrantRequest> = jdbc.query(
        "SELECT * FROM role_grant_request WHERE status = ? ORDER BY created_at",
        REQUEST_MAPPER, GrantStatus.REQUESTED.name,
    )

    @Transactional
    fun revoke(userId: String, role: AdminRole, actor: String): RoleOutcome {
        lastSecurityAdmin(role, holdersOf(AdminRole.SECURITY_ADMIN))?.let { return it }

        val changed = jdbc.update(
            "DELETE FROM admin_role WHERE user_id = ? AND role = ?",
            UUID.fromString(userId), role.name,
        )
        if (changed == 0) return RoleOutcome.Unchanged

        audit.record(
            AuditAction.ADMIN_ROLE_REVOKED,
            subject = userId,
            actor = actor,
            detail = mapOf("role" to role.name),
        )
        return RoleOutcome.Changed
    }

    /**
     * 실제로 역할 행을 넣는 유일한 자리.
     *
     * 부트스트랩과 승인만 여기로 들어온다. 부여 경로를 하나로 모아 두어야, 승인을
     * 건너뛰는 길이 새로 생겼는지 이 함수를 부르는 곳만 보면 알 수 있다.
     */
    private fun grantNow(
        userId: String,
        role: AdminRole,
        actor: String,
        detail: Map<String, Any?> = emptyMap(),
    ): RoleOutcome {
        val changed = jdbc.update(
            """
            INSERT INTO admin_role (user_id, role, granted_by) VALUES (?, ?, ?)
            ON CONFLICT (user_id, role) DO NOTHING
            """.trimIndent(),
            UUID.fromString(userId), role.name, actor,
        )
        // 이미 있는 역할을 다시 주는 것은 아무 일도 아니다. 감사 로그에 남기면 실제로
        // 권한이 바뀐 순간을 찾기 어려워진다.
        if (changed == 0) return RoleOutcome.Unchanged

        audit.record(
            AuditAction.ADMIN_ROLE_GRANTED,
            subject = userId,
            actor = actor,
            detail = mapOf("role" to role.name) + detail,
        )
        return RoleOutcome.Changed
    }

    private fun decide(requestId: UUID, status: GrantStatus, actor: String) = jdbc.update(
        """
        UPDATE role_grant_request SET status = ?, decided_by = ?, decided_at = now()
         WHERE id = ? AND status = ?
        """.trimIndent(),
        status.name, actor, requestId, GrantStatus.REQUESTED.name,
    )

    private fun findRequest(id: UUID): GrantRequest? =
        jdbc.query("SELECT * FROM role_grant_request WHERE id = ?", REQUEST_MAPPER, id).firstOrNull()

    private fun pending(userId: String, role: AdminRole): GrantRequest? = jdbc.query(
        "SELECT * FROM role_grant_request WHERE user_id = ? AND role = ? AND status = ?",
        REQUEST_MAPPER, UUID.fromString(userId), role.name, GrantStatus.REQUESTED.name,
    ).firstOrNull()

    private fun holdersOf(role: AdminRole): Int = jdbc.queryForObject(
        "SELECT count(*) FROM admin_role WHERE role = ?", Int::class.java, role.name,
    ) ?: 0

    companion object {
        /**
         * 자기 자신에게는 역할을 줄 수 없다 (§11.2 2인 승인).
         *
         * 막지 않으면 SECURITY_ADMIN 한 명이 자기에게 CONTENT_EDITOR 와 PUBLISHER 를
         * 붙여 등록과 승인을 혼자 밟는다. 2인 승인이 등록자·승인자 비교로 서 있는데,
         * 역할을 스스로 늘릴 수 있으면 그 비교는 사람 수를 세지 못한다.
         */
        internal fun selfGrant(userId: String, actor: String): RoleOutcome? =
            if (userId == actor) {
                RoleOutcome.Refused(
                    "자기 자신에게는 역할을 줄 수 없다. 다른 SECURITY_ADMIN 이 줘야 한다 (§11.2)",
                )
            } else {
                null
            }

        /**
         * 승인자가 될 수 없는 경우 (§11.2).
         *
         * 요청자와 같으면 2인 승인이 아니다. **역할을 받는 사람과 같아도 안 된다** —
         * 같아도 되면 SECURITY_ADMIN 둘이 서로에게 주면서 각자 권한을 늘릴 수 있고,
         * 그러면 권한을 키우는 데 필요한 사람 수가 다시 둘로 돌아간다.
         */
        internal fun approvalConflict(
            userId: String,
            requestedBy: String,
            approver: String,
        ): RoleOutcome? = when (approver) {
            requestedBy -> RoleOutcome.Refused(
                "요청자와 승인자가 같다. 역할 부여는 두 사람이 필요하다 (§11.2)",
            )

            userId -> RoleOutcome.Refused(
                "역할을 받는 사람은 자기 승격을 승인할 수 없다. 제3의 SECURITY_ADMIN 이 승인해야 한다 (§11.2)",
            )

            else -> null
        }

        /**
         * 승인 없이 혼자 줄 수 있는가 — 부트스트랩 구간에서만 참이다.
         *
         * SECURITY_ADMIN 이 한 명뿐이면 승인해 줄 사람이 없어 어떤 역할도 만들 수
         * 없다. 그 한 명에게 **동료를 만드는 것 하나만** 허용한다. 다른 역할까지
         * 열어 두면 혼자서 부하 계정에 PUBLISHER 를 붙일 수 있고, 그러면 이 변경이
         * 막으려던 것이 그대로 남는다.
         *
         * 두 번째 SECURITY_ADMIN 이 생기는 순간 이 길은 저절로 닫힌다.
         */
        internal fun soloGrant(role: AdminRole, securityAdmins: Int): Boolean =
            role == AdminRole.SECURITY_ADMIN && securityAdmins <= 1

        /**
         * 마지막 SECURITY_ADMIN 은 회수할 수 없다.
         *
         * 회수하면 역할을 줄 수 있는 사람이 없어진다. 그런데 부트스트랩은 역할 표가
         * **완전히** 비어야 열리므로, 다른 역할이 하나라도 남아 있으면 되살릴 길이
         * 없다 — DB 를 직접 건드리는 수밖에 없는 상태가 된다.
         */
        internal fun lastSecurityAdmin(role: AdminRole, holders: Int): RoleOutcome? =
            if (role == AdminRole.SECURITY_ADMIN && holders <= 1) {
                RoleOutcome.Refused(
                    "마지막 SECURITY_ADMIN 은 회수할 수 없다. 회수하면 역할을 줄 수 있는 " +
                        "사람이 없어지고, 부트스트랩은 역할 표가 완전히 비어야 열린다",
                )
            } else {
                null
            }

        private const val BOOTSTRAP_ACTOR = "bootstrap"

        private val REQUEST_MAPPER = RowMapper { rs, _ ->
            GrantRequest(
                id = rs.getObject("id", UUID::class.java),
                userId = rs.getString("user_id"),
                role = AdminRole.valueOf(rs.getString("role")),
                reason = rs.getString("reason"),
                status = GrantStatus.valueOf(rs.getString("status")),
                requestedBy = rs.getString("requested_by"),
                decidedBy = rs.getString("decided_by"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            )
        }
    }

    /** 부여 현황. 누가 무엇을 할 수 있는지는 SECURITY_ADMIN 이 볼 수 있어야 한다. */
    fun grants(): List<RoleGrant> = jdbc.query(
        """
        SELECT user_id, role, granted_by, granted_at
          FROM admin_role ORDER BY granted_at, user_id, role
        """.trimIndent(),
    ) { rs, _ ->
        RoleGrant(
            userId = rs.getString("user_id"),
            role = rs.getString("role"),
            grantedBy = rs.getString("granted_by"),
            grantedAt = rs.getTimestamp("granted_at").toInstant(),
        )
    }

    private fun read(userId: String): Set<AdminRole> = jdbc.query(
        "SELECT role FROM admin_role WHERE user_id = ?",
        { rs, _ -> rs.getString("role") },
        UUID.fromString(userId),
    ).mapNotNull { name -> runCatching { AdminRole.valueOf(name) }.getOrNull() }.toSet()

    private fun count(): Int =
        jdbc.queryForObject("SELECT count(*) FROM admin_role", Int::class.java) ?: 0
}

/** 역할 변경의 결과. "안 바뀌었다"와 "거절했다"는 부르는 쪽에서 갈라야 한다. */
sealed interface RoleOutcome {
    data object Changed : RoleOutcome

    /** 이미 그 상태였다. 오류가 아니다 — 시딩은 여러 번 돌아도 같아야 한다. */
    data object Unchanged : RoleOutcome

    /**
     * 접수만 됐다. **아직 권한은 늘지 않았다** (§11.2).
     *
     * [Changed] 와 갈라 두어야 한다. 부르는 쪽이 둘을 같게 다루면 "줬다"고 표시해 놓고
     * 실제로는 아무 권한도 없는 상태가 되고, 그 차이는 그 계정이 처음 거부당할 때에야
     * 드러난다.
     */
    data class Requested(val requestId: UUID) : RoleOutcome

    data class Refused(val reason: String) : RoleOutcome
}

data class RoleGrant(
    val userId: String,
    val role: String,
    val grantedBy: String,
    val grantedAt: Instant,
)

/** 역할 부여 요청 (§11.2). */
data class GrantRequest(
    val id: UUID,
    val userId: String,
    val role: AdminRole,
    val reason: String,
    val status: GrantStatus,
    val requestedBy: String,
    val decidedBy: String?,
    val createdAt: Instant,
)

enum class GrantStatus { REQUESTED, APPROVED, REJECTED }

/**
 * 계정 조회 포트 (§3.1 모듈 경계).
 *
 * Admin 은 Identity 를 참조하지 않는다. 부트스트랩에 이메일 → 계정 id 하나가 필요할
 * 뿐이라, 그 한 줄만 포트로 열고 어댑터는 `:control-plane:app` 에 둔다.
 */
fun interface AdminAccounts {
    /** 없으면 null. 존재 여부를 응답으로 흘리지 않도록 부르는 쪽이 조심한다 (§11.1). */
    fun findIdByEmail(email: String): String?
}
