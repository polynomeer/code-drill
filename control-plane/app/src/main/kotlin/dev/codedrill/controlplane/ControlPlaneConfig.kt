package dev.codedrill.controlplane

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.codedrill.judge.protocol.SubmissionQueued
import dev.codedrill.platform.messaging.JudgeQueues
import dev.codedrill.controlplane.outbox.OutboxRoute
import dev.codedrill.controlplane.outbox.OutboxRoutes
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import java.nio.file.Path

@Configuration
@EnableScheduling
class ControlPlaneConfig {

    // ObjectMapper 는 Spring Boot 가 만든 것을 그대로 쓴다. 여기서 새로 만들면 Boot 가
    // 등록해 주는 JavaTimeModule 이 빠져 Instant 직렬화가 런타임에 깨진다.

    @Bean
    fun problemPackageLoader(@Value("\${codedrill.content.root}") root: String) =
        ProblemPackageLoader(Path.of(root))

    /**
     * 아웃박스 이벤트를 어느 큐로, 어떤 타입으로 보낼지 (§3.3).
     *
     * 새 이벤트 타입을 추가하면 여기에도 등록해야 발행된다. 등록되지 않은 타입은
     * 퍼블리셔가 경고를 남기고 건너뛴다.
     */
    @Bean
    fun outboxRoutes(mapper: ObjectMapper) = object : OutboxRoutes {
        override fun routeFor(type: String): OutboxRoute? = when (type) {
            "SubmissionQueued" -> OutboxRoute(JudgeQueues.SUBMISSIONS) {
                mapper.readValue<SubmissionQueued>(it)
            }
            else -> null
        }
    }
}
