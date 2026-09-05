package dev.codedrill.judge.runner.messaging

import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.runner.execution.ExecutionEngine
import dev.codedrill.platform.messaging.JudgeQueues
import dev.codedrill.platform.observability.CorrelationIds
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

/**
 * 실행 요청 수신 지점 (기술 설계서 §4.1 5~6단계).
 *
 * Runner 는 결과를 브로커로만 돌려보낸다. Control Plane 자격증명도, DB 연결도 갖지
 * 않는다 (§2.3, §5.1).
 */
@Component
class ExecutionListener(
    private val engine: ExecutionEngine,
    private val rabbit: RabbitTemplate,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @RabbitListener(queues = [JudgeQueues.EXECUTIONS])
    fun onExecution(request: ExecutionRequest) {
        log.atInfo()
            .addKeyValue(CorrelationIds.SUBMISSION_ID, request.submissionId)
            .addKeyValue(CorrelationIds.EXECUTION_ID, request.executionId)
            .addKeyValue(CorrelationIds.ATTEMPT, request.attempt)
            .log("실행을 시작한다")

        // 엔진은 자기 안에서 일어난 모든 실패를 판정으로 바꿔 돌려준다. 여기서 예외가
        // 새어나가면 브로커가 재전달하고, 같은 실패를 반복하게 된다.
        val result = engine.execute(request)
        rabbit.convertAndSend(JudgeQueues.RESULTS, result)
    }
}
