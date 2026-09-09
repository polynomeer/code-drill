package dev.codedrill.controlplane.identity

import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * 사용자와 세션 저장소 (기술 설계서 §11.2).
 *
 * 토큰은 해시로만 오간다. 이 클래스의 어떤 메서드도 평문 토큰을 받지 않는 것이
 * 의도다 — 해싱을 잊은 호출부가 있으면 컴파일이 아니라 조회 실패로 드러나야 하는데,
 * 그것도 늦다. 해싱은 [IdentityService] 한 곳에서만 한다.
 */
@Repository
class IdentityRepository(private val jdbc: JdbcTemplate) {

    /** 이미 있는 이메일이면 null. 어느 쪽이 먼저인지는 DB 제약이 정한다. */
    fun insertUser(id: UUID, email: String, displayName: String, passwordHash: String): User? =
        try {
            jdbc.update(
                """
                INSERT INTO app_user (id, email, display_name, password_hash)
                VALUES (?, ?, ?, ?)
                """.trimIndent(),
                id, email, displayName, passwordHash,
            )
            findById(id)
        } catch (e: DuplicateKeyException) {
            null
        }

    fun findById(id: UUID): User? =
        jdbc.query("SELECT * FROM app_user WHERE id = ?", USER, id).firstOrNull()

    /** 로그인용. 해시를 함께 돌려주는 유일한 경로다. */
    /**
     * 로그인용 자격.
     *
     * 지운 계정은 돌려주지 않는다. 무덤값 해시로는 어차피 맞출 수 없지만, 그 판단을
     * 비밀번호 비교에 맡기면 "왜 안 되는지"가 코드에 남지 않는다 (§11.3).
     */
    fun findCredentials(email: String): Pair<User, String>? = jdbc.query(
        "SELECT * FROM app_user WHERE email = ? AND deleted_at IS NULL",
        { rs, _ -> USER.mapRow(rs, 0)!! to rs.getString("password_hash") },
        email,
    ).firstOrNull()

    fun insertSession(
        id: UUID,
        userId: UUID,
        accessHash: String,
        refreshHash: String,
        accessExpiresAt: Instant,
        refreshExpiresAt: Instant,
    ) {
        jdbc.update(
            """
            INSERT INTO user_session (
                id, user_id, access_token_hash, refresh_token_hash,
                access_expires_at, refresh_expires_at
            ) VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            id, userId, accessHash, refreshHash,
            java.sql.Timestamp.from(accessExpiresAt), java.sql.Timestamp.from(refreshExpiresAt),
        )
    }

    fun findByAccessHash(hash: String): Session? = jdbc.query(
        "SELECT * FROM user_session WHERE access_token_hash = ?", SESSION, hash,
    ).firstOrNull()

    fun findByRefreshHash(hash: String): Session? = jdbc.query(
        "SELECT * FROM user_session WHERE refresh_token_hash = ?", SESSION, hash,
    ).firstOrNull()

    /** 끊는다. 행을 지우지 않아 "언제 왜 끊겼는지"가 남는다. */
    fun revoke(id: UUID, reason: String): Int = jdbc.update(
        "UPDATE user_session SET revoked_at = now(), revoked_reason = ? WHERE id = ? AND revoked_at IS NULL",
        reason, id,
    )

    /** 한 사용자의 살아 있는 세션 전부. refresh 재사용이 탐지되면 통째로 끊는다. */
    /**
     * 계정에서 사람을 식별하는 값을 지운다 (§11.3).
     *
     * 이미 지운 계정은 건드리지 않는다 — 두 번째 요청이 무덤값을 또 덮어쓰면 삭제 시각이
     * 뒤로 밀린다.
     */
    fun anonymize(id: UUID, tombstoneEmail: String): Int = jdbc.update(
        """
        UPDATE app_user
           SET email = ?, display_name = '탈퇴한 사용자',
               -- BCrypt 형식이 아니라 어떤 비밀번호로도 맞출 수 없다.
               password_hash = 'deleted', deleted_at = now()
         WHERE id = ? AND deleted_at IS NULL
        """.trimIndent(),
        tombstoneEmail, id,
    )

    fun revokeAllFor(userId: UUID, reason: String): Int = jdbc.update(
        "UPDATE user_session SET revoked_at = now(), revoked_reason = ? WHERE user_id = ? AND revoked_at IS NULL",
        reason, userId,
    )

    data class Session(
        val id: UUID,
        val userId: UUID,
        val accessExpiresAt: Instant,
        val refreshExpiresAt: Instant,
        val revokedAt: Instant?,
    )

    private companion object {
        val USER = RowMapper { rs, _ ->
            User(
                id = rs.getObject("id", UUID::class.java),
                email = rs.getString("email"),
                displayName = rs.getString("display_name"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            )
        }

        val SESSION = RowMapper { rs, _ ->
            Session(
                id = rs.getObject("id", UUID::class.java),
                userId = rs.getObject("user_id", UUID::class.java),
                accessExpiresAt = rs.getTimestamp("access_expires_at").toInstant(),
                refreshExpiresAt = rs.getTimestamp("refresh_expires_at").toInstant(),
                revokedAt = rs.getTimestamp("revoked_at")?.toInstant(),
            )
        }
    }
}
