package dev.codedrill.controlplane.workspace

import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.platform.messaging.JudgeQueues
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * 시험 실행 결과 수신 (기획서 부록 A 실행 도메인).
 *
 * 판정 결과와 다른 큐에서 받는다. 여기 오는 것은 판정이 아니므로 제출 상태를 건드리지
 * 않고, 역량 증거도 만들지 않는다.
 */
@Component
class TrialResultListener(
    private val repository: TrialRepository,
    private val learning: LearningSignals = LearningSignals.NONE,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @RabbitListener(queues = [JudgeQueues.TRIAL_RESULTS])
    fun onResult(result: ExecutionResult) {
        val id = runCatching { UUID.fromString(result.executionId) }.getOrNull()
        if (id == null) {
            log.warn("실행 id 를 읽지 못했다: {}", result.executionId)
            return
        }

        // 케이스를 하나도 돌리지 못한 경우다 — 컴파일 실패나 플랫폼 오류. 사용자에게는
        // "돌지 않았다"와 "돌았는데 틀렸다"가 전혀 다른 사실이므로 상태로 가른다.
        val status = if (result.cases.isEmpty()) TrialStatus.FAILED else TrialStatus.COMPLETED

        val cases = result.cases.mapIndexed { index, case ->
            TrialCaseResult(
                index = index,
                outcome = case.verdict.name,
                actual = case.actual,
                message = case.message,
                wallTimeMillis = case.measurements.wallTimeMillis,
                peakMemoryBytes = case.measurements.peakMemoryBytes,
            )
        }

        val updated = repository.complete(id, status, result.compileLog, cases)
        if (updated == 0) {
            // 아웃박스는 at-least-once 다. 같은 결과가 두 번 오는 것은 정상이며 오류가 아니다.
            log.debug("이미 끝난 시험 실행이다: {}", id)
            return
        }

        // 여기서 재는 것은 코드가 맞았는지가 아니라 **무엇을 시험해야 하는지 아는가**다
        // (§8.2 테스트 설계). 기대 출력을 적은 케이스만 센다.
        val trial = repository.find(id) ?: return
        runCatching {
            learning.tested(
                userId = trial.userId,
                problemId = trial.problemId,
                trialId = id.toString(),
                judgedCases = trial.cases.count { it.expected != null },
            )
        }.onFailure { log.warn("학습 기록에 남기지 못했다: {} ({})", id, it.message) }
    }
}
