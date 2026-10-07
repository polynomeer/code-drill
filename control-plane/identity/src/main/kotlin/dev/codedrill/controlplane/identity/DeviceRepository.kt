package dev.codedrill.controlplane.identity

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/** 기기 승인 요청 (V49). 기기 세션 자체는 [IdentityRepository] 의 user_session 이다. */
@Repository
class DeviceRepository(private val jdbc: JdbcTemplate) {

    data class Request(
        val id: UUID,
        val userCode: String,
        val deviceName: String,
        val client: String,
        val status: String,
        val userId: UUID?,
        val createdAt: Instant,
        val expiresAt: Instant,
        val lastPolledAt: Instant?,
    )

    /** 사용자 코드가 기다리는 요청과 겹치면 false — 부른 쪽이 새 코드로 다시 시도한다 */
    fun insert(id: UUID, deviceCodeHash: String, userCode: String, deviceName: String, client: String, origin: String, expiresAt: Instant): Boolean =
        jdbc.update(
            """
            INSERT INTO device_authorization (id, device_code_hash, user_code, device_name, client, origin, expires_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT DO NOTHING
            """.trimIndent(),
            id, deviceCodeHash, userCode, deviceName, client, origin, Timestamp.from(expiresAt),
        ) == 1

    /** 한 출처가 최근에 받아 간 코드 수 — 남용 방어 (§10.2) */
    fun recentFromOrigin(origin: String, since: Instant): Int =
        jdbc.queryForObject(
            "SELECT count(*) FROM device_authorization WHERE origin = ? AND created_at > ?",
            Int::class.java, origin, Timestamp.from(since),
        ) ?: 0

    fun findByDeviceCodeHash(hash: String): Request? =
        jdbc.query("SELECT * FROM device_authorization WHERE device_code_hash = ?", REQUEST, hash).firstOrNull()

    /** 기다리는 요청만 — 승인 화면이 보는 것 */
    fun findPending(userCode: String): Request? =
        jdbc.query("SELECT * FROM device_authorization WHERE user_code = ? AND status = 'PENDING'", REQUEST, userCode).firstOrNull()

    fun touchPoll(id: UUID, now: Instant): Int =
        jdbc.update("UPDATE device_authorization SET last_polled_at = ? WHERE id = ?", Timestamp.from(now), id)

    /** 승인·거절. 기다리는 중이고 만료 전일 때만 — 둘이 동시에 눌러도 한 번만 된다 */
    fun decide(userCode: String, userId: UUID, approve: Boolean, now: Instant): Int =
        jdbc.update(
            """
            UPDATE device_authorization
               SET status = ?, user_id = ?, decided_at = ?
             WHERE user_code = ? AND status = 'PENDING' AND expires_at > ?
            """.trimIndent(),
            if (approve) "APPROVED" else "DENIED", userId, Timestamp.from(now), userCode, Timestamp.from(now),
        )

    /** 승인된 것을 한 번만 세션으로 바꾼다 — 같은 기기 코드로 세션을 둘 받을 수 없다 */
    fun consume(id: UUID): Int =
        jdbc.update("UPDATE device_authorization SET status = 'CONSUMED' WHERE id = ? AND status = 'APPROVED'", id)

    /** 계정 삭제 (§11.3) — 그 사람이 결정한 요청에 남은 기기 이름까지 지운다 */
    fun eraseFor(userId: UUID): Int = jdbc.update("DELETE FROM device_authorization WHERE user_id = ?", userId)

    private companion object {
        val REQUEST = RowMapper { rs, _ ->
            Request(
                id = rs.getObject("id", UUID::class.java),
                userCode = rs.getString("user_code"),
                deviceName = rs.getString("device_name"),
                client = rs.getString("client"),
                status = rs.getString("status"),
                userId = rs.getObject("user_id", UUID::class.java),
                createdAt = rs.getTimestamp("created_at").toInstant(),
                expiresAt = rs.getTimestamp("expires_at").toInstant(),
                lastPolledAt = rs.getTimestamp("last_polled_at")?.toInstant(),
            )
        }
    }
}
