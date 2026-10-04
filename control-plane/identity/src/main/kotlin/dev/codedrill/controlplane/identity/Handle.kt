package dev.codedrill.controlplane.identity

/**
 * 공개 프로필 주소의 이름 (`/u/{handle}`).
 *
 * 소문자·숫자·`-`·`_` 로 3~20자, 첫 글자는 영문이나 숫자. 주소에 그대로 들어가므로 인코딩이 필요한
 * 글자를 받지 않는다. 화면 경로와 겹치는 이름(`me` 등)은 막는다 — `/u/me` 는 "내 프로필"이다.
 */
object Handle {
    private val PATTERN = Regex("^[a-z0-9][a-z0-9_-]{2,19}$")

    private val RESERVED = setOf("me", "admin", "settings", "new", "edit", "null", "undefined", "codedrill")

    /** 정규화한 핸들, 또는 왜 안 되는지. */
    fun parse(raw: String): Result {
        val handle = raw.trim().lowercase()
        return when {
            !PATTERN.matches(handle) -> Result.Invalid("핸들은 영문 소문자·숫자·-·_ 로 3~20자, 첫 글자는 영문이나 숫자다")
            handle in RESERVED -> Result.Invalid("쓸 수 없는 핸들이다: $handle")
            else -> Result.Valid(handle)
        }
    }

    sealed interface Result {
        data class Valid(val handle: String) : Result
        data class Invalid(val reason: String) : Result
    }
}
