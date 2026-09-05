package dev.codedrill.judge.orchestrator.messaging

import dev.codedrill.judge.orchestrator.JudgeCoordinator
import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.judge.protocol.SubmissionQueued
import dev.codedrill.platform.messaging.JudgeQueues
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

/**
 * 브로커 수신 지점.
 *
 * 작업은 at-least-once 로 전달되므로 두 핸들러 모두 멱등해야 한다. 멱등성은
 * [dev.codedrill.judge.orchestrator.lease.AttemptRegistry] 가 책임진다 (§4.3).
 */
@Component
class JudgeListeners(private val coordinator: JudgeCoordinator) {

    @RabbitListener(queues = [JudgeQueues.SUBMISSIONS])
    fun onSubmission(message: SubmissionQueued) = coordinator.onSubmissionQueued(message)

    @RabbitListener(queues = [JudgeQueues.RESULTS])
    fun onResult(result: ExecutionResult) =
        coordinator.onExecutionResult(result, correlationId = result.executionId)
}
