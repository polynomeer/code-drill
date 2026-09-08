package dev.codedrill.controlplane.identity

import com.fasterxml.jackson.databind.ObjectMapper
import dev.codedrill.platform.common.ApiError
import dev.codedrill.platform.common.ErrorCode
import dev.codedrill.platform.common.Principal
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.UUID

/**
 * 사용자 인증 (기술 설계서 §11.2, §11.4 API authz 게이트).
 *
 * 이전 슬라이스는 `X-User-Id` 헤더를 그대로 믿었다. 헤더 한 줄만 바꾸면 남의 제출과
 * 소스가 열렸다 (§11.1 소스 노출). 여기서 토큰을 확인하고, 컨트롤러는 확인된 주체만
 * 본다.
 *
 * **보호 경로를 접두사로 건다.** 엔드포인트마다 거는 대신 경로로 걸어, 새 엔드포인트를
 * 추가한 사람이 인증을 붙이는 것을 잊어도 열리지 않게 한다 — 보안은 기억이 아니라
 * 기본값이어야 한다.
 *
 * 문제 목록·상세는 로그인 없이 열린다. 무엇을 풀 수 있는지 둘러보는 것은 공개 정보이며,
 * 여기에 로그인을 요구하면 얻는 것 없이 진입만 막는다.
 */
class AuthInterceptor(
    private val identity: IdentityService,
    private val json: ObjectMapper,
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val token = bearerOf(request)
            ?: return reject(response, ErrorCode.UNAUTHENTICATED, "로그인이 필요하다")

        return when (val resolution = identity.resolve(token)) {
            is IdentityService.Resolution.Active -> {
                request.setAttribute(Principal.ATTRIBUTE, resolution.user)
                true
            }

            // 만료와 폐기를 나눠 알린다. 만료면 클라이언트가 refresh 로 조용히 이어갈
            // 수 있지만, 폐기면 사용자가 다시 로그인해야 한다.
            IdentityService.Resolution.Expired ->
                reject(response, ErrorCode.TOKEN_EXPIRED, "access token 이 만료됐다. refresh 로 갱신한다")

            IdentityService.Resolution.Revoked ->
                reject(response, ErrorCode.UNAUTHENTICATED, "끊긴 세션이다. 다시 로그인해야 한다")

            IdentityService.Resolution.Unknown ->
                reject(response, ErrorCode.UNAUTHENTICATED, "알 수 없는 토큰이다")
        }
    }

    /**
     * 토큰은 `Authorization: Bearer <token>` 으로만 받는다.
     *
     * 쿼리 문자열로는 받지 않는다. URL 은 프록시 로그·브라우저 기록·리퍼러에 남아,
     * 토큰이 우리가 통제하지 않는 곳에 복제된다 (§11.3).
     */
    private fun bearerOf(request: HttpServletRequest): String? =
        request.getHeader("Authorization")
            ?.takeIf { it.startsWith(BEARER, ignoreCase = true) }
            ?.substring(BEARER.length)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun reject(response: HttpServletResponse, code: ErrorCode, message: String): Boolean {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = "application/json;charset=UTF-8"
        response.writer.write(
            json.writeValueAsString(ApiError(code, message, UUID.randomUUID().toString())),
        )
        return false
    }

    private companion object {
        const val BEARER = "Bearer "
    }
}

@Configuration
@EnableConfigurationProperties(IdentityProperties::class)
class IdentitySecurityConfig(
    private val identity: IdentityService,
    private val json: ObjectMapper,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(AuthInterceptor(identity, json))
            // 사용자의 것: 제출·초안·자기 정보. 소유자만 열 수 있어야 한다.
            .addPathPatterns(
                "/api/v1/submissions/**", "/api/v1/workspaces/**", "/api/v1/auth/**",
                // 관리자 API 도 같은 방식으로 로그인한다. 인가는 Admin 모듈이 이어서
                // 하지만, **누구인지 확인하는 일은 한 곳에서만** 일어나야 한다 (§11.2).
                "/api/v1/admin/**",
            )
            // 로그인과 가입 자체는 토큰 없이 부를 수 있어야 한다.
            .excludePathPatterns("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh")
            .order(AUTHENTICATION_ORDER)
    }

    private companion object {
        /** 인증이 인가보다 먼저다. AdminSecurityConfig 가 그 다음 순서를 쓴다. */
        const val AUTHENTICATION_ORDER = 0
    }
}
