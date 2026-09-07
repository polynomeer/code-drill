package dev.codedrill.controlplane.identity

import dev.codedrill.platform.common.ApiError
import dev.codedrill.platform.common.ErrorCode
import dev.codedrill.platform.common.Principal
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

/**
 * 인증 API (기술 설계서 §9.2, §11.2).
 *
 * 응답에 비밀번호도, 토큰 해시도, 다른 사용자의 존재 여부도 담지 않는다.
 */
@RestController
@RequestMapping("/api/v1/auth")
class IdentityController(private val identity: IdentityService) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<Any> =
        when (val outcome = identity.register(request.email, request.displayName, request.password)) {
            is IdentityService.Registration.Created ->
                ResponseEntity.status(HttpStatus.CREATED).body(SessionResponse.of(outcome.session))

            IdentityService.Registration.EmailTaken ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(
                    error(ErrorCode.UNAUTHENTICATED, "이미 쓰이고 있는 이메일이다"),
                )
        }

    /**
     * 로그인.
     *
     * 실패 사유를 나누지 않는다. "없는 계정"과 "틀린 비밀번호"를 구분해 알려 주면,
     * 그것만으로 어떤 이메일이 가입돼 있는지 확인할 수 있다 (§11.1).
     */
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<Any> =
        identity.login(request.email, request.password)
            ?.let { ResponseEntity.ok(SessionResponse.of(it)) }
            ?: ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                error(ErrorCode.UNAUTHENTICATED, "이메일 또는 비밀번호가 맞지 않는다"),
            )

    /** 갱신. 쓰는 순간 이전 refresh 는 무효가 된다 (§11.2 회전). */
    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): ResponseEntity<Any> =
        identity.refresh(request.refreshToken)
            ?.let { ResponseEntity.ok(SessionResponse.of(it)) }
            ?: ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                error(ErrorCode.UNAUTHENTICATED, "쓸 수 없는 refresh token 이다. 다시 로그인해야 한다"),
            )

    @PostMapping("/logout")
    fun logout(@RequestHeader("Authorization") authorization: String): ResponseEntity<Void> {
        identity.logout(authorization.removePrefix("Bearer ").trim())
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/me")
    fun me(@RequestAttribute(Principal.ATTRIBUTE) principal: Principal) = mapOf(
        "id" to principal.id,
        "displayName" to principal.displayName,
    )

    @ExceptionHandler(IllegalArgumentException::class)
    fun onInvalid(e: IllegalArgumentException): ResponseEntity<ApiError> =
        ResponseEntity.badRequest().body(
            error(ErrorCode.INVALID_SIGNATURE, e.message ?: "요청이 유효하지 않다"),
        )

    private fun error(code: ErrorCode, message: String) =
        ApiError(code, message, UUID.randomUUID().toString())
}

data class RegisterRequest(
    @field:Email @field:NotBlank val email: String,
    val displayName: String = "",
    @field:NotBlank val password: String,
)

data class LoginRequest(
    @field:NotBlank val email: String,
    @field:NotBlank val password: String,
)

data class RefreshRequest(@field:NotBlank val refreshToken: String)

/** 발급 응답. 평문 토큰이 나가는 유일한 곳이다. */
data class SessionResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAt: Instant,
    val refreshExpiresAt: Instant,
    val userId: String,
    val displayName: String,
) {
    companion object {
        fun of(session: IssuedSession) = SessionResponse(
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            accessExpiresAt = session.accessExpiresAt,
            refreshExpiresAt = session.refreshExpiresAt,
            userId = session.user.id.toString(),
            displayName = session.user.displayName,
        )
    }
}
