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

/**
 * 관리자 설정 (§11.2).
 *
 * **비밀이 없다.** 예전에는 여기에 `<이름>:<토큰>:<역할>` 이 통째로 들어 있었고, 그
 * 토큰은 프로세스 목록에서 읽는 순간 곧바로 관리자였다. 이제 신원은 Identity 모듈의
 * 계정이 정하고, 이 설정에는 **첫 역할을 누구에게 줄지**만 남는다.
 *
 * [bootstrapEmail] 은 비밀이 아니다. 알아도 그 계정의 비밀번호가 없으면 아무것도 못
 * 한다. 그리고 역할 표가 비어 있을 때 딱 한 번만 쓰인다 ([AdminRoles]).
 *
 * 비워 두면 부트스트랩이 없다. 이미 역할을 가진 사람이 있는 환경에서는 그게 맞다 —
 * 첫 역할을 만드는 길은 필요할 때만 열려 있어야 한다.
 */
@ConfigurationProperties(prefix = "codedrill.admin")
data class AdminProperties(val bootstrapEmail: String = "")

/**
 * 이 엔드포인트를 부를 수 있는 역할.
 *
 * 붙이지 않은 관리자 엔드포인트는 "인증된 운영자 아무나"가 된다. 조회는 그래도 되지만
 * 상태를 바꾸는 엔드포인트에는 반드시 붙인다.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresRole(val value: AdminRole)
