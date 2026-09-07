package dev.codedrill.controlplane.admin

import java.util.UUID

/**
 * Admin 이 제출 도메인에 요청하는 것 (기술 설계서 §3.1 모듈 경계).
 *
 * Admin 은 제출 테이블을 직접 읽지 않는다. 재채점은 "무엇을 다시 돌릴지 결정"하는
 * 일이고, "제출을 다시 큐에 올리는" 일은 제출 도메인의 것이다. 둘을 섞으면 아웃박스와
 * 함께 커밋해야 한다는 §3.2 규칙이 두 모듈에 나뉘어 걸린다.
 */
interface JudgedSubmissions {

    /** 종료된 제출의 ID. 재채점 대상은 이미 판정이 난 것들뿐이다. */
    fun completedFor(problemId: String): List<UUID>

    /** 그 제출이 종료됐으면 ID 를, 아니면 빈 목록. */
    fun completed(submissionId: UUID): List<UUID>

    /**
     * 다시 채점 큐에 올린다. 제출 행과 아웃박스 이벤트를 함께 커밋한다 (§3.2).
     *
     * 실제로 큐에 오른 건수를 돌려준다. 사이에 사라진 제출은 조용히 빠진다.
     */
    fun requeue(ids: List<UUID>): Int
}
