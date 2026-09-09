package dev.codedrill.platform.messaging

/**
 * 채점 경로의 큐 이름 (기술 설계서 §0.1, §10.2).
 *
 * 운영에서는 전부 quorum queue 로 선언한다. 작업 유실이 0 이어야 하기 때문이다 (§12.3).
 * 트레이스 작업은 별도 저우선순위 큐를 쓰며 여기에 섞지 않는다.
 */
object JudgeQueues {
    /** 제어 영역 → 오케스트레이터: 채점할 제출. */
    const val SUBMISSIONS = "judge.submissions"

    /** 오케스트레이터 → Runner: 실행 요청. */
    const val EXECUTIONS = "judge.executions"

    /** Runner → 오케스트레이터: 실행 결과 봉투. */
    const val RESULTS = "judge.results"

    /** 오케스트레이터 → 제어 영역: 진행과 종료 알림. */
    const val PROGRESS = "judge.progress"

    /**
     * Runner → 오케스트레이터: 실행 중이라는 심장 박동 (§4.3).
     *
     * 결과와 큐를 나눈다. 심장 박동은 잦고 값이 낮으므로, 브로커가 밀렸을 때 판정
     * 결과보다 앞에 서면 안 된다.
     */
    const val HEARTBEATS = "judge.heartbeats"

    val all = listOf(SUBMISSIONS, EXECUTIONS, RESULTS, PROGRESS, HEARTBEATS)

    /**
     * 제어 영역 내부 팬아웃: 제출 상태 변화 (§9.1 SSE).
     *
     * 위의 큐들과 성격이 다르다. 저것들은 **일이 하나씩 처리되어야** 하는 작업 큐라
     * 소비자 하나가 가져가면 끝이지만, 이것은 **모든 인스턴스가 받아야** 한다.
     *
     * SSE 구독은 인스턴스 메모리에 있다. 사용자가 A 에 붙어 있는데 판정 결과를 B 가
     * 처리하면, 팬아웃이 없을 때 그 사용자는 아무것도 받지 못한다.
     *
     * 여기 실리는 것은 이미 DB 에 쓴 사실의 사본이다. 유실돼도 재조회로 수렴한다.
     */
    const val SUBMISSION_EVENTS = "codedrill.submission-events"
}

/**
 * 인스턴스 사이로 흘려보내는 제출 이벤트 (§9.1).
 *
 * 진실의 원천이 아니다 — DB 조회가 정한다. 그래서 필드가 얇고, 놓쳐도 클라이언트가
 * 재조회로 수렴한다.
 */
data class SubmissionEvent(
    val submissionId: String,
    val event: String,
    val payload: Map<String, Any?>,
)
