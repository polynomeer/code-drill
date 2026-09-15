package dev.codedrill.controlplane.admin

import java.util.UUID

/**
 * Admin 이 질문 게시판에 요청하는 것 (기술 설계서 §3.1 모듈 경계, 기획서 §8.5 신고·제재).
 *
 * Admin 은 글·신고 표를 직접 읽지 않는다. 검수는 "무엇을 내릴지 결정"하는 일이고, 내리는
 * 것은 게시판의 것이다. 아레나 검수([ArenaModeration])와 같은 모양이다.
 */
interface DiscussionModeration {

    /** 열린 신고와 신고된 글. 글 전체가 실린다 — 검수자에게 잠금은 없다. */
    fun queue(): Any

    /** 신고를 처리한다. [hide] 면 글을 내리고, 아니면 신고를 기각한다. */
    fun resolve(reportId: UUID, reviewer: String, hide: Boolean, resolution: String): ArenaModeration.Decision
}
