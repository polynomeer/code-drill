package dev.codedrill.controlplane.submission

import dev.codedrill.judge.protocol.SourceRef

/**
 * 제출 소스가 실행 영역으로 가는 길 (기술 설계서 §8.3, §3.1 조립 지점).
 *
 * 소스는 메시지에 실리지 않는다. 제출할 때 오브젝트 스토어에 올리고 참조만 큐에 싣는다 —
 * 브로커의 큐·데드레터·로그에 사용자의 코드가 복제되지 않는다 (§11.1, §11.3). DB 의 소스는
 * 화면·공유·유사도 신호의 것이고 그대로 남는다; 스토어의 것은 **실행용 복제**라 DB 에서
 * 언제든 다시 만들 수 있다 ([ensure]). 그래서 백업은 DB 만 받아도 반쪽이 되지 않는다.
 *
 * 삭제(§11.3)는 둘을 함께 지운다 — 스토어의 것이 남으면 지운 것이 아니다.
 */
interface SourceStore {

    /** 올린다. 같은 제출을 다시 올리면 같은 키에 덮어쓴다. */
    fun store(submissionId: String, source: String): SourceRef

    /** 있고 digest 가 맞으면 그대로, 없거나 다르면 다시 올린다. 재채점이 쓴다. */
    fun ensure(submissionId: String, source: String): SourceRef

    /** 지운다. 없어도 조용하다. */
    fun delete(submissionId: String)

    companion object {
        /** 스토어가 없는 조립(테스트)용. 소스를 메시지에 그대로 싣게 된다 — 운영에는 쓰지 않는다. */
        val NONE: SourceStore? = null
    }
}
