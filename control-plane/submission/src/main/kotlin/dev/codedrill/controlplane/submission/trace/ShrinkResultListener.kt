package dev.codedrill.controlplane.submission.trace

import dev.codedrill.judge.protocol.ShrinkReport
import dev.codedrill.platform.messaging.JudgeQueues
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

/** 반례 축소 결과 수신 (§6.3). */
@Component
class ShrinkResultListener(private val service: CounterexampleService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @RabbitListener(queues = [JudgeQueues.SHRINK_RESULTS])
    fun onReport(report: ShrinkReport) {
        service.completed(report)
        log.atInfo()
            .addKeyValue("shrinkId", report.shrinkId)
            .log("반례 축소: {} ({}→{}, {}라운드)", report.status, report.originalSize, report.minimalSize, report.rounds)
    }
}
