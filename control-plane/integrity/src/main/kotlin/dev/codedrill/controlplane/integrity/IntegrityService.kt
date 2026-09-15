package dev.codedrill.controlplane.integrity

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 제출 사이의 유사도 신호 (§11.4, §10.4).
 *
 * 맞힌 제출이 확정될 때 지문을 뜨고, 같은 문제·같은 언어의 다른 사람 지문과 견준다. 문턱을
 * 넘는 쌍이 신호가 되어 검수 큐에 오른다. **신호는 판정을 바꾸지 않는다** — 검수자가 두
 * 소스를 나란히 보고 확인하거나 기각하고, 확인된 것도 지금은 기록이다.
 *
 * 짧은 소스는 뜨지 않는다. 한 줄짜리 풀이는 누가 써도 같다.
 */
@Service
class IntegrityService(
    private val repository: IntegrityRepository,
    private val sources: SubmissionSources = SubmissionSources.NONE,
) {

    /** 맞힌 제출이 확정됐다. 지문을 뜨고 겹치는 쌍을 신호로 남긴다. 실패해도 판정을 막지 않는다 — 부르는 쪽이 삼킨다. */
    @Transactional
    fun accepted(userId: String, problemId: String, submissionId: UUID, language: String, source: String): List<SimilarityFlag> {
        val print = Fingerprint.of(source)
        if (print.tokenCount < MIN_TOKENS) return emptyList()
        val flags = repository.others(problemId, language, userId, COMPARE_LIMIT)
            .map { it to Fingerprint.similarity(print.hashes, it.hashes) }
            .filter { (_, score) -> score >= THRESHOLD }
            .sortedByDescending { (_, score) -> score }
            .take(MAX_FLAGS_PER_SUBMISSION)
            .map { (other, score) ->
                val (first, second) = if (submissionId.toString() < other.submissionId.toString()) submissionId to other.submissionId else other.submissionId to submissionId
                val (firstUser, secondUser) = if (first == submissionId) userId to other.userId else other.userId to userId
                SimilarityFlag(
                    id = UUID.randomUUID(), problemId = problemId, language = language,
                    submissionId = first, otherSubmissionId = second, userId = firstUser, otherUserId = secondUser,
                    score = score, status = FlagStatus.OPEN, reviewedBy = null, reviewedAt = null, note = null, createdAt = Instant.now(),
                )
            }
            .filter { repository.insertFlag(it) }
        repository.savePrint(submissionId, userId, problemId, language, print)
        if (flags.isNotEmpty()) log.info("유사도 신호 {}건: {} ({})", flags.size, submissionId, problemId)
        return flags
    }

    // --- 검수자 -----------------------------------------------------------------

    /** 열린 신호와 두 소스, 새 것부터. 소스는 볼 때만 제출 도메인에 묻는다. */
    fun queue(): List<FlaggedPair> = repository.open(QUEUE_LIMIT).map {
        FlaggedPair(it, sources.source(it.submissionId), sources.source(it.otherSubmissionId))
    }

    @Transactional
    fun resolve(id: UUID, reviewer: String, confirmed: Boolean, note: String?): ReviewOutcome {
        val flag = repository.find(id) ?: return ReviewOutcome.Rejected("그런 신호가 없다")
        if (flag.status != FlagStatus.OPEN) return ReviewOutcome.Rejected("이미 결정된 신호다: ${flag.status}")
        repository.resolve(id, if (confirmed) FlagStatus.CONFIRMED else FlagStatus.DISMISSED, reviewer, note?.trim()?.ifBlank { null })
        return ReviewOutcome.Decided(repository.find(id)!!)
    }

    data class FlaggedPair(val flag: SimilarityFlag, val source: String?, val otherSource: String?)

    sealed interface ReviewOutcome {
        data class Decided(val flag: SimilarityFlag) : ReviewOutcome
        data class Rejected(val reason: String) : ReviewOutcome
    }

    companion object {
        /** 이보다 짧은 소스는 뜨지 않는다. 두 수의 합 정도가 60 토큰 안팎이다. */
        const val MIN_TOKENS = 40
        const val THRESHOLD = 0.8
        const val COMPARE_LIMIT = 500
        const val MAX_FLAGS_PER_SUBMISSION = 3
        const val QUEUE_LIMIT = 100
        private val log = LoggerFactory.getLogger(IntegrityService::class.java)
    }
}
