package dev.codedrill.controlplane.workspace

import java.time.Instant
import java.util.UUID

/**
 * 문제별 질문 게시판의 글 (기획서 §8.5 "질문 게시판과 코드 구간 링크", "리플레이 시점을
 * 공유하는 주석").
 *
 * 질문과 답은 같은 것이다 — 답은 [parentId] 가 있는 글이다. 둘 다 [anchor] 로 자기 제출의
 * 코드 구간이나 리플레이 시점을 붙일 수 있고, 둘 다 신고되고 내려진다.
 *
 * **풀이가 드러나는 글은 잠긴다.** [spoiler] 인 글의 본문과 코드 구간은 이 문제를 맞힌
 * 사람에게만 보인다 — 아레나와 같은 잠금이다. 질문의 제목은 잠기지 않는다; 무엇을 묻는지는
 * 맞히기 전에도 보여야 "이미 물은 것인가"를 알 수 있다.
 *
 * 글쓴이의 이름은 나가지 않는다. 내 것인가 아닌가만 나간다 — 순위표에 이름을 올리는
 * 것은 평판 정책이 서고 나서다 (§8.5 기여자 평판).
 */
data class DiscussionPost(
    val id: UUID,
    val problemId: String,
    val parentId: UUID?,
    /** 계정을 지우면 null. 글은 남되 누구의 것인지는 남지 않는다 (§11.3). */
    val authorId: String?,
    val title: String?,
    val body: String,
    val anchor: Anchor?,
    val spoiler: Boolean,
    val status: PostStatus,
    val hiddenBy: String?,
    val hiddenAt: Instant?,
    val hiddenReason: String?,
    val createdAt: Instant,
)

enum class PostStatus { VISIBLE, HIDDEN }

/**
 * 글에 붙인 제출의 한 자리.
 *
 * [lineFrom]..[lineTo] 가 있으면 코드 구간이고 [excerpt] 가 그 줄들이다. [step] 이 있으면
 * 리플레이의 그 걸음이다. 둘 다 있을 수 있다 — "이 줄이 이 걸음에서 틀어진다".
 *
 * 제출은 글쓴이 자신의 것이어야 한다. 남의 제출을 가리키는 길은 없다.
 */
data class Anchor(
    val submissionId: UUID,
    val lineFrom: Int?,
    val lineTo: Int?,
    val step: Int?,
    /** 붙인 시점의 코드 구간. 제출 소스가 지워지면 이것도 지워진다. */
    val excerpt: String?,
)

data class DiscussionReport(
    val id: UUID,
    val postId: UUID,
    val reporterId: String,
    val reason: String,
    /** RETIRED 는 "글을 내렸다"다. 아레나 신고와 같은 상태 기계라 같은 이름을 쓴다. */
    val status: ReportStatus,
    val resolvedBy: String?,
    val resolvedAt: Instant?,
    val resolution: String?,
    val createdAt: Instant,
)

/**
 * 글에 붙일 수 있는 제출을 제출 도메인에 묻는다 (§3.1 조립 지점).
 *
 * 워크스페이스는 제출 표를 읽지 않는다. 그 제출이 누구의 것이고 어느 문제이고 소스가
 * 몇 줄인지는 제출 도메인의 것이다.
 */
fun interface AnchorableSubmissions {

    /** 이 사람의 것이고 이 문제의 제출이면 그 소스 줄들, 아니면 null. 소스가 지워진 제출은 빈 목록. */
    fun lines(userId: String, problemId: String, submissionId: UUID): List<String>?

    companion object {
        val NONE = AnchorableSubmissions { _, _, _ -> null }
    }
}
