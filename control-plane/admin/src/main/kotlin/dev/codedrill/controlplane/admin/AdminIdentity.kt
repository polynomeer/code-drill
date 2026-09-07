package dev.codedrill.controlplane.admin

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 관리자 역할 (기술 설계서 §11.2).
 *
 * 역할을 나누는 이유는 권한을 줄이기 위해서가 아니라 **한 사람이 혼자 끝내지 못하게**
 * 하기 위해서다. 문제를 등록하는 사람과 공개하는 사람, 재채점을 요청하는 사람과
 * 승인하는 사람이 갈려야 2인 승인이 이름만 남지 않는다.
 *
 * 그래서 한 계정에 CONTENT_EDITOR 와 PUBLISHER 를 함께 주면 안 된다. 코드가 막지는
 * 않는다 — 막을 수 있는 것은 "같은 사람이 두 단계를 밟는 것"이고, 그건 등록자·승인자
 * 비교로 이미 막혀 있다 (PublishService, RejudgeService).
 */
enum class AdminRole {
    /** 문제 패키지 등록 */
    CONTENT_EDITOR,

    /** 재채점 승인·반려 */
    REVIEWER,

    /** 문제 공개·보관 */
    PUBLISHER,

    /** 재채점 요청, 실행 영역 운영 */
    JUDGE_OPERATOR,

    /** 감사 로그 열람 */
    SECURITY_ADMIN,
}

/** 인증된 운영자. 감사 로그의 actor 는 이 이름이며, 헤더로 자칭한 값이 아니다 (§13.3). */
data class Operator(val name: String, val roles: Set<AdminRole>)

/**
 * 운영자 목록 (§11.2, §11.4 API authz 게이트).
 *
 * **비어 있으면 관리자 API 전체가 닫힌다.** 기본 토큰을 심어 두는 쪽이 편하지만, 그
 * 토큰은 반드시 어딘가의 운영 환경에 그대로 남는다. 설정하지 않은 환경에서 관리자
 * API 가 열려 있는 것보다, 설정을 잊었을 때 아무것도 안 되는 편이 낫다.
 *
 * 형식은 `<이름>:<토큰>:<역할,역할>` 을 `;` 로 이은 것이다. 로컬 설정 방법은
 * docs/running-locally.md 에 있다.
 *
 * 토큰을 이렇게 문자열로 두는 것은 슬라이스의 한계다. Identity 모듈이 붙으면 워크로드
 * ID 와 짧은 수명 토큰으로 옮긴다 (§11.2).
 */
@ConfigurationProperties(prefix = "codedrill.admin")
data class AdminProperties(val operators: String = "") {

    val byToken: Map<String, Operator> = operators
        .split(';')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .associate { entry ->
            val parts = entry.split(':')
            require(parts.size == 3) {
                "운영자 설정 형식은 <이름>:<토큰>:<역할,역할> 이다: $entry"
            }
            val (name, token, roles) = parts
            require(token.length >= MIN_TOKEN_LENGTH) {
                "운영자 토큰이 너무 짧다 (${MIN_TOKEN_LENGTH}자 이상): $name"
            }
            token to Operator(
                name = name,
                roles = roles.split(',')
                    .map { it.trim().uppercase() }
                    .filter { it.isNotEmpty() }
                    .map(AdminRole::valueOf)
                    .toSet(),
            )
        }

    fun resolve(token: String?): Operator? = token?.let { byToken[it] }

    private companion object {
        /** 짧은 토큰은 추측 가능하다. 사람이 손으로 고른 토큰을 걸러 내는 최소선이다. */
        const val MIN_TOKEN_LENGTH = 24
    }
}

/**
 * 이 엔드포인트를 부를 수 있는 역할.
 *
 * 붙이지 않은 관리자 엔드포인트는 "인증된 운영자 아무나"가 된다. 조회는 그래도 되지만
 * 상태를 바꾸는 엔드포인트에는 반드시 붙인다.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresRole(val value: AdminRole)
