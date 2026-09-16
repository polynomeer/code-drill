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
 *
 * 다만 로그인한 사람에게는 목록에 **푼 문제**가 표시돼야 한다 (FR-203). 그래서 [required]
 * 를 끈 채로 같은 인터셉터를 한 번 더 건다 — 토큰이 있으면 주체를 넣고, 없으면 그냥
 * 통과시킨다. 토큰을 푸는 코드를 따로 만들지 않는 이유는 위와 같다: **누구인지 확인하는
 * 일은 한 곳에서만** 일어나야 한다.
 */
class AuthInterceptor(
    private val identity: IdentityService,
    private val json: ObjectMapper,
    /** 제재 (§8.5). null 이면 이 인터셉터는 제재를 보지 않는다 — 공개 경로 쪽이 그렇다. */
    private val sanctions: SanctionService? = null,
    /**
     * 토큰이 없거나 못 쓰면 막을지.
     *
     * `false` 면 익명으로 통과시킨다. **잘못된 토큰도 통과시킨다** — 공개 경로에서
     * 만료된 토큰 때문에 목록이 안 보이면, 사용자는 자기가 무엇을 잘못했는지 알 수 없다.
     * 그 경로는 로그인 여부와 무관하게 답할 수 있어야 한다.
     */
    private val required: Boolean = true,
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val token = bearerOf(request)
            ?: return !required || reject(response, ErrorCode.UNAUTHENTICATED, "로그인이 필요하다")

        val resolution = identity.resolve(token)
        if (resolution !is IdentityService.Resolution.Active && !required) return true

        return when (resolution) {
            is IdentityService.Resolution.Active -> {
                request.setAttribute(Principal.ATTRIBUTE, resolution.user)
                sanctioned(request, resolution.user.id)?.let { return blocked(response, it) }
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

    /**
     * 제재 중인 계정의 쓰기 요청을 문 앞에서 거른다 (§8.5 단계적 제재).
     *
     * 엔드포인트마다 묻지 않고 여기서 경로로 거는 이유는 인증과 같다 — 새 쓰기 경로를 만든
     * 사람이 제재를 잊어도 열리지 않아야 한다. 읽기는 전부 열린다: 제재 중에도 자기 판정과
     * 남의 글은 볼 수 있어야 하고, 이의는 낼 수 있어야 한다.
     */
    private fun sanctioned(request: HttpServletRequest, userId: String): Sanction? {
        val gate = sanctions ?: return null
        if (request.method !in WRITE_METHODS) return null
        val path = request.requestURI
        if (path.startsWith(APPEAL_PATH)) return null
        val active = gate.active(userId) ?: return null
        val writing = WRITING_PATHS.any { path.startsWith(it) }
        val executing = EXECUTION_PATHS.any { path.startsWith(it) }
        return when {
            executing && active.kind.blocksExecution -> active
            writing && active.kind.blocksWriting -> active
            else -> null
        }
    }

    private fun blocked(response: HttpServletResponse, sanction: Sanction): Boolean {
        response.status = HttpStatus.FORBIDDEN.value()
        response.contentType = "application/json;charset=UTF-8"
        val what = if (sanction.kind == SanctionKind.SUSPEND) "제출과 실행이 정지" else "글쓰기가 정지"
        val until = sanction.endsAt?.let { " ($it 까지)" } ?: ""
        response.writer.write(
            json.writeValueAsString(
                ApiError(ErrorCode.ACCOUNT_SANCTIONED, "${what}됐다$until: ${sanction.reason}. 이의는 계정 설정에서 낼 수 있다", UUID.randomUUID().toString()),
            ),
        )
        return false
    }

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
        val WRITE_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
        const val APPEAL_PATH = "/api/v1/auth/me/sanction"
        /** 커뮤니티의 쓰기 — 질문·답·풀이·도움됐다·신고, 그리고 아레나의 기부·신고. */
        val WRITING_PATHS = listOf("/api/v1/discussions", "/api/v1/arena", "/api/v1/contests")
        /** 실행 — 제출, 테스트 실행, 실험실, 변이 평가, 아레나 시도. */
        val EXECUTION_PATHS = listOf("/api/v1/submissions", "/api/v1/trials", "/api/v1/labs", "/api/v1/mutations", "/api/v1/arena", "/api/v1/projects")
    }
}

@Configuration
@EnableConfigurationProperties(IdentityProperties::class)
class IdentitySecurityConfig(
    private val identity: IdentityService,
    private val json: ObjectMapper,
    private val sanctions: SanctionService,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(AuthInterceptor(identity, json, sanctions))
            // 사용자의 것: 제출·초안·자기 정보. 소유자만 열 수 있어야 한다.
            .addPathPatterns(
                "/api/v1/submissions/**", "/api/v1/workspaces/**", "/api/v1/trials/**",
                "/api/v1/prequestions/**", "/api/v1/mutations/**", "/api/v1/coaching/**", "/api/v1/labs/**", "/api/v1/arena/**", "/api/v1/discussions/**", "/api/v1/contests/**",
                // 프로젝트형: 제출과 그 기록은 사용자의 것. 목록·상세는 아래의 공개 경로다.
                "/api/v1/projects/*/submissions", "/api/v1/projects/submissions/**",
                "/api/v1/me/**", "/api/v1/auth/**",
                // 관리자 API 도 같은 방식으로 로그인한다. 인가는 Admin 모듈이 이어서
                // 하지만, **누구인지 확인하는 일은 한 곳에서만** 일어나야 한다 (§11.2).
                "/api/v1/admin/**",
            )
            // 로그인과 가입 자체는 토큰 없이 부를 수 있어야 한다.
            .excludePathPatterns("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh")
            .order(AUTHENTICATION_ORDER)

        // 공개 경로지만 로그인했다면 알아본다. 목록에 "푼 문제"를 표시하기 위해서다.
        registry.addInterceptor(AuthInterceptor(identity, json, required = false))
            .addPathPatterns("/api/v1/problems/**", "/api/v1/problems", "/api/v1/projects", "/api/v1/projects/*")
            .order(AUTHENTICATION_ORDER)
    }

    private companion object {
        /** 인증이 인가보다 먼저다. AdminSecurityConfig 가 그 다음 순서를 쓴다. */
        const val AUTHENTICATION_ORDER = 0
    }
}
