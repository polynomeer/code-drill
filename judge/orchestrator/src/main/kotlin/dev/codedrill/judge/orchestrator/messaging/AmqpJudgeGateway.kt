package dev.codedrill.judge.orchestrator.messaging

import dev.codedrill.judge.orchestrator.JudgeGateway
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.JudgeCompleted
import dev.codedrill.judge.protocol.JudgeProgressed
import dev.codedrill.platform.messaging.JudgeQueues
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

/** [JudgeGateway] 의 브로커 구현. 실행 영역은 브로커로만 바깥과 말한다 (§2.3). */
@Component
class AmqpJudgeGateway(private val rabbit: RabbitTemplate) : JudgeGateway {

    override fun requestExecution(request: ExecutionRequest) =
        rabbit.convertAndSend(JudgeQueues.EXECUTIONS, request)

    override fun publishProgress(progress: JudgeProgressed) =
        rabbit.convertAndSend(JudgeQueues.PROGRESS, progress)

    override fun publishCompleted(completed: JudgeCompleted) =
        rabbit.convertAndSend(JudgeQueues.PROGRESS, completed)
}
