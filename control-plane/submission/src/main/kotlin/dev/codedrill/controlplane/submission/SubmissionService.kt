package dev.codedrill.controlplane.submission

import com.fasterxml.jackson.databind.ObjectMapper
import dev.codedrill.judge.protocol.JudgeCompleted
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.SubmissionQueued
import dev.codedrill.platform.common.Cursor
import dev.codedrill.platform.common.IdempotencyKey
import dev.codedrill.platform.common.Page
import dev.codedrill.platform.messaging.OutboxEvent
import org.slf4j.LoggerFactory
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
    private val quota: SubmissionQuota,
    private val rejudges: RejudgeContext = RejudgeContext.NONE,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 제출을 만든다. 같은 `(userId, idempotencyKey)` 로 다시 부르면 새로 만들지 않고
     * 기존 제출을 그대로 돌려준다 (§4.3 제출 버튼 중복).
     */
    @Transactional
    fun create(command: CreateSubmission): Submission {
        val key = IdempotencyKey(command.idempotencyKey)

        // 쿼터를 먼저 본다. 다만 **같은 키로 다시 온 요청은 막지 않는다** — 그것은 새
        // 제출이 아니라 이미 받은 제출을 다시 묻는 것이고, 네트워크가 흔들려 재시도한
        // 클라이언트를 쿼터로 막으면 멱등성이 있으나 마나다 (§4.3).
        quota.exceededBy(command.userId)?.let { reason ->
            repository.findByIdempotencyKey(command.userId, key.value)?.let { return it }
            metrics.quotaRejected(command.language.name)
            throw QuotaExceededException(reason)
        }

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
     * 채점 종료를 반영한다 (§4.2 INV-02).
     *
     * 최초 판정과 재채점 결과가 같은 경로를 지난다. 갈리는 것은 **현재 판정을 갈아
     * 끼우는지**뿐이다.
     *
     * - 최초 판정: CREATED~AGGREGATING → COMPLETED 로 옮기고 판정을 채운다.
     * - 재채점: 이미 COMPLETED 인 행의 판정만 갈아 끼우고 revision 을 올린다. 상태는
     *   그대로다 — 바뀌는 것은 "무엇으로 판정됐는가"이지 "끝났는가"가 아니다.
     * - dry-run 재채점: 이력에만 남기고 현재 판정은 건드리지 않는다.
     *
     * 어느 경우든 **이력을 먼저 남긴다.** 이력 삽입이 `execution_id` 로 중복을
     * 걸러 주므로, 같은 결과가 다시 와도 revision 이 헛되이 오르지 않는다 (§4.3).
     */
    @Transactional
    fun complete(message: JudgeCompleted): Boolean {
        val id = UUID.fromString(message.submissionId)
        val current = repository.findById(id) ?: return false
        val groupsJson = json.writeValueAsString(message.groups)

        val pending = rejudges.pendingFor(id)

        // **판정은 재채점으로만 바뀐다** (§4.2, §11.2).
        //
        // 이미 끝난 제출에 다른 결과가 오는 길은 둘이다. 승인된 재채점이거나, 뒤늦게
        // 도착한 낡은 실행 결과거나. 앞의 것만 판정을 움직여야 한다 — 뒤의 것까지
        // 반영하면 아무도 승인하지 않은 판정 변경이 생기고, 그 순간 2인 승인은 우회된다.
        //
        // 낡은 결과도 이력에는 남긴다. 무슨 일이 있었는지는 남아야 한다 (§13.3).
        val late = current.status == SubmissionStatus.COMPLETED && pending == null
        val apply = pending?.dryRun != true && !late

        // 이력의 revision 은 이 판정을 반영한 **뒤** 제출이 갖게 될 값이다. 최초 판정은
        // 제출을 만들 때 이미 revision 1 이므로 올리지 않는다 — 올리면 아무도 재채점하지
        // 않았는데 이력만 2 부터 시작한다.
        val revised = apply && current.status == SubmissionStatus.COMPLETED

        val recorded = repository.recordJudgement(
            id = id,
            revision = if (revised) current.revision + 1 else current.revision,
            executionId = message.executionId,
            verdict = message.verdict,
            score = message.score,
            compileLog = message.compileLog,
            groupsJson = groupsJson,
            rejudgeJobId = pending?.jobId,
            applied = apply,
        )
        // 이미 기록된 실행이다. 여기서 멈춰야 중복 전달이 revision 을 올리지 못한다.
        if (recorded == 0) return false

        if (late) {
            log.warn(
                "끝난 제출에 뒤늦은 결과가 왔다. 이력에만 남기고 판정은 그대로 둔다: {} ({})",
                id, message.executionId,
            )
            return false
        }

        val updated = when {
            !apply -> 0
            revised -> repository.revise(
                id, message.verdict, message.score, message.compileLog, groupsJson,
            )
            else -> repository.complete(
                id, message.verdict, message.score, message.compileLog, groupsJson,
            )
        }

        pending?.let { job ->
            rejudges.judged(
                RejudgeContext.Outcome(
                    submissionId = id,
                    jobId = job.jobId,
                    applied = apply,
                    previousVerdict = current.verdict?.name,
                    previousScore = current.score,
                    verdict = message.verdict.name,
                    score = message.score,
                ),
            )
        }

        // dry-run 은 현재 판정을 바꾸지 않았으므로 SSE 로 알릴 것도 없다.
        return updated > 0
    }

    /** 제출 하나의 판정 이력 (§4.2). 최초 판정부터 전부 들어 있다. */
    fun judgements(id: UUID): List<Judgement> = repository.judgements(id)

    /** 재채점 대상 산출 (§3.1). 제출 테이블을 아는 쪽이 답한다. */
    fun completedFor(problemId: String): List<UUID> = repository.completedIds(problemId)

    fun completed(submissionId: UUID): List<UUID> = repository.completedId(submissionId)

    /**
     * 종료된 제출을 다시 채점 큐에 올린다 (§3.2).
     *
     * 제출 행을 새로 만들지 않는다. 재채점은 같은 제출의 다음 revision 이지 새 제출이
     * 아니며, 새로 만들면 사용자의 기록에 자기가 하지 않은 제출이 늘어난다.
     */
    @Transactional
    fun requeue(ids: List<UUID>): Int = ids.count { id ->
        val submission = repository.findById(id) ?: return@count false
        val source = repository.findSource(id) ?: return@count false

        repository.enqueueOutbox(
            OutboxEvent(
                id = UUID.randomUUID(),
                aggregate = "submission",
                aggregateId = id.toString(),
                type = "SubmissionQueued",
                payload = json.writeValueAsString(
                    SubmissionQueued(
                        submissionId = id.toString(),
                        correlationId = UUID.randomUUID().toString(),
                        queuedAt = Instant.now(),
                        problemId = submission.problemId,
                        problemVersion = submission.problemVersion,
                        language = Language.valueOf(submission.language),
                        source = source,
                        // 재채점은 판정을 다시 내는 일이다. 학습용 트레이스까지 다시
                        // 만들면 Runner 용량의 절반이 거기로 간다 (§7.1).
                        requestTrace = false,
                    ),
                ),
                occurredAt = Instant.now(),
            ),
        )
        true
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
