package dev.codedrill.controlplane.admin

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * 관리자 API 인증·인가 (기술 설계서 §11.2, §11.4 API authz 게이트).
 *
 * 이전 슬라이스는 행위자를 `X-Actor` 헤더로 **자칭**하게 두었다. 그러면 감사 로그의
 * actor 는 "누가 했는가"가 아니라 "누구라고 적었는가"가 되고, 2인 승인은 헤더를 두 번
 * 바꾸는 것으로 무너진다. 여기서 토큰으로 신원을 확인하고, 컨트롤러는 확인된 이름만
 * 본다.
 *
 * 인증 실패도 감사 로그에 남긴다 (§13.3). 관리자 API 를 두드리는 시도는 그 자체가
 * 보안 신호이며, 성공만 기록하면 공격의 앞부분이 통째로 비어 버린다.
 */
class AdminAuthInterceptor(
    private val properties: AdminProperties,
    private val audit: AuditLog,
) : HandlerInterceptor {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (properties.byToken.isEmpty()) {
            return deny(
                request, response, HttpStatus.SERVICE_UNAVAILABLE,
                "운영자가 설정되지 않아 관리자 API 가 닫혀 있다 (codedrill.admin.operators)",
                actor = ANONYMOUS,
            )
        }

        val operator = properties.resolve(tokenOf(request))
            ?: return deny(
                request, response, HttpStatus.UNAUTHORIZED,
                "유효한 운영자 토큰이 필요하다", actor = ANONYMOUS,
            )

        val required = (handler as? HandlerMethod)
            ?.getMethodAnnotation(RequiresRole::class.java)
            ?.value

        if (required != null && required !in operator.roles) {
            return deny(
                request, response, HttpStatus.FORBIDDEN,
                "이 작업에는 $required 역할이 필요하다", actor = operator.name,
            )
        }

        request.setAttribute(ACTOR_ATTRIBUTE, operator.name)
        return true
    }

    /**
     * 토큰은 `Authorization: Bearer <token>` 으로 받는다.
     *
     * 쿼리 문자열로는 받지 않는다. URL 은 프록시 로그·브라우저 기록·리퍼러에 그대로
     * 남아, 토큰이 우리가 통제하지 않는 곳에 복제된다 (§11.3).
     */
    private fun tokenOf(request: HttpServletRequest): String? =
        request.getHeader("Authorization")
            ?.takeIf { it.startsWith(BEARER, ignoreCase = true) }
            ?.substring(BEARER.length)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun deny(
        request: HttpServletRequest,
        response: HttpServletResponse,
        status: HttpStatus,
        reason: String,
        actor: String,
    ): Boolean {
        log.warn("관리자 API 접근 거부 [{}] {} {}: {}", status.value(), request.method, request.requestURI, reason)
        audit.record(
            AuditAction.ADMIN_ACCESS_DENIED,
            subject = "${request.method} ${request.requestURI}",
            actor = actor,
            // 토큰은 남기지 않는다. 거부 사유는 무엇이 부족했는지까지면 충분하다 (§11.3).
            detail = mapOf("status" to status.value(), "reason" to reason),
        )
        response.status = status.value()
        response.contentType = "application/json;charset=UTF-8"
        response.writer.write("""{"code":"FORBIDDEN","message":"$reason"}""")
        return false
    }

    companion object {
        /** 컨트롤러가 `@RequestAttribute` 로 받는 키. 확인된 이름만 여기에 들어간다. */
        const val ACTOR_ATTRIBUTE = "codedrill.actor"

        private const val BEARER = "Bearer "
        private const val ANONYMOUS = "anonymous"
    }
}

/**
 * 관리자 API 전체에 인증을 건다.
 *
 * 엔드포인트마다 거는 대신 경로 접두사로 건다. 새 관리자 엔드포인트를 추가한 사람이
 * 인증을 붙이는 것을 잊어도 열리지 않는다 — 보안은 기억이 아니라 기본값이어야 한다.
 */
@Configuration
@EnableConfigurationProperties(AdminProperties::class)
class AdminSecurityConfig(
    private val properties: AdminProperties,
    private val audit: AuditLog,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(AdminAuthInterceptor(properties, audit))
            .addPathPatterns("/api/v1/admin/**")
    }
}
