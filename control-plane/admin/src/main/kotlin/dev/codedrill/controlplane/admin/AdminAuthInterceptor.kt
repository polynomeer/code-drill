package dev.codedrill.controlplane.admin

import dev.codedrill.platform.common.Principal
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
 * **인증은 하지 않는다. 인가만 한다.** 누가 호출했는지는 Identity 모듈의 인터셉터가
 * 먼저 확인해 [Principal] 을 요청에 넣어 두고, 여기서는 그 계정에 붙은 역할만 본다.
 *
 * 그래서 관리자 API 가 별도의 장기 비밀을 갖지 않는다. 예전에는 `ADMIN_OPERATORS` 에
 * 적힌 토큰이 곧 신원이었고, 그 값은 프로세스 목록에 평문으로 보였다 — 읽는 순간
 * 관리자였다. 지금은 관리자도 사람과 같은 방식으로 로그인하고, 그 세션은 30분이면
 * 만료된다 (§11.2 짧은 수명 토큰).
 *
 * 감사 로그의 actor 는 계정 id 다. 이름은 겹칠 수 있고 바뀔 수 있는데, 2인 승인은
 * "같은 사람인가"를 문자열 비교로 판단한다 (PublishService, RejudgeService).
 *
 * 인가 실패도 감사 로그에 남긴다 (§13.3). 관리자 API 를 두드리는 시도는 그 자체가
 * 보안 신호이며, 성공만 기록하면 공격의 앞부분이 통째로 비어 버린다.
 */
class AdminAuthInterceptor(
    private val roles: GrantedRoles,
    private val audit: AuditLog,
) : HandlerInterceptor {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val principal = request.getAttribute(Principal.ATTRIBUTE) as? Principal
        val required = (handler as? HandlerMethod)
            ?.getMethodAnnotation(RequiresRole::class.java)
            ?.value

        return when (
            val decision = decide(principal, principal?.let { roles.of(it.id) }.orEmpty(), required)
        ) {
            is Decision.Allow -> {
                request.setAttribute(ACTOR_ATTRIBUTE, decision.actor)
                true
            }

            is Decision.Deny -> deny(
                request, response, decision.status, decision.reason,
                actor = principal?.id ?: ANONYMOUS,
            )
        }
    }

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
        /**
         * 인가 판단. HTTP 를 모르는 순수 함수로 둔다.
         *
         * 여기에 담긴 것이 관리자 API 의 보안 규칙 전부다. 인터셉터 안에 섞어 두면
         * 서블릿을 흉내 내지 않고는 시험할 수 없고, 시험하지 않는 보안 규칙은
         * 리팩터링 한 번에 조용히 뒤집힌다.
         */
        internal fun decide(
            principal: Principal?,
            granted: Set<AdminRole>,
            required: AdminRole?,
        ): Decision = when {
            // Identity 인터셉터가 먼저 돌아 넣어 둔다. 없다는 것은 이 경로가 인증
            // 대상에서 빠졌다는 뜻이므로 닫는다 — 설정 실수가 관리자 API 를 여는
            // 쪽으로 기울면 안 된다.
            principal == null ->
                Decision.Deny(HttpStatus.UNAUTHORIZED, "로그인이 필요하다")

            granted.isEmpty() ->
                Decision.Deny(HttpStatus.FORBIDDEN, "이 계정에는 관리자 역할이 없다")

            // 역할을 명시하지 않은 엔드포인트는 "역할을 가진 운영자 아무나"다. 조회는
            // 그래도 되지만, 상태를 바꾸는 곳에는 @RequiresRole 을 붙인다.
            required != null && required !in granted ->
                Decision.Deny(HttpStatus.FORBIDDEN, "이 작업에는 $required 역할이 필요하다")

            else -> Decision.Allow(principal.id)
        }

        /**
         * 컨트롤러가 `@RequestAttribute` 로 받는 키.
         *
         * 값은 **계정 id** 다. 표시 이름이 아니다 — 2인 승인이 이 값의 문자열 비교로
         * 판단하는데, 이름은 겹칠 수 있고 바뀔 수 있다.
         */
        const val ACTOR_ATTRIBUTE = "codedrill.actor"

        private const val ANONYMOUS = "anonymous"
    }
}

/** [AdminAuthInterceptor.decide] 의 결과. */
internal sealed interface Decision {
    /** [actor] 는 계정 id 다. 감사 로그와 2인 승인이 이 값을 쓴다. */
    data class Allow(val actor: String) : Decision
    data class Deny(val status: HttpStatus, val reason: String) : Decision
}

/**
 * 계정에 부여된 역할 (§11.2).
 *
 * 인터셉터가 [AdminRoles] 를 직접 붙들지 않는다. 인가 판단은 DB 없이 시험할 수 있어야
 * 하고, 그 판단이 이 모듈에서 가장 뒤집히면 안 되는 규칙이다.
 */
fun interface GrantedRoles {
    fun of(userId: String): Set<AdminRole>
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
    private val roles: GrantedRoles,
    private val audit: AuditLog,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(AdminAuthInterceptor(roles, audit))
            .addPathPatterns("/api/v1/admin/**")
            // 인증 다음에 인가다. 순서를 명시하지 않으면 설정 클래스가 등록되는 순서에
            // 달리고, 그러면 Principal 이 아직 없는 채로 이 인터셉터가 먼저 돌 수 있다.
            .order(AUTHORIZATION_ORDER)
    }

    private companion object {
        const val AUTHORIZATION_ORDER = 10
    }
}
