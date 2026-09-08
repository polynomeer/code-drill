package dev.codedrill.controlplane.admin

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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

        grant(userId, AdminRole.SECURITY_ADMIN, BOOTSTRAP_ACTOR)
        return setOf(AdminRole.SECURITY_ADMIN)
    }

    @Transactional
    fun grant(userId: String, role: AdminRole, actor: String): Boolean {
        val changed = jdbc.update(
            """
            INSERT INTO admin_role (user_id, role, granted_by) VALUES (?, ?, ?)
            ON CONFLICT (user_id, role) DO NOTHING
            """.trimIndent(),
            UUID.fromString(userId), role.name, actor,
        )
        // 이미 있는 역할을 다시 주는 것은 아무 일도 아니다. 감사 로그에 남기면 실제로
        // 권한이 바뀐 순간을 찾기 어려워진다.
        if (changed == 0) return false

        audit.record(
            AuditAction.ADMIN_ROLE_GRANTED,
            subject = userId,
            actor = actor,
            detail = mapOf("role" to role.name),
        )
        return true
    }

    @Transactional
    fun revoke(userId: String, role: AdminRole, actor: String): Boolean {
        val changed = jdbc.update(
            "DELETE FROM admin_role WHERE user_id = ? AND role = ?",
            UUID.fromString(userId), role.name,
        )
        if (changed == 0) return false

        audit.record(
            AuditAction.ADMIN_ROLE_REVOKED,
            subject = userId,
            actor = actor,
            detail = mapOf("role" to role.name),
        )
        return true
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

    private companion object {
        const val BOOTSTRAP_ACTOR = "bootstrap"
    }
}

data class RoleGrant(
    val userId: String,
    val role: String,
    val grantedBy: String,
    val grantedAt: java.time.Instant,
)

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
