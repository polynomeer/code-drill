package dev.codedrill.judge.runner.messaging

import dev.codedrill.judge.protocol.ExecutionHeartbeat
import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.LabRequest
import dev.codedrill.judge.protocol.MutationRequest
import dev.codedrill.judge.protocol.ShrinkRequest
import dev.codedrill.judge.runner.execution.ExecutionEngine
import dev.codedrill.judge.runner.execution.LabRunner
import dev.codedrill.judge.runner.execution.MutationEvaluator
import dev.codedrill.judge.runner.execution.Shrinker
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
    private val mutations: MutationEvaluator,
    private val shrinker: Shrinker,
    private val lab: LabRunner,
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

    /**
     * 판정과 시험 실행을 **한 리스너로** 받는다.
     *
     * 큐는 나뉘어 있다 — 시험 실행이 몰려도 채점이 그 뒤에 줄 서지 않게 하려는 것이다.
     * 그런데 리스너까지 나누면 둘이 같은 머신에서 동시에 돌고, 그러면 서로의 CPU 와
     * wall time 을 오염시킨다 (§5.2). 한 리스너에 두 큐를 걸면 브로커가 둘을 번갈아
     * 주면서도 한 번에 하나만 돈다.
     */
    @RabbitListener(queues = [JudgeQueues.EXECUTIONS, JudgeQueues.TRIALS])
    fun onExecution(request: ExecutionRequest) {
        if (request.mode == ExecutionMode.TRIAL) return runTrial(request)

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
     * 시험 실행 (기획서 부록 A 실행 도메인).
     *
     * 심장 박동을 보내지 않는다. 박동은 오케스트레이터가 임대 만료로 워커 유실을 잡기
     * 위한 것인데, 시험 실행에는 임대가 없다 — 잃어버리면 사용자가 다시 누른다.
     *
     * 엔진은 같은 것을 쓴다. **사용자가 자기 입력으로 본 결과와 채점이 본 결과가 다르면
     * 시험 실행은 쓸모가 없으므로**, 실행 경로를 따로 두지 않는다.
     */
    private fun runTrial(request: ExecutionRequest) {
        log.atInfo()
            .addKeyValue(CorrelationIds.EXECUTION_ID, request.executionId)
            .log("시험 실행을 시작한다")

        val result = engine.execute(request)
        rabbit.convertAndSend(JudgeQueues.TRIAL_RESULTS, result)
    }

    /**
     * 참조 풀이 계측 실행 (FR-805).
     *
     * 시험 실행과 같은 모양이다 — 심장 박동도 임대도 없고, 잃어버리면 제어 영역이 다시
     * 요청한다. 다른 것은 여기 실린 소스가 **저작자의 정답**이라는 것뿐이며, Runner 는
     * 그 사실을 알 필요가 없다.
     */
    @RabbitListener(queues = [JudgeQueues.REFERENCE_TRACES])
    fun onReferenceTrace(request: ExecutionRequest) {
        log.atInfo()
            .addKeyValue(CorrelationIds.EXECUTION_ID, request.executionId)
            .log("참조 트레이스를 시작한다")

        rabbit.convertAndSend(JudgeQueues.REFERENCE_TRACE_RESULTS, engine.execute(request))
    }

    /**
     * 변이 평가 (PRD FR-804).
     *
     * **큐는 따로, 리스너는 같이.** 한 건이 정답 한 번 + 오답 N 번을 돌려 다른 어떤
     * 작업보다 오래 걸리므로 큐를 나눠 뒤를 막지 않게 하고, 리스너를 나누지 않아
     * 채점과 동시에 돌지 않게 한다 (§5.2).
     *
     * 심장 박동도 임대도 없다. 잃어버리면 사용자가 다시 누른다 — 시험 실행과 같다.
     */
    @RabbitListener(queues = [JudgeQueues.MUTATIONS])
    fun onMutation(request: MutationRequest) {
        log.atInfo()
            .addKeyValue(CorrelationIds.EXECUTION_ID, request.evaluationId)
            .log("변이 평가를 시작한다: 오답 {}개, 케이스 {}건", request.mutants.size, request.cases.size)

        rabbit.convertAndSend(JudgeQueues.MUTATION_RESULTS, mutations.evaluate(request))
    }

    /**
     * 최소 반례 축소 (§6.3).
     *
     * 이 시스템에서 가장 오래 걸리는 작업이다 — 라운드마다 컴파일 두 번에 후보 수십 개를
     * 돌린다. 그래서 큐를 따로 두고, 그래도 리스너는 같이 쓴다: 채점과 동시에 돌면 둘 다
     * 느려지고 측정까지 오염된다 (§5.2).
     */
    @RabbitListener(queues = [JudgeQueues.SHRINKS])
    fun onShrink(request: ShrinkRequest) {
        log.atInfo()
            .addKeyValue(CorrelationIds.EXECUTION_ID, request.shrinkId)
            .log("반례 축소를 시작한다")

        rabbit.convertAndSend(JudgeQueues.SHRINK_RESULTS, shrinker.shrink(request))
    }

    /**
     * 실험실 (§6.4~6.6). 풀이 수만큼 계측 실행이다. 큐는 따로, 리스너는 같이.
     */
    @RabbitListener(queues = [JudgeQueues.LABS])
    fun onLab(request: LabRequest) {
        log.atInfo()
            .addKeyValue(CorrelationIds.EXECUTION_ID, request.labId)
            .log("실험실 실행을 시작한다: 풀이 {}개", request.approaches.size)

        rabbit.convertAndSend(JudgeQueues.LAB_RESULTS, lab.run(request))
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
