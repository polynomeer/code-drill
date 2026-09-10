package dev.codedrill.controlplane.submission.trace

import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.platform.messaging.JudgeQueues
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

/**
 * 참조 트레이스 결과 수신 (FR-805).
 *
 * 봉투의 `submissionId` 에는 제출이 아니라 `<문제 버전>/<그룹>/<케이스>` 가 실려 있다.
 * 참조 실행은 어느 제출의 것도 아니고, 결과만 보고도 어느 자리에 넣을지 알아야 하기
 * 때문이다 — 요청과 결과를 짝지어 기억해 두면 재시작에 그 기억이 사라진다.
 */
@Component
class ReferenceTraceListener(private val service: DivergenceService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @RabbitListener(queues = [JudgeQueues.REFERENCE_TRACE_RESULTS])
    fun onResult(result: ExecutionResult) {
        // `<problemId>@<version>/<groupId>/<caseId>` 에서 앞의 문제 버전만 떼어낸다.
        val problemVersionId = result.submissionId.substringBefore('/')
        val caseId = result.submissionId.substringAfter('/', "")
        if (problemVersionId.isBlank() || caseId.isBlank()) {
            log.warn("참조 트레이스의 자리를 읽지 못했다: {}", result.submissionId)
            return
        }

        val events = result.trace?.events.orEmpty()
        // 이벤트가 없는 것은 실패가 아니다. 참조 풀이가 계측을 부르지 않았을 뿐이며,
        // 그때는 분기를 짚을 수 없다는 사실을 그대로 남긴다 (§7.3).
        service.referenceTraced(problemVersionId, caseId, events, empty = events.isEmpty())

        log.atInfo()
            .addKeyValue("problemVersionId", problemVersionId)
            .log("참조 트레이스 지문을 넣었다: {} 이벤트 {}개", caseId, events.size)
    }
}
