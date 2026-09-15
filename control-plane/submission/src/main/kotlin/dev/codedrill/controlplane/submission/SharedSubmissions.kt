package dev.codedrill.controlplane.submission

import java.util.UUID

/**
 * 남의 제출을 읽어도 되는 경우 (§3.1 조립 지점, 기획서 §8.5 "리플레이 시점을 공유하는 주석").
 *
 * 제출은 소유자의 것이다. 예외는 하나 — 글쓴이가 질문 게시판에 자기 제출의 리플레이 시점을
 * 붙였을 때, 그 글을 볼 수 있는 사람은 그 리플레이를 열 수 있어야 한다. 누가 무엇을 붙였는지는
 * 게시판이 알고, 제출 도메인은 묻기만 한다.
 *
 * 열리는 것은 **판정과 트레이스**다. 소스는 열리지 않는다 — 글에 붙은 코드 구간은 글을
 * 통해 나가고, 그 밖의 줄은 글쓴이가 보이기로 한 것이 아니다.
 */
fun interface SharedSubmissions {

    fun sharedWith(readerId: String, submissionId: UUID): Boolean

    companion object {
        val NONE = SharedSubmissions { _, _ -> false }
    }
}
