package dev.codedrill.controlplane.project

/**
 * Project 모듈 경계 (기술 설계서 §3.1, feature-roadmap 11단계).
 *
 * - 소유 데이터: project_submission, project_submission_file
 * - 제공 인터페이스: 프로젝트형 문제 목록·상세(시작 저장소), 제출과 판정 조회
 * - 금지 의존성: 다른 도메인 모듈. 공개 여부는 [PublishedProjects] 로, 스토어는 [WorkspaceStore] 로
 *   조립 지점이 잇는다
 *
 * 두 번째 판정기의 제어 영역 쪽이다. Submission 모듈과 표도 봉투도 다르지만, "제출 행과
 * 아웃박스를 한 트랜잭션에" · "종착은 불변" · "남의 것은 없는 것처럼" 은 같다.
 */
internal object ProjectModule
