package dev.codedrill.controlplane.submission.trace

import com.fasterxml.jackson.databind.ObjectMapper
import dev.codedrill.controlplane.submission.SubmissionRepository
import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.TraceEvent
import dev.codedrill.judge.protocol.TraceReady
import dev.codedrill.judge.protocol.TraceStatus
import dev.codedrill.platform.messaging.OutboxEvent
import dev.codedrill.platform.problempackage.ProblemPackage
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 최초 분기 진단 (PRD FR-805).
 *
 * 사용자의 트레이스가 도착하면 **같은 케이스를 참조 풀이로 돌린 지문**과 견줘 처음 갈라진
 * 자리를 찾는다.
 *
 * 참조 지문은 (문제 버전, 케이스) 의 함수라 **한 번만 계산한다.** 제출마다 참조를 다시
 * 돌리면 채점 한 번에 실행이 하나씩 더 붙고, 그 값을 내는 것이 늘 같은 답이다.
 *
 * 지문이 아직 없으면 요청을 걸어 두고 `PENDING` 으로 남긴다. 지문이 도착하면 그것을
 * 기다리던 제출들을 마저 계산한다 — 기다린 사람이 다시 제출해야 답을 보는 구조는
 * 만들지 않는다.
 */
@Service
class DivergenceService(
    private val repository: DivergenceRepository,
    private val traces: TraceRepository,
    private val submissions: SubmissionRepository,
    private val packages: ProblemPackageLoader,
    private val json: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /** 사용자 트레이스가 준비됐다. */
    @Transactional
    fun traced(ready: TraceReady) {
        val submissionId = runCatching { UUID.fromString(ready.submissionId) }.getOrNull() ?: return
        if (ready.manifest.status != TraceStatus.READY) return

        val submission = submissions.findById(submissionId) ?: return
        val pkg = runCatching { packages.load(submission.problemId) }.getOrNull() ?: return
        val caseId = ready.manifest.caseId
        val events = ready.chunks.sortedBy { it.index }.flatMap { it.events }

        when (val signature = repository.signature(pkg.problemVersionId, caseId)) {
            null -> {
                // 아직 없다. 자리를 잡아 두고 참조 실행을 건다.
                repository.save(pending(submissionId, caseId))
                requestReference(pkg, caseId)
            }

            else -> repository.save(compare(submissionId, caseId, events, signature))
        }
    }

    /**
     * 참조 트레이스가 도착했다.
     *
     * 지문을 넣고, 이것을 기다리던 제출들을 마저 계산한다.
     */
    @Transactional
    fun referenceTraced(problemVersionId: String, caseId: String, events: List<TraceEvent>, empty: Boolean) {
        val status = if (empty) TraceStatus.EMPTY else TraceStatus.READY
        repository.putSignature(problemVersionId, caseId, events.map { it.contractKey() }, status)

        val signature = repository.signature(problemVersionId, caseId) ?: return
        for (submissionId in repository.pendingFor(caseId)) {
            val userEvents = userEventsOf(submissionId) ?: continue
            repository.save(compare(submissionId, caseId, userEvents, signature))
        }
    }

    fun find(userId: String, submissionId: UUID): Divergence? {
        // 남의 제출은 없는 것처럼 답한다 (§11.1).
        submissions.findById(submissionId)?.takeIf { it.userId == userId } ?: return null
        return repository.find(submissionId)
    }

    private fun compare(
        submissionId: UUID,
        caseId: String,
        events: List<TraceEvent>,
        signature: ReferenceSignature,
    ): Divergence {
        if (signature.status != TraceStatus.READY || signature.keys.isEmpty()) {
            return Divergence(
                submissionId, caseId, DivergenceOutcome.NO_REFERENCE,
                sharedPrefix = null, divergedAtSeq = null, sourceLine = null,
                expectedStep = null, actualStep = null,
            )
        }

        val point = FirstDivergence.of(events, signature.keys)
            ?: return Divergence(
                submissionId, caseId, DivergenceOutcome.SAME,
                sharedPrefix = events.size, divergedAtSeq = null, sourceLine = null,
                expectedStep = null, actualStep = null,
            )

        // 첫 이벤트부터 다르면 갈라진 것이 아니라 다른 길이다. 억지로 한 지점을 짚으면
        // 사용자는 멀쩡한 첫 줄을 의심한다.
        val different = point.index == 0

        return Divergence(
            submissionId = submissionId,
            caseId = caseId,
            outcome = if (different) {
                DivergenceOutcome.DIFFERENT_APPROACH
            } else {
                DivergenceOutcome.DIVERGED
            },
            sharedPrefix = point.index,
            divergedAtSeq = point.userEvent?.seq,
            sourceLine = point.userEvent?.sourceLine,
            // **다른 접근이면 참조가 무엇을 했는지 말하지 않는다.** 참조의 한 걸음을
            // 내보내는 유일한 명분은 "같이 오다가 여기서 갈렸다"를 설명하는 것인데,
            // 함께 온 길이 없으면 설명할 것도 없고 남는 것은 공짜로 새는 첫 수뿐이다.
            expectedStep = when {
                different -> null
                else -> point.referenceKey?.let(::describeKey) ?: "참조 풀이는 여기서 이미 끝났다"
            },
            actualStep = point.userEvent?.describe() ?: "내 풀이는 여기서 끝났다",
        )
    }

    /**
     * 정규화 키를 사람 말로 되돌린다.
     *
     * 키는 `TYPE|target|after` 다. 저장할 때 이벤트 본문을 버렸으므로 여기서 복원할 수
     * 있는 것도 그 셋뿐이고, **그것이 의도한 상한이다.**
     */
    private fun describeKey(key: String): String {
        val parts = key.split('|')
        val type = parts.getOrNull(0).orEmpty()
        val target = parts.getOrNull(1).orEmpty()
        val after = parts.getOrNull(2).orEmpty()
        return buildString {
            append(type)
            append(" [")
            append(target)
            append(']')
            if (after.isNotEmpty()) {
                append(" → ")
                append(after)
            }
        }
    }

    private fun pending(submissionId: UUID, caseId: String) = Divergence(
        submissionId, caseId, DivergenceOutcome.PENDING,
        sharedPrefix = null, divergedAtSeq = null, sourceLine = null,
        expectedStep = null, actualStep = null,
    )

    private fun userEventsOf(submissionId: UUID): List<TraceEvent>? {
        val manifest = traces.findManifest(submissionId) ?: return null
        return manifest.chunks
            .mapNotNull { chunk -> traces.findChunk(submissionId, chunk.index)?.events }
            .flatten()
    }

    /**
     * 참조 실행을 건다.
     *
     * 아웃박스로 내보낸다. 행과 메시지가 함께 커밋되지 않으면 지문 없이 `PENDING` 인
     * 제출이 영영 그대로 남는다 (§3.2).
     */
    private fun requestReference(pkg: ProblemPackage, caseId: String) {
        val reference = packages.referenceSolution(pkg.manifest.id)
        if (reference == null) {
            log.debug("참조 풀이가 없어 분기를 짚을 수 없다: {}", pkg.manifest.id)
            return
        }

        val groupId = caseId.substringBefore('/')
        val id = caseId.substringAfter('/')
        val group = pkg.groups.firstOrNull { it.policy.id == groupId } ?: return
        val case = group.cases.firstOrNull { it.id == id } ?: return

        val executionId = UUID.randomUUID().toString()
        submissions.enqueueOutbox(
            OutboxEvent(
                id = UUID.randomUUID(),
                aggregate = "reference-trace",
                // 같은 (문제 버전, 케이스) 로 두 번 걸려도 지문 삽입이 흡수한다.
                aggregateId = "${pkg.problemVersionId}/$caseId",
                type = REFERENCE_TRACE_EVENT,
                payload = json.writeValueAsString(
                    ExecutionRequest(
                        executionId = executionId,
                        // 제출이 아니다. 참조가 어느 (문제 버전, 케이스) 것인지를 봉투에
                        // 실어, 결과만 보고도 어디에 넣을지 알 수 있게 한다.
                        submissionId = "${pkg.problemVersionId}/$caseId",
                        attempt = 1,
                        fencingToken = FencingToken(1),
                        correlationId = executionId,
                        problemVersionId = pkg.problemVersionId,
                        packageDigest = pkg.packageDigest,
                        language = Language.KOTLIN,
                        source = reference,
                        signature = pkg.manifest.signature,
                        limits = pkg.manifest.limits,
                        groups = listOf(RequestedGroup(group.policy, listOf(case))),
                        mode = ExecutionMode.TRACE,
                    ),
                ),
                occurredAt = Instant.now(),
            ),
        )
    }

    companion object {
        const val REFERENCE_TRACE_EVENT = "ReferenceTraceRequested"
    }
}
