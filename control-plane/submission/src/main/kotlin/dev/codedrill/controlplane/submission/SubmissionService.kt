package dev.codedrill.controlplane.submission

import com.fasterxml.jackson.databind.ObjectMapper
import dev.codedrill.judge.protocol.JudgeCompleted
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.SubmissionQueued
import dev.codedrill.platform.common.IdempotencyKey
import dev.codedrill.platform.messaging.OutboxEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 제출 생성과 종료 (기술 설계서 §4.1 1~2단계, §3.2).
 *
 * 생성은 **하나의 트랜잭션**에서 제출 행과 아웃박스 이벤트를 함께 커밋한다. 발행은
 * 커밋 이후 퍼블리셔가 맡는다. 이 순서가 깨지면 브로커 장애 때 제출이 조용히 사라진다.
 */
@Service
class SubmissionService(
    private val repository: SubmissionRepository,
    private val json: ObjectMapper,
) {

    /**
     * 제출을 만든다. 같은 `(userId, idempotencyKey)` 로 다시 부르면 새로 만들지 않고
     * 기존 제출을 그대로 돌려준다 (§4.3 제출 버튼 중복).
     */
    @Transactional
    fun create(command: CreateSubmission): Submission {
        val key = IdempotencyKey(command.idempotencyKey)
        val id = UUID.randomUUID()
        val correlationId = UUID.randomUUID().toString()

        val submission = Submission(
            id = id,
            userId = command.userId,
            idempotencyKey = key.value,
            problemId = command.problemId,
            problemVersion = command.problemVersion,
            language = command.language.name,
            status = SubmissionStatus.CREATED,
        )

        val queued = SubmissionQueued(
            submissionId = id.toString(),
            correlationId = correlationId,
            problemId = command.problemId,
            problemVersion = command.problemVersion,
            language = command.language,
            source = command.source,
        )

        val inserted = repository.insertWithOutbox(
            submission,
            OutboxEvent(
                id = UUID.randomUUID(),
                aggregate = "submission",
                aggregateId = id.toString(),
                type = "SubmissionQueued",
                payload = json.writeValueAsString(queued),
                occurredAt = Instant.now(),
            ),
        )

        if (inserted == 0) {
            return requireNotNull(repository.findByIdempotencyKey(command.userId, key.value)) {
                "멱등 충돌인데 기존 제출을 찾지 못했다"
            }
        }

        repository.updateSource(id, command.source)
        // CREATED → QUEUED. 아웃박스에 이벤트가 들어간 순간 큐에 오른 것으로 본다.
        repository.transition(id, SubmissionStatus.CREATED, SubmissionStatus.QUEUED, version = 0)
        return submission.copy(status = SubmissionStatus.QUEUED)
    }

    fun find(id: UUID): Submission? = repository.findById(id)

    fun recent(userId: String, limit: Int = 20): List<Submission> = repository.recentFor(userId, limit)

    /**
     * 채점 종료를 반영한다.
     *
     * 이미 종료된 제출은 건드리지 않는다. 종료 상태는 불변이고, 재채점은 새 revision 을
     * 만드는 별도 경로다 (§4.2).
     */
    @Transactional
    fun complete(message: JudgeCompleted): Boolean {
        val id = UUID.fromString(message.submissionId)
        val updated = repository.complete(
            id = id,
            verdict = message.verdict,
            score = message.score,
            compileLog = message.compileLog,
            groupsJson = json.writeValueAsString(message.groups),
        )
        return updated > 0
    }
}

data class CreateSubmission(
    val userId: String,
    val idempotencyKey: String,
    val problemId: String,
    val problemVersion: Int,
    val language: Language,
    val source: String,
)
