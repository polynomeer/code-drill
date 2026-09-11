package dev.codedrill.controlplane.workspace

import dev.codedrill.judge.protocol.ArenaReport
import dev.codedrill.platform.messaging.JudgeQueues
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

/** 아레나 결과 수신 (§8.3). */
@Component
class ArenaResultListener(private val service: ArenaService) {

    @RabbitListener(queues = [JudgeQueues.ARENA_RESULTS])
    fun onReport(report: ArenaReport) = service.completed(report)
}
