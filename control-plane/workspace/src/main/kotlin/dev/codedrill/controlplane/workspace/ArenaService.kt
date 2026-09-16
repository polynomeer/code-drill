package dev.codedrill.controlplane.workspace

import com.fasterxml.jackson.databind.ObjectMapper
import dev.codedrill.judge.protocol.ArenaReport
import dev.codedrill.judge.protocol.ArenaRequest
import dev.codedrill.platform.messaging.OutboxEvent
import dev.codedrill.platform.problempackage.DefectKind
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * 반례 아레나 (기획서 §8.3).
 *
 * > 익명화된 오답을 실패시키는 입력을 작성합니다. 성공한 반례를 자동 축소하고 깨뜨린
 * > 가정을 분류합니다. 작은 반례, 새로운 버그 유형, 많은 제출을 깨는 반례를 별도 평가합니다.
 *
 * 세 평가 축이 그대로 기록판이다 — 가장 작은 반례(smallest), 처음 깨뜨림(first), 한 입력이
 * 깨뜨린 오답 수(한 시도의 결과). "깨뜨린 가정"은 오답의 결함군이다.
 *
 * **맞힌 사람에게만 연다.** 오답의 소스가 보이는데, 오답은 정답에서 한 곳만 다른 코드라
 * 그것을 보는 것은 정답을 보는 것과 거의 같다.
 *
 * **과녁은 두 출처다.** 저작자의 대표 오답과, 검수를 거쳐 세운 남의 오답
 * ([CommunityMutantService]). 여기서는 둘을 구분하지 않고 같은 과녁으로 돌린다 —
 * 다른 것은 화면에서 "누가 세운 것인가"뿐이다.
 */
@Service
class ArenaService(
    private val repository: ArenaRepository,
    private val packages: ProblemPackageLoader,
    private val json: ObjectMapper,
    private val community: CommunityMutantService,
    private val gate: ArenaGate = ArenaGate.CLOSED,
    private val learning: LearningSignals = LearningSignals.NONE,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /** 깨뜨릴 오답들과 기록판. 잠겨 있으면 오답 없이 잠겼다는 사실만. */
    fun board(userId: String, problemId: String): ArenaBoard {
        val open = gate.solved(userId, problemId)
        val authored = if (!open) emptyList() else packages.mutants(problemId)
            // 성능 오답은 뺀다. 손으로 적는 입력으로는 깨뜨릴 수 없다 (DefectKind.PERFORMANCE).
            .filter { it.kind.reachableByHandWrittenCase }
            .map { ArenaTarget(it.name, it.kind, it.kind.label, it.note.substringBefore(". ").trimEnd('.') + ".", it.source, community = false) }
        val donated = if (!open) emptyList() else community.targets(problemId)
            .map { ArenaTarget(it.name, it.kind, it.kind.label, it.note, it.source, community = true) }
        val targets = authored + donated
        return ArenaBoard(
            problemId = problemId,
            locked = !open,
            targets = targets,
            records = if (open) repository.records(problemId, userId) else emptyList(),
        )
    }

    @Transactional
    fun attempt(userId: String, problemId: String, args: List<Any>): Outcome {
        if (!gate.solved(userId, problemId)) return Outcome.Locked

        val used = repository.recentCount(userId, Instant.now().minus(WINDOW))
        if (used >= PER_HOUR) return Outcome.Throttled(used, PER_HOUR)

        val pkg = runCatching { packages.load(problemId) }.getOrNull() ?: return Outcome.Invalid("문제를 읽지 못했다")
        val parameters = pkg.manifest.signature.parameters
        CaseShape.mismatch(args, parameters.map { it.type })?.let { return Outcome.Invalid(it) }

        val reference = packages.referenceSolution(problemId) ?: return Outcome.Invalid("대조할 정답이 없다")
        val mutants = packages.mutants(problemId).filter { it.kind.reachableByHandWrittenCase } + community.targets(problemId)
        if (mutants.isEmpty()) return Outcome.Invalid("깨뜨릴 오답이 없다")

        val id = UUID.randomUUID()
        val attempt = ArenaAttempt(id, userId, problemId, args, ArenaAttemptStatus.PENDING, null, emptyList(), Instant.now())
        repository.insertWithOutbox(
            attempt,
            OutboxEvent(
                id = UUID.randomUUID(),
                aggregate = "arena",
                aggregateId = id.toString(),
                type = ARENA_EVENT,
                payload = json.writeValueAsString(
                    ArenaRequest(
                        attemptId = id.toString(),
                        correlationId = id.toString(),
                        problemVersionId = pkg.problemVersionId,
                        packageDigest = pkg.packageDigest,
                        signature = pkg.manifest.signature,
                        limits = pkg.manifest.limits,
                        reference = reference,
                        mutants = mutants,
                        args = args,
                    ),
                ),
                occurredAt = Instant.now(),
            ),
        )
        return Outcome.Started(attempt)
    }

    @Transactional
    fun completed(report: ArenaReport) {
        val id = runCatching { UUID.fromString(report.attemptId) }.getOrNull() ?: return
        val updated = repository.complete(id, ArenaAttemptStatus.of(report.status), report.message, report.results)
        if (updated == 0) return

        val attempt = repository.find(id) ?: return
        val broken = report.results.filter { it.broken }
        for (result in broken) {
            repository.recordBreak(attempt.problemId, result.name, attempt.userId, result.minimalSize ?: sizeOf(attempt.args))
        }

        // 반례 역량의 증거 (§8.2 "깨뜨린 제출과 반례 품질"). 전제 밖 입력은 시도가 아니다.
        if (report.status != dev.codedrill.judge.protocol.ArenaStatus.COMPLETED) return
        runCatching {
            learning.brokeMutants(
                userId = attempt.userId,
                problemId = attempt.problemId,
                attemptId = id.toString(),
                broken = broken.size,
                total = report.results.size,
                kinds = broken.map { it.kind }.distinct(),
                targets = broken.map { it.name },
                at = attempt.createdAt,
            )
        }.onFailure { log.warn("아레나 결과를 학습 기록에 남기지 못했다: {} ({})", id, it.message) }
    }

    fun find(userId: String, id: UUID): ArenaAttempt? = repository.find(id)?.takeIf { it.userId == userId }

    private fun sizeOf(args: List<Any>) = args.sumOf { (it as? List<*>)?.size ?: 0 }

    sealed interface Outcome {
        data class Started(val attempt: ArenaAttempt) : Outcome
        data object Locked : Outcome
        data class Invalid(val reason: String) : Outcome
        data class Throttled(val used: Int, val allowed: Int) : Outcome
    }

    companion object {
        const val ARENA_EVENT = "ArenaAttempted"

        /** 한 시간에 몇 번까지. 시도마다 오답 수만큼 실행에 깨뜨린 만큼의 축소가 붙는다. */
        private const val PER_HOUR = 30
        private val WINDOW: Duration = Duration.ofHours(1)
    }
}

/** 이 문제를 맞혔나 (§3.1 조립 지점). 아레나의 잠금이다. */
fun interface ArenaGate {
    fun solved(userId: String, problemId: String): Boolean

    companion object {
        val CLOSED = ArenaGate { _, _ -> false }
    }
}

data class ArenaBoard(
    val problemId: String,
    val locked: Boolean,
    val targets: List<ArenaTarget>,
    val records: List<ArenaRecord>,
)

/** 깨뜨릴 오답. 소스가 보인다 — 맞힌 사람에게만 나가는 응답이다. */
data class ArenaTarget(
    val name: String,
    val kind: DefectKind,
    val kindLabel: String,
    /** 무엇을 잘못하는지 한 줄. 힌트가 아니라 과녁의 설명이다. */
    val note: String,
    val source: String,
    /** 저작자의 대표 오답이 아니라 검수를 거쳐 세운 남의 오답인가 (§8.3). 신고는 이쪽에만 걸린다. */
    val community: Boolean,
)
