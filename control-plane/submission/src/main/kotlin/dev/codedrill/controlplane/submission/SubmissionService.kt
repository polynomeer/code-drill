package dev.codedrill.controlplane.submission

import com.fasterxml.jackson.databind.ObjectMapper
import dev.codedrill.judge.protocol.JudgeCompleted
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.SubmissionQueued
import dev.codedrill.platform.common.Cursor
import dev.codedrill.platform.common.IdempotencyKey
import dev.codedrill.platform.common.Page
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
    private val metrics: SubmissionMetrics,
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
            // 큐 대기 시간의 기준점. 아웃박스 행과 같은 트랜잭션에 들어가므로, 커밋되지
            // 않은 제출이 대기 시간에 섞이지 않는다 (§12.1 Queue wait).
            queuedAt = Instant.now(),
            correlationId = correlationId,
            problemId = command.problemId,
            problemVersion = command.problemVersion,
            language = command.language,
            source = command.source,
            requestTrace = command.requestTrace,
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
            metrics.created(command.language.name, idempotentHit = true)
            return requireNotNull(repository.findByIdempotencyKey(command.userId, key.value)) {
                "멱등 충돌인데 기존 제출을 찾지 못했다"
            }
        }
        metrics.created(command.language.name, idempotentHit = false)

        repository.updateSource(id, command.source)
        // CREATED → QUEUED. 아웃박스에 이벤트가 들어간 순간 큐에 오른 것으로 본다.
        repository.transition(id, SubmissionStatus.CREATED, SubmissionStatus.QUEUED, version = 0)
        return submission.copy(status = SubmissionStatus.QUEUED)
    }

    fun find(id: UUID): Submission? = repository.findById(id)

    /**
     * 제출 기록 한 페이지 (§9.1).
     *
     * 커서가 가리키는 항목 **다음**부터 [limit] 개를 읽는다. 커서는 권한을 담지 않으므로
     * 소유자 조건은 여기서 다시 건다.
     */
    fun history(userId: String, problemId: String?, cursor: String?, limit: Int?): Page<Submission> {
        val size = Cursor.limitOf(limit)
        val after = Cursor.decode(cursor)?.let { parts ->
            runCatching { Instant.parse(parts[0]) to UUID.fromString(parts[1]) }.getOrNull()
        }

        // 한 건 더 읽어 다음 페이지가 있는지 본다. count(*) 보다 싸고 정확하다.
        val rows = repository.page(userId, problemId, after, size + 1)
        val items = rows.take(size)
        val nextCursor = if (rows.size > size) {
            items.last().let { Cursor.encode(it.createdAt.toString(), it.id.toString()) }
        } else {
            null
        }
        return Page(items, nextCursor)
    }

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
    val requestTrace: Boolean = true,
)
