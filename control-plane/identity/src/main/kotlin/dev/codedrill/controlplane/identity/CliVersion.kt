package dev.codedrill.controlplane.identity

import dev.codedrill.platform.common.ApiError
import dev.codedrill.platform.common.ErrorCode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.servlet.HandlerInterceptor
import java.util.UUID

/**
 * CLI 의 최소 판 (feature-roadmap 11단계 이어서 — 2단계).
 *
 * `codedrill` 은 `User-Agent: codedrill/<판> (...)` 을 싣는다. 키트 형식이나 제출 규칙이 바뀌어 옛 CLI 가 틀린 것을
 * 올릴 때가 오면 [IdentityProperties.cliMinimumVersion] 을 올린다 — 그보다 낮은 CLI 는 426 을 받고 업그레이드하라는
 * 말을 본다. 조용히 엇나간 제출보다 분명한 거절이 낫다.
 *
 * 판을 읽을 수 없는 것(`dev` 빌드, 다른 클라이언트)은 막지 않는다. 막는 것은 **낡았다고 확실한** 판뿐이다.
 */
object CliVersion {

    private val AGENT = Regex("""^codedrill/(\S+)""")
    private val SEMVER = Regex("""^v?(\d+)\.(\d+)\.(\d+)""")

    /** 이 요청이 [minimum] 보다 낮은 CLI 의 것이면 그 판, 아니면 null */
    fun tooOld(userAgent: String?, minimum: String): String? {
        val version = userAgent?.let { AGENT.find(it)?.groupValues?.get(1) } ?: return null
        val mine = parts(version) ?: return null
        val floor = parts(minimum) ?: return null
        return version.takeIf { compare(mine, floor) < 0 }
    }

    private fun parts(version: String): List<Int>? =
        SEMVER.find(version)?.groupValues?.drop(1)?.map(String::toInt)

    private fun compare(a: List<Int>, b: List<Int>): Int =
        a.zip(b).firstOrNull { (x, y) -> x != y }?.let { (x, y) -> x.compareTo(y) } ?: 0
}

class CliVersionInterceptor(private val minimum: String, private val json: ObjectMapper) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val old = CliVersion.tooOld(request.getHeader("User-Agent"), minimum) ?: return true
        response.status = 426
        response.contentType = "application/json;charset=UTF-8"
        response.setHeader("X-CodeDrill-Minimum-Version", minimum)
        response.writer.write(
            json.writeValueAsString(
                ApiError(ErrorCode.UPGRADE_REQUIRED, "codedrill $minimum 이상이 필요하다 (지금 $old). 업그레이드한다", UUID.randomUUID().toString()),
            ),
        )
        return false
    }
}
