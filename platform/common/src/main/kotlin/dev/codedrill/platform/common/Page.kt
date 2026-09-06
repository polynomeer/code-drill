package dev.codedrill.platform.common

import java.util.Base64

/**
 * cursor 페이지네이션 (기술 설계서 §9.1).
 *
 * offset 을 쓰지 않는다. 목록을 넘기는 동안 새 항목이 끼어들면 offset 은 항목을 건너뛰거나
 * 중복해서 보여준다. 커서는 "마지막으로 본 항목"을 가리키므로 그 사이에 무엇이 추가돼도
 * 이어보기가 어긋나지 않는다.
 *
 * 정렬 키는 고정한다. 같은 요청이 같은 순서를 내지 않으면 커서 자체가 의미를 잃는다.
 */
data class Page<T>(
    val items: List<T>,
    /** 다음 페이지 커서. null 이면 마지막 페이지다. */
    val nextCursor: String? = null,
)

/**
 * 커서 인코딩.
 *
 * 정렬 키 값들을 이어 붙여 base64 로 감싼다. 불투명하게 보이는 것이 목적이며, 클라이언트가
 * 내용을 해석하거나 만들어내지 않게 한다. 서명은 하지 않으므로 **커서로 접근 권한을
 * 결정해서는 안 된다** — 권한은 항상 요청 주체로 다시 판단한다.
 */
object Cursor {

    /** 목록 API 가 한 번에 돌려줄 수 있는 최대 개수. */
    const val MAX_LIMIT = 100

    private const val DEFAULT_LIMIT = 20
    private const val SEPARATOR = "|"

    fun encode(vararg parts: String): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString(parts.joinToString(SEPARATOR).toByteArray())

    /** 해석할 수 없는 커서는 예외 대신 null 이다. 첫 페이지부터 보여주면 된다. */
    fun decode(cursor: String?): List<String>? {
        if (cursor.isNullOrBlank()) return null
        return runCatching {
            String(Base64.getUrlDecoder().decode(cursor)).split(SEPARATOR)
        }.getOrNull()
    }

    fun limitOf(requested: Int?): Int = (requested ?: DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT)
}
