package dev.codedrill.controlplane.notification

import java.time.Instant

/**
 * 알림 하나.
 *
 * **저장하지 않고 사실에서 만든다.** 알림 표를 따로 두면 사실과 알림이 갈라진다 — 글이 내려졌는데 "답이
 * 달렸다"가 남거나, 대회가 취소됐는데 "곧 시작"이 남는다. 사실에서 매번 만들면 사실이 바뀌면 알림도 바뀐다.
 *
 * [id] 는 사실에서 정해진다(같은 사실이면 같은 id) — 화면이 목록을 다시 그려도 줄이 흔들리지 않는다.
 * [at] 은 알릴 만해진 때다. 대회 시작은 시작 하루 전부터, 나머지는 그 일이 일어난 때.
 */
data class Notification(
    val id: String,
    val kind: Kind,
    val title: String,
    val body: String?,
    /** 눌렀을 때 갈 웹 주소 */
    val link: String,
    val at: Instant,
) {
    enum class Kind { CONTEST_STARTING, RATING_CHANGED, ANSWERED, HELPFUL, TRANSFER_DONE, SANCTION, APPEAL_RESOLVED }
}

data class Feed(val items: List<FeedItem>, val unread: Int)

data class FeedItem(
    val id: String,
    val kind: Notification.Kind,
    val title: String,
    val body: String?,
    val link: String,
    val at: Instant,
    val unread: Boolean,
)

/**
 * 알림이 기대는 사실들 (§3.1 조립 지점). [since] 이후 알릴 만해진 것, [now] 를 넘지 않는 것.
 */
fun interface NotificationSources {
    fun since(userId: String, since: Instant, now: Instant): List<Notification>
}
