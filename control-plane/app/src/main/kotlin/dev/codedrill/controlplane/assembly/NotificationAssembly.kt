package dev.codedrill.controlplane.assembly

import dev.codedrill.controlplane.identity.PersonalData
import dev.codedrill.controlplane.notification.Notification
import dev.codedrill.controlplane.notification.Notification.Kind
import dev.codedrill.controlplane.notification.NotificationRepository
import dev.codedrill.controlplane.notification.NotificationSources
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.Duration

/**
 * 알림이 기대는 사실들 (§3.1 조립 지점, docs/ui-overhaul.md §4 "알림").
 *
 * 대회(Contest)·게시판(Workspace)·전이(Coaching)·제재(Identity)의 표를 읽어 Notification 의 말로 옮긴다.
 * 판정 완료는 알리지 않는다 — 제출할 때마다 하나씩 쌓여 다른 알림을 묻고, 판정은 풀이 화면이 이미 실시간으로
 * 보여 준다. 알림은 **내가 보고 있지 않을 때 생긴 일**이다.
 */
@Configuration
class NotificationAssembly {

    @Bean
    fun notificationSources(jdbc: JdbcTemplate) = NotificationSources { userId, since, now ->
        val from = Timestamp.from(since)
        buildList {
            // 참가한 대회가 하루 안에 시작하거나 진행 중 — 시작 하루 전부터 알린다
            addAll(
                jdbc.query(
                    """
                    SELECT c.id, c.title, c.starts_at, c.ends_at, e.joined_at FROM contest c
                      JOIN contest_entry e ON e.contest_id = c.id AND e.user_id = ?
                     WHERE c.kind = 'CONTEST' AND c.starts_at IS NOT NULL AND c.ends_at > ? AND c.starts_at <= ?
                    """.trimIndent(),
                    { rs, _ ->
                        val starts = rs.getTimestamp("starts_at").toInstant()
                        val running = !starts.isAfter(now)
                        Notification(
                            id = "contest:${rs.getString("id")}",
                            kind = Kind.CONTEST_STARTING,
                            title = if (running) "${rs.getString("title")} — 진행 중입니다" else "${rs.getString("title")} — 곧 시작합니다",
                            body = null,
                            link = "/contests/${rs.getString("id")}",
                            at = maxOf(rs.getTimestamp("joined_at").toInstant(), starts.minus(Duration.ofDays(1))),
                        )
                    },
                    userId, Timestamp.from(now), Timestamp.from(now.plus(Duration.ofDays(1))),
                ),
            )
            // 레이팅이 적용됐다
            addAll(
                jdbc.query(
                    """
                    SELECT r.contest_id, c.title, r.rank, r.before, r.after, r.applied_at FROM rating_change r
                      JOIN contest c ON c.id = r.contest_id
                     WHERE r.user_id = ? AND r.applied_at >= ?
                    """.trimIndent(),
                    { rs, _ ->
                        val delta = rs.getInt("after") - rs.getInt("before")
                        Notification(
                            id = "rating:${rs.getString("contest_id")}",
                            kind = Kind.RATING_CHANGED,
                            title = "${rs.getString("title")} — ${rs.getInt("rank")}위, 레이팅 ${if (delta >= 0) "+$delta" else "$delta"}",
                            body = "${rs.getInt("before")} → ${rs.getInt("after")}",
                            link = "/contests/${rs.getString("contest_id")}",
                            at = rs.getTimestamp("applied_at").toInstant(),
                        )
                    },
                    userId, from,
                ),
            )
            // 내 질문에 남이 답했다 (내려진 답은 빼고)
            addAll(
                jdbc.query(
                    """
                    SELECT a.id, q.title, q.problem_id, a.created_at FROM discussion_post a
                      JOIN discussion_post q ON q.id = a.parent_id
                     WHERE q.author_id = ? AND a.kind = 'ANSWER' AND a.status = 'VISIBLE'
                       AND (a.author_id IS NULL OR a.author_id <> ?) AND a.created_at >= ?
                    """.trimIndent(),
                    { rs, _ ->
                        Notification(
                            id = "answer:${rs.getString("id")}",
                            kind = Kind.ANSWERED,
                            title = "내 질문에 답이 달렸습니다",
                            body = rs.getString("title"),
                            link = "/problems/${rs.getString("problem_id")}/solve",
                            at = rs.getTimestamp("created_at").toInstant(),
                        )
                    },
                    userId, userId, from,
                ),
            )
            // 내 글이 도움됐다 — 글마다 하나로 묶는다 (도움됐다마다 하나면 목록이 그것으로 덮인다)
            addAll(
                jdbc.query(
                    """
                    SELECT p.id, p.title, p.problem_id, count(*) AS n, max(h.created_at) AS last FROM discussion_helpful h
                      JOIN discussion_post p ON p.id = h.post_id
                     WHERE p.author_id = ? AND h.user_id <> ? AND h.created_at >= ? AND p.status = 'VISIBLE'
                     GROUP BY p.id, p.title, p.problem_id
                    """.trimIndent(),
                    { rs, _ ->
                        val last = rs.getTimestamp("last").toInstant()
                        Notification(
                            id = "helpful:${rs.getString("id")}:${last.epochSecond}",
                            kind = Kind.HELPFUL,
                            title = "내 글이 ${rs.getInt("n")}명에게 도움됐습니다",
                            body = rs.getString("title"),
                            link = "/problems/${rs.getString("problem_id")}/solve",
                            at = last,
                        )
                    },
                    userId, userId, from,
                ),
            )
            // 전이 과제가 끝났다
            addAll(
                jdbc.query(
                    "SELECT id, target_problem_id, status, completed_at FROM transfer_task WHERE user_id = ? AND completed_at >= ?",
                    { rs, _ ->
                        val verified = rs.getString("status") == "VERIFIED"
                        Notification(
                            id = "transfer:${rs.getString("id")}",
                            kind = Kind.TRANSFER_DONE,
                            title = if (verified) "전이가 확인됐습니다 — 가장 무거운 증거가 됐습니다" else "변형 문제를 풀었지만 전이 증거는 되지 않았습니다",
                            body = rs.getString("target_problem_id"),
                            link = "/competencies",
                            at = rs.getTimestamp("completed_at").toInstant(),
                        )
                    },
                    userId, from,
                ),
            )
            // 제재와 이의 판단 — 무엇이 막혔고 왜인지는 본인이 먼저 알아야 한다 (§8.5)
            jdbc.query(
                "SELECT id, kind, reason, created_at, appeal_resolution, resolved_at FROM sanction WHERE user_id = ? AND (created_at >= ? OR resolved_at >= ?)",
                { rs, _ ->
                    buildList {
                        val created = rs.getTimestamp("created_at").toInstant()
                        if (!created.isBefore(since)) {
                            add(Notification("sanction:${rs.getString("id")}", Kind.SANCTION, "제재를 받았습니다 — ${rs.getString("reason")}", null, "/me", created))
                        }
                        val resolved = rs.getTimestamp("resolved_at")?.toInstant()
                        if (resolved != null && !resolved.isBefore(since)) {
                            val lifted = rs.getString("appeal_resolution") == "LIFTED"
                            add(Notification("appeal:${rs.getString("id")}", Kind.APPEAL_RESOLVED, if (lifted) "이의가 받아들여져 제재가 풀렸습니다" else "이의를 검토했고 제재는 유지됩니다", null, "/me", resolved))
                        }
                    }
                },
                userId, from, from,
            ).forEach(::addAll)
        }
    }

    /** 읽음 표시도 사용자의 것이다 (§11.3). */
    @Bean
    fun notificationPersonalArea(repository: NotificationRepository) = object : PersonalData {
        override val area = "notifications"
        override fun export(userId: String) = mapOf("readUntil" to repository.readUntil(userId))
        override fun erase(userId: String) = mapOf("readUntil" to repository.erase(userId))
    }
}
