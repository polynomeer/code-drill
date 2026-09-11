package dev.codedrill.controlplane.submission.lab

import dev.codedrill.judge.protocol.LabReport
import dev.codedrill.platform.messaging.JudgeQueues
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

/** 실험실 결과 수신 (§6.4~6.6). */
@Component
class LabResultListener(private val service: LabService) {

    @RabbitListener(queues = [JudgeQueues.LAB_RESULTS])
    fun onReport(report: LabReport) = service.completed(report)
}
