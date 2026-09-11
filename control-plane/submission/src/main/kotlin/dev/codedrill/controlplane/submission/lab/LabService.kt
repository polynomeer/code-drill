package dev.codedrill.controlplane.submission.lab

import com.fasterxml.jackson.databind.ObjectMapper
import dev.codedrill.controlplane.submission.SubmissionRepository
import dev.codedrill.judge.protocol.Approach
import dev.codedrill.judge.protocol.LabReport
import dev.codedrill.judge.protocol.LabRequest
import dev.codedrill.judge.protocol.Language
import dev.codedrill.platform.messaging.OutboxEvent
import dev.codedrill.platform.problempackage.DefectKind
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * 해설·비교·실험실 (기획서 §6.4~6.6, PRD FR-214).
 *
 * 셋이 하나의 요청이다 — **한 입력에 여러 풀이를 돌려 나란히 놓는 것.** 해설은 참조 풀이를
 * 사용자의 입력으로 다시 실행한 것이고, 비교 경기장은 거기에 내 풀이를 옆에 세운 것이며,
 * 실험실은 입력을 바꿔 가며 그것을 반복하는 것이다.
 *
 * **잠금은 여기서 하나로 지킨다.** 참조 풀이의 이벤트 열이 통째로 나가는 유일한 자리라,
 * "맞혔거나 명시적으로 열었거나"를 확인하는 코드가 두 곳이면 언젠가 한쪽만 고쳐진다.
 */
@Service
class LabService(
    private val repository: LabRepository,
    private val submissions: SubmissionRepository,
    private val packages: ProblemPackageLoader,
    private val json: ObjectMapper,
) {

    /**
     * 해설과 실험실 후보.
     *
     * 잠겨 있으면 본문 없이 잠겼다는 사실만 돌려준다. 잠금을 404 로 감추지 않는다 —
     * 해설이 있다는 것과 지금은 볼 수 없다는 것을 함께 알아야 "열기"를 고를 수 있다.
     */
    fun editorial(userId: String, problemId: String): EditorialView {
        val text = packages.editorial(problemId)
        val open = isOpen(userId, problemId)
        return EditorialView(
            problemId = problemId,
            available = text != null,
            locked = !open,
            solved = submissions.latestAccepted(userId, problemId) != null,
            body = if (open) text else null,
            approaches = if (open) approaches(problemId).map { it.label } else emptyList(),
        )
    }

    /**
     * 정답 전에 연다 (FR-214).
     *
     * 되돌릴 수 없다. 방법을 본 것은 본 것이고, 그 뒤의 제출은 그만큼 가벼운 증거가 된다.
     * 그 사실을 여는 쪽 화면이 미리 말한다.
     */
    @Transactional
    fun unlock(userId: String, problemId: String): EditorialView {
        repository.unlock(userId, problemId)
        return editorial(userId, problemId)
    }

    /** 정답 전에 해설을 열었나. 그 문제의 도움 단계를 정하는 조립 지점이 묻는다. */
    fun unlockedEarly(userId: String, problemId: String): Boolean = repository.unlocked(userId, problemId)

    @Transactional
    fun start(userId: String, problemId: String, args: List<Any>, labels: List<String>): Outcome {
        if (!isOpen(userId, problemId)) return Outcome.Locked
        if (labels.isEmpty()) return Outcome.Invalid("풀이를 하나 이상 골라야 한다")
        if (labels.size > LabRequest.MAX_APPROACHES) {
            return Outcome.Invalid("한 번에 ${LabRequest.MAX_APPROACHES}개까지 나란히 놓을 수 있다")
        }

        val used = repository.recentCount(userId, Instant.now().minus(WINDOW))
        if (used >= PER_HOUR) return Outcome.Throttled(used, PER_HOUR)

        val pkg = runCatching { packages.load(problemId) }.getOrNull()
            ?: return Outcome.Invalid("문제를 읽지 못했다")
        val parameters = pkg.manifest.signature.parameters
        if (args.size != parameters.size) {
            return Outcome.Invalid("인자 ${parameters.size}개가 필요한데 ${args.size}개다")
        }

        val available = approaches(problemId).associateBy { it.label }
        val mine = submissions.latestAccepted(userId, problemId)?.let { accepted ->
            submissions.findSource(accepted.id)?.let { source ->
                Approach(MINE, Language.valueOf(accepted.language), source)
            }
        }
        val chosen = labels.map { label ->
            when (label) {
                MINE -> mine ?: return Outcome.Invalid("아직 맞힌 풀이가 없어 '내 풀이'를 세울 수 없다")
                else -> available[label] ?: return Outcome.Invalid("그런 풀이가 없다: $label")
            }
        }

        val id = UUID.randomUUID()
        val run = LabRun(
            id = id, userId = userId, problemId = problemId, args = args, labels = labels,
            status = LabStatus.PENDING, results = emptyList(), createdAt = Instant.now(),
        )
        repository.insertWithOutbox(
            run,
            OutboxEvent(
                id = UUID.randomUUID(),
                aggregate = "lab",
                aggregateId = id.toString(),
                type = LAB_EVENT,
                payload = json.writeValueAsString(
                    LabRequest(
                        labId = id.toString(),
                        correlationId = id.toString(),
                        problemVersionId = pkg.problemVersionId,
                        packageDigest = pkg.packageDigest,
                        signature = pkg.manifest.signature,
                        limits = pkg.manifest.limits,
                        args = args,
                        approaches = chosen,
                    ),
                ),
                occurredAt = Instant.now(),
            ),
        )
        return Outcome.Started(run)
    }

    @Transactional
    fun completed(report: LabReport) {
        val id = runCatching { UUID.fromString(report.labId) }.getOrNull() ?: return
        repository.complete(id, report.results)
    }

    fun find(userId: String, id: UUID): LabRun? = repository.find(id)?.takeIf { it.userId == userId }

    /**
     * 나란히 놓을 수 있는 풀이들.
     *
     * 참조 풀이, `solutions/` 의 다른 풀이, 그리고 **성능 오답**. 성능 오답은 답은 맞고
     * 느릴 뿐이라 완전탐색 그 자체이고 — 기획서 §6.4 의 "완전탐색 → 개선 → 최적 풀이의
     * 발전 과정"의 첫 칸이 저작자 손으로 이미 써져 있는 셈이다.
     */
    private fun approaches(problemId: String): List<Approach> = buildList {
        packages.referenceSolution(problemId)?.let { add(Approach(REFERENCE, Language.KOTLIN, it)) }
        packages.alternativeSolutions(problemId).forEach { (name, source) ->
            add(Approach(name, Language.KOTLIN, source))
        }
        packages.mutants(problemId).filter { it.kind == DefectKind.PERFORMANCE }.forEach { mutant ->
            add(Approach("완전탐색 (${mutant.name})", Language.KOTLIN, mutant.source))
        }
    }

    private fun isOpen(userId: String, problemId: String): Boolean =
        submissions.latestAccepted(userId, problemId) != null || repository.unlocked(userId, problemId)

    sealed interface Outcome {
        data class Started(val run: LabRun) : Outcome
        data object Locked : Outcome
        data class Invalid(val reason: String) : Outcome
        data class Throttled(val used: Int, val allowed: Int) : Outcome
    }

    companion object {
        const val LAB_EVENT = "LabRequested"
        const val REFERENCE = "참조 풀이"
        const val MINE = "내 풀이"

        /** 한 시간에 몇 번까지. 한 건이 풀이 수만큼 계측 실행이다 (§15 실행 횟수를 곱한다). */
        private const val PER_HOUR = 20
        private val WINDOW: Duration = Duration.ofHours(1)
    }
}

/** 해설 응답. 잠겨 있으면 본문이 없다. */
data class EditorialView(
    val problemId: String,
    /** 해설이 저작돼 있나. */
    val available: Boolean,
    val locked: Boolean,
    val solved: Boolean,
    val body: String?,
    /** 실험실에 세울 수 있는 풀이 이름들. "내 풀이"는 맞힌 제출이 있을 때 화면이 더한다. */
    val approaches: List<String>,
)
