package dev.codedrill.judge.runner.messaging

import dev.codedrill.judge.protocol.ExecutionHeartbeat
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.runner.execution.ExecutionEngine
import dev.codedrill.platform.messaging.JudgeQueues
import dev.codedrill.platform.observability.CorrelationIds
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 실행 요청 수신 지점 (기술 설계서 §4.1 5~6단계).
 *
 * Runner 는 결과를 브로커로만 돌려보낸다. Control Plane 자격증명도, DB 연결도 갖지
 * 않는다 (§2.3, §5.1).
 *
 * 실행하는 동안 심장 박동을 보낸다. 이것이 없으면 오케스트레이터는 "아직 안 왔다"와
 * "워커가 죽었다"를 구분할 수 없어, 브로커에 밀려 늦어진 실행을 유실로 오해한다 (§4.3).
 */
@Component
class ExecutionListener(
    private val engine: ExecutionEngine,
    private val rabbit: RabbitTemplate,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 심장 박동 전용 스레드.
     *
     * 실행 스레드에서 보낼 수는 없다 — 실행은 샌드박스가 끝나기를 막고 기다리므로,
     * 그 사이에는 아무것도 보내지 못한다. 그리고 그 "그 사이"가 정확히 임대가 만료되는
     * 구간이다.
     */
    private val heartbeats = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "execution-heartbeat").apply { isDaemon = true }
    }

    @RabbitListener(queues = [JudgeQueues.EXECUTIONS])
    fun onExecution(request: ExecutionRequest) {
        log.atInfo()
            .addKeyValue(CorrelationIds.SUBMISSION_ID, request.submissionId)
            .addKeyValue(CorrelationIds.EXECUTION_ID, request.executionId)
            .addKeyValue(CorrelationIds.ATTEMPT, request.attempt)
            .log("실행을 시작한다")

        val beat = ExecutionHeartbeat(
            submissionId = request.submissionId,
            executionId = request.executionId,
            attempt = request.attempt,
            fencingToken = request.fencingToken,
        )
        // 집어 들자마자 한 번 보낸다. 첫 박동을 주기만큼 미루면 그 사이에 만료될 수 있다.
        send(beat)
        val ticking = heartbeats.scheduleAtFixedRate(
            { send(beat) }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS,
        )

        try {
            // 엔진은 자기 안에서 일어난 모든 실패를 판정으로 바꿔 돌려준다. 여기서 예외가
            // 새어나가면 브로커가 재전달하고, 같은 실패를 반복하게 된다.
            val result = engine.execute(request)
            rabbit.convertAndSend(JudgeQueues.RESULTS, result)
        } finally {
            ticking.cancel(false)
        }
    }

    /**
     * 심장 박동 발행 실패는 삼킨다.
     *
     * 브로커가 흔들려 박동을 못 보내면 임대가 만료되고 실행이 다시 걸린다 — 그것이
     * 의도한 동작이다. 여기서 예외를 올리면 실행 자체가 죽어, 잃지 않아도 될 판정을
     * 잃는다.
     */
    private fun send(beat: ExecutionHeartbeat) {
        runCatching { rabbit.convertAndSend(JudgeQueues.HEARTBEATS, beat) }
            .onFailure { log.debug("심장 박동을 보내지 못했다: {}", it.message) }
    }

    private companion object {
        /**
         * 박동 주기.
         *
         * 임대 기간보다 충분히 짧아야 한다. 운영에서 임대를 이 값의 두 배 아래로 줄이면
         * 정상 실행이 만료되기 시작한다 (docs/runbook.md#worker-loss).
         */
        const val HEARTBEAT_INTERVAL_SECONDS = 5L
    }
}
