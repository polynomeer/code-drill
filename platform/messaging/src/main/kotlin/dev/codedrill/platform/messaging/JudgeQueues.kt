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
}
