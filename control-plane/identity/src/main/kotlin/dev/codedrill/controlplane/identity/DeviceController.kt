package dev.codedrill.controlplane.identity

import dev.codedrill.platform.common.ApiError
import dev.codedrill.platform.common.ErrorCode
import dev.codedrill.platform.common.Principal
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

/**
 * 기기 승인 API (RFC 8628 — feature-roadmap 11단계 이어서, 2단계). 규칙은 [DeviceService].
 *
 * `device/code`·`device/token` 은 CLI 가 부르므로 토큰 없이 열린다(인증 인터셉터에서 뺐다). 승인과 연결된 기기는
 * 사람의 것이라 웹 세션으로만 — 기기 세션으로는 다른 기기를 승인하지 못한다 ([DeviceAuthorization.allows]).
 */
@RestController
@RequestMapping("/api/v1/auth")
class DeviceController(private val service: DeviceService, private val guard: AbuseGuard) {

    @PostMapping("/device/code")
    fun code(@Valid @RequestBody request: DeviceCodeRequest, http: HttpServletRequest): ResponseEntity<Any> =
        when (val outcome = service.start(request.deviceName, request.client, guard.originOf(http))) {
            DeviceService.StartOutcome.Throttled -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "3600")
                .body(error(ErrorCode.QUOTA_EXCEEDED, "이 곳에서 기기 코드를 너무 많이 받았다. 한 시간 뒤에 다시 시도한다"))
            is DeviceService.StartOutcome.Ok -> ResponseEntity.ok(
                DeviceCodeResponse(
                    deviceCode = outcome.started.deviceCode,
                    userCode = outcome.started.userCode,
                    verificationUri = "/device",
                    expiresAt = outcome.started.expiresAt,
                    interval = outcome.started.intervalSeconds,
                ),
            )
        }

    /**
     * CLI 가 승인을 기다리며 묻는다. 승인됐으면 세션(웹 로그인과 같은 모양), 아니면 400 과 이유 — RFC 8628 §3.5 의
     * authorization_pending·slow_down·access_denied·expired_token 을 우리 오류 코드로 옮겼다.
     */
    @PostMapping("/device/token")
    fun token(@Valid @RequestBody request: DeviceTokenRequest): ResponseEntity<Any> =
        when (val outcome = service.poll(request.deviceCode)) {
            is DeviceService.PollOutcome.Issued -> ResponseEntity.ok(SessionResponse.of(outcome.session))
            is DeviceService.PollOutcome.Wait -> ResponseEntity.badRequest().body(
                when (outcome.answer) {
                    DeviceAuthorization.Answer.Pending -> error(ErrorCode.AUTHORIZATION_PENDING, "아직 승인되지 않았다")
                    DeviceAuthorization.Answer.SlowDown -> error(ErrorCode.SLOW_DOWN, "너무 자주 물었다. 간격을 늘린다")
                    DeviceAuthorization.Answer.Denied -> error(ErrorCode.ACCESS_DENIED, "거절됐다")
                    else -> error(ErrorCode.DEVICE_CODE_EXPIRED, "코드가 만료됐거나 이미 썼다. 다시 로그인한다")
                },
            )
        }

    /** 승인 화면 — 사용자 코드로 기다리는 요청을 찾는다 */
    @GetMapping("/device/requests/{userCode}")
    fun request(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable userCode: String,
    ): ResponseEntity<DeviceRequestResponse> {
        val found = service.describe(userCode) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(DeviceRequestResponse(found.userCode, found.deviceName, found.client, found.createdAt, found.expiresAt))
    }

    @PostMapping("/device/requests/{userCode}")
    fun decide(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable userCode: String,
        @RequestBody request: DeviceDecisionRequest,
    ): ResponseEntity<Any> =
        if (service.decide(userCode, principal.id, request.approve)) ResponseEntity.noContent().build()
        else ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ErrorCode.DEVICE_CODE_EXPIRED, "기다리는 요청이 없다 — 만료됐거나 이미 결정했다"))

    /** 연결된 기기 */
    @GetMapping("/devices")
    fun devices(@RequestAttribute(Principal.ATTRIBUTE) principal: Principal): List<DeviceResponse> =
        service.connected(principal.id).map {
            DeviceResponse(it.id.toString(), it.device?.name ?: "", it.device?.client ?: "", it.createdAt, it.lastUsedAt)
        }

    @DeleteMapping("/devices/{id}")
    fun disconnect(@RequestAttribute(Principal.ATTRIBUTE) principal: Principal, @PathVariable id: UUID): ResponseEntity<Void> =
        if (service.disconnect(principal.id, id)) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()

    private fun error(code: ErrorCode, message: String) = ApiError(code, message, UUID.randomUUID().toString())
}

data class DeviceCodeRequest(@field:NotBlank val deviceName: String, @field:NotBlank val client: String)

data class DeviceCodeResponse(val deviceCode: String, val userCode: String, val verificationUri: String, val expiresAt: Instant, val interval: Long)

data class DeviceTokenRequest(@field:NotBlank val deviceCode: String)

data class DeviceDecisionRequest(val approve: Boolean)

data class DeviceRequestResponse(val userCode: String, val deviceName: String, val client: String, val createdAt: Instant, val expiresAt: Instant)

data class DeviceResponse(val id: String, val deviceName: String, val client: String, val connectedAt: Instant?, val lastUsedAt: Instant?)
