package dev.codedrill.platform.common

/**
 * 인증된 호출자 (기술 설계서 §11.2).
 *
 * 도메인 모듈은 인증을 구현하지 않고 이 타입만 안다. Identity 모듈이 토큰을 확인해
 * 요청 속성에 넣고, 컨트롤러는 `@RequestAttribute(Principal.ATTRIBUTE)` 로 받는다.
 * 그래서 컨트롤러가 인증 방식을 몰라도 되고, Identity 는 도메인 모듈을 몰라도 된다 (§3.1).
 *
 * **헤더로 받지 않는다.** 이전 슬라이스는 `X-User-Id` 를 그대로 믿었는데, 그러면 헤더
 * 한 줄로 남의 제출과 소스를 열 수 있다 (§11.1 소스 노출).
 *
 * 이메일은 담지 않는다. 소유자 비교에 필요한 것은 [id] 뿐이고, 표시에 필요한 것은
 * [displayName] 뿐이다. 담아 두면 로그나 응답 어딘가로 실려 나갈 길이 생긴다 (§11.3).
 */
data class Principal(val id: String, val displayName: String) {
    companion object {
        const val ATTRIBUTE = "codedrill.principal"
    }
}
