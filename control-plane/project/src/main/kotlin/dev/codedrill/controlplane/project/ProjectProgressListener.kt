package dev.codedrill.controlplane.project

import dev.codedrill.judge.protocol.JudgeProgressed
import dev.codedrill.judge.protocol.JudgeStatus
import dev.codedrill.judge.protocol.ProjectCompleted
import dev.codedrill.platform.messaging.JudgeQueues
import dev.codedrill.platform.observability.CorrelationIds
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * 프로젝트형 판정의 진행·종료 알림 (feature-roadmap 11단계). 두 핸들러 모두 멱등하다.
 *
 * 큐가 알고리즘 판정의 것과 다르다 — 한 큐에 두 종류의 종료 메시지를 섞으면 모르는 타입이
 * 도착한 순간 리스너가 그것을 dead 큐로 보내고, 그 사이 뒤의 메시지가 밀린다.
 */
@Component
@RabbitListener(queues = [JudgeQueues.PROJECT_PROGRESS])
class ProjectProgressListener(private val service: ProjectService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @RabbitHandler
    fun onProgress(message: JudgeProgressed) {
        if (message.status == JudgeStatus.LEASED) service.leased(UUID.fromString(message.submissionId))
    }

    @RabbitHandler
    fun onCompleted(message: ProjectCompleted) {
        if (!service.complete(message)) {
            log.atInfo()
                .addKeyValue(CorrelationIds.SUBMISSION_ID, message.submissionId)
                .log("이미 종료된 프로젝트 제출이다. no-op")
        }
    }
}
