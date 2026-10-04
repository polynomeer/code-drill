package dev.codedrill.controlplane.analytics

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Clock
import java.time.Duration

/**
 * UX 이벤트를 받는다. 계정 id 는 받지 않는다 — 탭마다 바뀌는 세션 id 와 "로그인했나" 하나만 (§16.2 개인 식별
 * 최소화). [RETENTION] 이 지난 줄은 받을 때마다 조금씩 지운다.
 */
@Service
class EventService(
    private val jdbc: JdbcTemplate,
    private val json: ObjectMapper,
    private val clock: Clock = Clock.systemUTC(),
) {

    data class Incoming(val name: String, val props: Map<String, Any?> = emptyMap())

    data class Result(val accepted: Int, val dropped: Int)

    fun record(session: String, signedIn: Boolean, uiVersion: String, events: List<Incoming>): Result {
        val sessionId = session.takeIf { SESSION.matches(it) } ?: return Result(0, events.size)
        val version = uiVersion.take(EventSchema.MAX_STRING)
        val now = clock.instant()
        var accepted = 0
        for (event in events.take(EventSchema.MAX_BATCH)) {
            val props = EventSchema.clean(event.name, event.props) ?: continue
            jdbc.update(
                "INSERT INTO ux_event (name, props, session_id, signed_in, ui_version, created_at) VALUES (?, ?::jsonb, ?, ?, ?, ?)",
                event.name, json.writeValueAsString(props), sessionId, signedIn, version, Timestamp.from(now),
            )
            accepted++
        }
        jdbc.update("DELETE FROM ux_event WHERE created_at < ?", Timestamp.from(now.minus(RETENTION)))
        return Result(accepted, events.size - accepted)
    }

    private companion object {
        val RETENTION: Duration = Duration.ofDays(90)
        val SESSION = Regex("^[A-Za-z0-9-]{8,64}$")
    }
}
