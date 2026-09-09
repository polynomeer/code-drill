package dev.codedrill.controlplane.admin

import dev.codedrill.platform.common.Principal
import org.springframework.http.HttpStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 관리자 인가 (기술 설계서 §11.2, §11.4 API authz).
 *
 * 이 테스트가 고정하는 것은 하나다: **역할을 받지 않은 사람은 아무것도 못 한다.**
 *
 * 예전에는 신원이 `ADMIN_OPERATORS` 의 공유 토큰이었고, 이 자리에서 그 문자열을 파싱하는
 * 것을 시험했다. 지금 신원은 Identity 모듈이 정하고 이 모듈은 인가만 하므로, 시험할
 * 것도 인가 판단 하나다.
 */
class AdminIdentityTest {

    @Test
    fun `로그인하지 않으면 아무것도 못 한다`() {
        // Identity 인터셉터가 principal 을 넣지 못한 경우다. 경로가 인증 대상에서
        // 빠졌다는 뜻이므로, 통과가 아니라 거부여야 한다.
        val decision = AdminAuthInterceptor.decide(
            principal = null,
            granted = setOf(AdminRole.SECURITY_ADMIN),
            required = null,
        )

        assertEquals(HttpStatus.UNAUTHORIZED, (decision as Decision.Deny).status)
    }

    @Test
    fun `역할이 없는 계정은 관리자 API 를 못 연다`() {
        // 일반 사용자도 로그인은 한다. 로그인했다는 사실만으로 열리면 안 된다.
        val decision = AdminAuthInterceptor.decide(
            principal = Principal("user-1", "보통 사람"),
            granted = emptySet(),
            required = null,
        )

        assertEquals(HttpStatus.FORBIDDEN, (decision as Decision.Deny).status)
        assertTrue("역할이 없다" in decision.reason, decision.reason)
    }

    @Test
    fun `역할이 모자라면 거부한다`() {
        val decision = AdminAuthInterceptor.decide(
            principal = Principal("user-1", "편집자"),
            granted = setOf(AdminRole.CONTENT_EDITOR),
            required = AdminRole.PUBLISHER,
        )

        assertEquals(HttpStatus.FORBIDDEN, (decision as Decision.Deny).status)
        assertTrue("PUBLISHER" in decision.reason, decision.reason)
    }

    @Test
    fun `역할을 가지면 계정 id 가 actor 가 된다`() {
        // 표시 이름이 아니라 id 다. 2인 승인이 이 값의 문자열 비교로 판단하는데,
        // 이름은 겹칠 수 있고 바뀔 수 있다.
        val decision = AdminAuthInterceptor.decide(
            principal = Principal("user-1", "겹치는 이름"),
            granted = setOf(AdminRole.PUBLISHER),
            required = AdminRole.PUBLISHER,
        )

        assertEquals("user-1", (decision as Decision.Allow).actor)
    }

    @Test
    fun `역할을 명시하지 않은 엔드포인트는 운영자 아무나 연다`() {
        val decision = AdminAuthInterceptor.decide(
            principal = Principal("user-1", "운영자"),
            granted = setOf(AdminRole.REVIEWER),
            required = null,
        )

        assertTrue(decision is Decision.Allow)
    }

    @Test
    fun `부트스트랩 이메일은 기본값이 비어 있다`() {
        // 비어 있으면 첫 역할을 만드는 길이 닫혀 있다. 설정하지 않은 환경에서 관리자
        // API 가 열려 있는 것보다, 설정을 잊었을 때 아무것도 안 되는 편이 낫다.
        assertEquals("", AdminProperties().bootstrapEmail)
    }
}

/**
 * 역할 변경의 안전장치 (기술 설계서 §11.2).
 *
 * 판단만 순수 함수로 빼 두었다. 규칙이 DB 질의 사이에 섞여 있으면 시험하려고 DB 를
 * 띄워야 하고, 그러면 이 두 규칙은 시험되지 않은 채로 남는다.
 */
class RoleChangeTest {

    @Test
    fun `자기 자신에게는 역할을 줄 수 없다`() {
        // 막지 않으면 SECURITY_ADMIN 한 명이 자기에게 등록 권한과 공개 권한을 붙여
        // 2인 승인을 혼자 통과한다.
        val refused = AdminRoles.selfGrant(userId = "user-1", actor = "user-1")

        assertTrue(refused is RoleOutcome.Refused)
        assertTrue("자기 자신" in refused.reason, refused.reason)
    }

    @Test
    fun `남에게 주는 것은 막지 않는다`() {
        assertEquals(null, AdminRoles.selfGrant(userId = "user-1", actor = "user-2"))
    }

    @Test
    fun `마지막 SECURITY_ADMIN 은 회수할 수 없다`() {
        // 회수하면 역할을 줄 수 있는 사람이 없어지는데, 부트스트랩은 역할 표가
        // 완전히 비어야 열린다. 다른 역할이 하나라도 남아 있으면 되살릴 길이 없다.
        val refused = AdminRoles.lastSecurityAdmin(AdminRole.SECURITY_ADMIN, holders = 1)

        assertTrue(refused is RoleOutcome.Refused)
        assertTrue("마지막" in refused.reason, refused.reason)
    }

    @Test
    fun `둘 이상이면 회수할 수 있다`() {
        assertEquals(null, AdminRoles.lastSecurityAdmin(AdminRole.SECURITY_ADMIN, holders = 2))
    }

    @Test
    fun `요청자가 자기 요청을 승인할 수 없다`() {
        val refused = AdminRoles.approvalConflict(
            userId = "subject", requestedBy = "admin-a", approver = "admin-a",
        )

        assertTrue(refused is RoleOutcome.Refused)
        assertTrue("요청자와 승인자가 같다" in refused.reason, refused.reason)
    }

    @Test
    fun `역할을 받는 사람은 자기 승격을 승인할 수 없다`() {
        // 이것이 담합을 막는 자리다. 허용하면 SECURITY_ADMIN 둘이 서로에게 주면서
        // 각자 권한을 늘릴 수 있고, 권한을 키우는 데 필요한 사람 수가 다시 둘이 된다.
        val refused = AdminRoles.approvalConflict(
            userId = "admin-b", requestedBy = "admin-a", approver = "admin-b",
        )

        assertTrue(refused is RoleOutcome.Refused)
        assertTrue("자기 승격" in refused.reason, refused.reason)
    }

    @Test
    fun `제3자가 승인하면 통과한다`() {
        assertEquals(
            null,
            AdminRoles.approvalConflict(
                userId = "subject", requestedBy = "admin-a", approver = "admin-b",
            ),
        )
    }

    @Test
    fun `SECURITY_ADMIN 이 한 명이면 동료를 혼자 만들 수 있다`() {
        // 승인해 줄 사람이 없는 구간이다. 이 길이 없으면 부트스트랩 계정 하나로
        // 굳어 아무 역할도 만들지 못한다.
        assertTrue(AdminRoles.soloGrant(AdminRole.SECURITY_ADMIN, securityAdmins = 1))
    }

    @Test
    fun `혼자여도 다른 역할은 못 준다`() {
        // 열어 두면 혼자서 부하 계정에 PUBLISHER 를 붙일 수 있고, 등록자·승인자
        // 비교는 그 둘을 두 사람으로 센다 — 막으려던 것이 그대로 남는다.
        assertEquals(false, AdminRoles.soloGrant(AdminRole.PUBLISHER, securityAdmins = 1))
    }

    @Test
    fun `동료가 생기면 단독 부여가 닫힌다`() {
        assertEquals(false, AdminRoles.soloGrant(AdminRole.SECURITY_ADMIN, securityAdmins = 2))
    }

    @Test
    fun `다른 역할은 마지막이어도 회수할 수 있다`() {
        // 잠기는 것은 역할을 줄 수 있는 사람이 사라질 때뿐이다. PUBLISHER 가 없어지면
        // 공개를 못 할 뿐, SECURITY_ADMIN 이 다시 줄 수 있다.
        assertEquals(null, AdminRoles.lastSecurityAdmin(AdminRole.PUBLISHER, holders = 1))
    }
}
