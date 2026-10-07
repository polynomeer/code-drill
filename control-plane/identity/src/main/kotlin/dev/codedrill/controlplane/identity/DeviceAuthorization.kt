package dev.codedrill.controlplane.identity

import java.security.SecureRandom
import java.time.Duration
import java.time.Instant

/**
 * 기기 승인의 규칙 (RFC 8628 Device Authorization Grant — feature-roadmap 11단계 이어서, 2단계).
 *
 * CLI 는 비밀번호를 받지 않는다. 기기 코드를 받아 두고, 사람은 브라우저에서 짧은 사용자 코드를 맞춰 승인한다.
 * 승인되면 CLI 가 기기 세션을 받는다. 여기 있는 것은 저장소 없이 시험할 수 있는 판단뿐이다.
 */
object DeviceAuthorization {

    /**
     * 사용자 코드의 글자. 헷갈리는 글자(모음, 0/O, 1/I/L)를 뺀 자음 스물 — RFC 8628 §6.1 의 권고다. 모음이 없으면
     * 우연히 낱말이 되지도 않는다. 여덟 글자면 20^8 ≈ 2.6×10^10 이라 10분 안에 맞힐 수 없다.
     */
    private const val ALPHABET = "BCDFGHJKLMNPQRSTVWXZ"
    private val random = SecureRandom()

    /** `WQXR-7KDP` 꼴이 아니라 `WQXR-KDPB` 꼴 — 숫자는 0/O 를 헷갈리게 해 쓰지 않는다 */
    fun userCode(): String = buildString {
        repeat(8) { index ->
            if (index == 4) append('-')
            append(ALPHABET[random.nextInt(ALPHABET.length)])
        }
    }

    /** 사람이 친 코드를 맞춘다 — 소문자·공백·빠진 하이픈을 받아 준다 */
    fun normalize(raw: String): String? {
        val letters = raw.uppercase().filter { it.isLetter() }
        if (letters.length != 8 || letters.any { it !in ALPHABET }) return null
        return "${letters.take(4)}-${letters.drop(4)}"
    }

    /** CLI 가 물었을 때 무엇을 답하나. [Issue] 일 때만 세션을 낸다 */
    sealed interface Answer {
        data object Pending : Answer
        data object SlowDown : Answer
        data object Denied : Answer
        data object Expired : Answer
        data object Issue : Answer
    }

    /**
     * 물음에 대한 답. 만료가 먼저다 — 승인된 뒤라도 시한을 넘겨 처음 물으면 받지 못한다. 이미 쓴 코드도 만료와 같다
     * (다시 물어 세션을 하나 더 받을 수 없다). 간격보다 빨리 물으면 늦추라고 한다 (RFC 8628 §3.5 slow_down).
     */
    fun answer(status: String, expiresAt: Instant, lastPolledAt: Instant?, now: Instant, interval: Duration): Answer = when {
        status == "CONSUMED" -> Answer.Expired
        !now.isBefore(expiresAt) -> Answer.Expired
        status == "DENIED" -> Answer.Denied
        status == "APPROVED" -> Answer.Issue
        lastPolledAt != null && now.isBefore(lastPolledAt.plus(interval).minusMillis(SLACK_MILLIS)) -> Answer.SlowDown
        else -> Answer.Pending
    }

    /** 네트워크가 흔들려 조금 일찍 온 것까지 나무라지 않는다 */
    private const val SLACK_MILLIS = 500L

    /**
     * 기기 세션이 열 수 있는 경로. **목록 밖은 전부 막는다** — 새 API 를 만든 사람이 기기를 잊어도 열리지 않게 한다.
     *
     * 프로젝트형 받기·제출·초안·기록, 그리고 "나는 누구인가"(CLI 가 연결을 보여 준다)와 로그아웃(연결을 스스로
     * 끊는다). 비밀번호 바꾸기·계정 삭제·다른 기기 승인은 기기 세션으로 못 한다.
     */
    fun allows(method: String, path: String): Boolean = when {
        path == "/api/v1/projects" || path.startsWith("/api/v1/projects/") -> true
        path == "/api/v1/auth/me" && method == "GET" -> true
        path == "/api/v1/auth/logout" && method == "POST" -> true
        else -> false
    }
}
