package dev.codedrill.platform.messaging

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.amqp.core.Declarables
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.boot.autoconfigure.AutoConfiguration

/**
 * 채점 경로 큐와 직렬화 설정.
 *
 * auto-configuration 으로 노출한다. 세 앱은 각자 다른 기본 패키지를 스캔하므로, 이
 * 설정을 컴포넌트 스캔에 기대면 의존성을 추가해도 큐가 선언되지 않는다.
 *
 * 큐는 durable + quorum 으로 선언한다. 브로커 노드가 죽어도 작업이 사라지면 안 된다
 * (§12.3 작업 유실 0).
 */
@AutoConfiguration
class AmqpConfig {

    // Declarables 로 감싸야 RabbitAdmin 이 선언 대상으로 인식한다. List<Queue> 빈은 무시된다.
    @Bean
    fun judgeQueues() = Declarables(
        JudgeQueues.all.map { QueueBuilder.durable(it).quorum().build() },
    )

    /**
     * 제출 이벤트 팬아웃 (§9.1).
     *
     * 큐를 여기서 만들지 않는다. 인스턴스마다 익명·배타·자동 삭제 큐를 붙이므로,
     * 인스턴스가 사라지면 그 큐도 함께 사라져야 한다 — 이름 있는 큐를 두면 죽은
     * 인스턴스의 큐에 이벤트가 영원히 쌓인다.
     */
    @Bean
    fun submissionEvents() = FanoutExchange(JudgeQueues.SUBMISSION_EVENTS, true, false)

    @Bean
    fun jsonMessageConverter(): MessageConverter =
        Jackson2JsonMessageConverter(
            ObjectMapper()
                .registerKotlinModule()
                // 메시지에 시간 타입이 늘어나도 런타임에 깨지지 않게 미리 등록한다.
                .registerModule(JavaTimeModule())
                // 소비자는 N/N-1 스키마를 함께 지원해야 한다. 새 필드가 생겼다고 구버전
                // 소비자가 메시지를 거절하면 배포 중에 채점이 멈춘다 (§15.3).
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES),
        ).apply {
            // 역직렬화 대상 패키지를 좁힌다. 브로커 메시지는 신뢰 경계를 넘어오므로,
            // 타입 헤더가 임의의 클래스를 지목하게 두면 안 된다 (§11.1 공급망).
            javaTypeMapper = DefaultJackson2JavaTypeMapper().apply {
                // 제어 영역 내부 팬아웃 메시지도 여기로 온다 (SubmissionEvent).
                setTrustedPackages("dev.codedrill.judge.protocol", "dev.codedrill.platform.messaging")
            }
        }

    @Bean
    fun rabbitTemplate(factory: ConnectionFactory, converter: MessageConverter) =
        RabbitTemplate(factory).apply { messageConverter = converter }
}
