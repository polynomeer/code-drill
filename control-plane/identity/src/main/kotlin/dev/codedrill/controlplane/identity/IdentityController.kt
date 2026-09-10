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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
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

    /**
     * 내 데이터를 전부 내려받는다 (§11.3).
     *
     * 지우기 전에 받아 갈 수 있어야 한다. 지우고 나면 돌려줄 것이 없다.
     */
    @GetMapping("/me/export")
    fun export(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
    ): ResponseEntity<Any> =
        identity.exportAccount(principal.id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.status(HttpStatus.NOT_FOUND).build()

    /**
     * 계정을 지운다 (§11.3).
     *
     * 되돌릴 수 없으므로 비밀번호를 다시 받는다. 자리를 비운 사이 남이 만졌을 때 막을
     * 것이 세션 하나뿐이면 안 된다.
     */
    @DeleteMapping("/me")
    fun deleteAccount(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @Valid @RequestBody request: DeleteAccountRequest,
    ): ResponseEntity<Any> =
        when (val outcome = identity.deleteAccount(principal.id, request.password)) {
            is IdentityService.Deletion.Done -> ResponseEntity.ok(mapOf("erased" to outcome.erased))

            IdentityService.Deletion.WrongPassword ->
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    error(ErrorCode.UNAUTHENTICATED, "비밀번호가 맞지 않는다"),
                )

            IdentityService.Deletion.NotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }

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

    /**
     * 표시 이름 변경 (기획서 부록 A 계정 도메인).
     *
     * 이메일은 여기서 바꾸지 않는다 — 이유는 [IdentityService.rename] 에 있다.
     */
    @PatchMapping("/me")
    fun rename(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @Valid @RequestBody request: RenameRequest,
    ): ResponseEntity<Any> =
        if (identity.rename(principal.id, request.displayName)) {
            ResponseEntity.ok(mapOf("id" to principal.id, "displayName" to request.displayName.trim()))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error(ErrorCode.UNAUTHENTICATED, "계정을 찾지 못했다"))
        }

    /**
     * 비밀번호 변경.
     *
     * 성공하면 **새 세션을 돌려준다.** 열린 세션을 전부 끊기 때문이며, 클라이언트는 받은
     * 세션으로 갈아 끼우면 된다.
     */
    @PostMapping("/me/password")
    fun changePassword(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @Valid @RequestBody request: ChangePasswordRequest,
    ): ResponseEntity<Any> =
        when (val outcome = identity.changePassword(principal.id, request.currentPassword, request.newPassword)) {
            is IdentityService.PasswordChange.Done ->
                ResponseEntity.ok(
                    mapOf(
                        "session" to SessionResponse.of(outcome.session),
                        // 몇 개가 끊겼는지 말해 준다. "다른 기기에서도 로그아웃됐다"를
                        // 사용자가 알아야 그 뒤에 일어나는 일이 놀랍지 않다.
                        "revokedSessions" to outcome.revoked,
                    ),
                )

            IdentityService.PasswordChange.WrongPassword ->
                ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(error(ErrorCode.UNAUTHENTICATED, "지금 비밀번호가 다르다"))

            IdentityService.PasswordChange.NotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(error(ErrorCode.UNAUTHENTICATED, "계정을 찾지 못했다"))
        }

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

data class DeleteAccountRequest(@field:NotBlank val password: String)

data class RenameRequest(@field:NotBlank val displayName: String)

data class ChangePasswordRequest(
    @field:NotBlank val currentPassword: String,
    @field:NotBlank val newPassword: String,
)

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
