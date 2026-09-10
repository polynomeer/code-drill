package dev.codedrill.controlplane.workspace

import dev.codedrill.judge.protocol.MutationReport
import dev.codedrill.judge.protocol.MutationStatus
import dev.codedrill.platform.messaging.JudgeQueues
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * 변이 평가 결과 수신 (FR-804).
 *
 * 시험 실행과 달리 **역량 증거를 남긴다.** 시험 실행에서 알 수 있는 것은 "기대 출력을
 * 적었다"까지이고, 여기서는 그 기대가 실제로 결함을 잡았는지까지 나온다 — 후자가
 * 검증 역량군이 재려던 것이다 (§4.2).
 */
@Component
class MutationReportListener(
    private val repository: MutationRepository,
    private val learning: LearningSignals = LearningSignals.NONE,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @RabbitListener(queues = [JudgeQueues.MUTATION_RESULTS])
    fun onReport(report: MutationReport) {
        val id = runCatching { UUID.fromString(report.evaluationId) }.getOrNull()
        if (id == null) {
            log.warn("평가 id 를 읽지 못했다: {}", report.evaluationId)
            return
        }

        val updated = repository.complete(
            id,
            MutationRunStatus.of(report.status),
            report.message,
            report.mistakenCases,
            report.mutants,
        )
        if (updated == 0) {
            // 아웃박스는 at-least-once 다. 같은 결과가 두 번 오는 것은 정상이다.
            log.debug("이미 끝난 변이 평가다: {}", id)
            return
        }

        // 잴 것이 없었으면 증거도 없다. NO_CASES 를 "전부 놓쳤다"로 세면 아무것도 재지
        // 않은 사람의 숙련도가 내려간다.
        if (report.status != MutationStatus.COMPLETED) return

        val evaluation = repository.find(id) ?: return
        runCatching {
            learning.mutationChecked(
                userId = evaluation.userId,
                problemId = evaluation.problemId,
                evaluationId = id.toString(),
                killedByKind = report.mutants
                    .groupBy { it.kind }
                    .mapValues { (_, rows) -> rows.count { it.killed } to rows.size },
            )
        }.onFailure { log.warn("학습 기록에 남기지 못했다: {} ({})", id, it.message) }
    }
}
