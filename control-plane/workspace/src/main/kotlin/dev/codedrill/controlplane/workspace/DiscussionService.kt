package dev.codedrill.controlplane.workspace

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 문제별 질문 게시판 (기획서 §8.5).
 *
 * 사용자 쪽(묻기·답하기·신고)과 검수자 쪽(내리기·기각)이 한 서비스에 있다 — 아레나의
 * 기부와 같은 이유다. 같은 표의 같은 상태 기계를 두 곳에서 움직이지 않는다.
 *
 * **무엇이 잠기나.** 글 자체는 맞히기 전에도 보인다 — 질문은 막힌 사람이 하는 것이다.
 * 잠기는 것은 글쓴이가 **풀이 노출**이라고 표시한 글의 본문과 코드 구간이고, 그것은 이
 * 문제를 맞힌 사람에게만 열린다. 아레나와 같은 잠금([ArenaGate])이다.
 */
@Service
class DiscussionService(
    private val repository: DiscussionRepository,
    private val submissions: AnchorableSubmissions = AnchorableSubmissions.NONE,
    private val gate: ArenaGate = ArenaGate.CLOSED,
) {

    // --- 읽기 -------------------------------------------------------------------

    fun questions(readerId: String, problemId: String): List<PostView> {
        val questions = repository.questions(problemId, LIST_LIMIT)
        val counts = repository.answerCounts(questions.map { it.id })
        val solved = lazy { gate.solved(readerId, problemId) }
        return questions.map { it.view(readerId, solved, counts[it.id] ?: 0) }
    }

    fun thread(readerId: String, questionId: UUID): Thread? {
        val question = repository.find(questionId)?.takeIf { it.parentId == null && it.status == PostStatus.VISIBLE }
            ?: return null
        val answers = repository.answers(questionId)
        val solved = lazy { gate.solved(readerId, question.problemId) }
        return Thread(
            question = question.view(readerId, solved, answers.size),
            answers = answers.map { it.view(readerId, solved, 0) },
        )
    }

    /**
     * 이 제출을 이 사람이 읽어도 되나 — 제출 도메인의 물음 (§3.1 조립 지점).
     *
     * 글에 붙은 리플레이 시점은 그 글을 볼 수 있는 사람이 열 수 있어야 한다. 풀이 노출인
     * 글에 붙은 것이면 맞힌 사람만이다.
     */
    fun sharedWith(readerId: String, submissionId: UUID): Boolean =
        repository.anchoring(submissionId).any { (problemId, spoiler) -> !spoiler || gate.solved(readerId, problemId) }

    // --- 쓰기 -------------------------------------------------------------------

    @Transactional
    fun ask(userId: String, problemId: String, title: String, body: String, anchor: AnchorRequest?, spoiler: Boolean): PostOutcome {
        val cleanTitle = title.trim()
        if (cleanTitle.length !in MIN_TITLE..MAX_TITLE) return PostOutcome.Invalid("제목은 ${MIN_TITLE}~${MAX_TITLE}자")
        return write(userId, problemId, parent = null, title = cleanTitle, body = body, anchor = anchor, spoiler = spoiler)
    }

    @Transactional
    fun answer(userId: String, questionId: UUID, body: String, anchor: AnchorRequest?, spoiler: Boolean): PostOutcome {
        val question = repository.find(questionId)?.takeIf { it.parentId == null && it.status == PostStatus.VISIBLE }
            ?: return PostOutcome.Invalid("그런 질문이 없다")
        return write(userId, question.problemId, parent = question, title = null, body = body, anchor = anchor, spoiler = spoiler)
    }

    private fun write(
        userId: String, problemId: String, parent: DiscussionPost?, title: String?, body: String,
        anchor: AnchorRequest?, spoiler: Boolean,
    ): PostOutcome {
        val cleanBody = body.trim()
        if (cleanBody.length < MIN_BODY) return PostOutcome.Invalid("본문은 ${MIN_BODY}자 이상")
        if (cleanBody.length > MAX_BODY) return PostOutcome.Invalid("본문은 ${MAX_BODY}자 이하")
        val resolved = anchor?.let { resolve(userId, problemId, it) }
        if (resolved is Resolved.Invalid) return PostOutcome.Invalid(resolved.reason)
        val post = DiscussionPost(
            id = UUID.randomUUID(), problemId = problemId, parentId = parent?.id, authorId = userId,
            title = title, body = cleanBody, anchor = (resolved as? Resolved.Ok)?.anchor, spoiler = spoiler,
            status = PostStatus.VISIBLE, hiddenBy = null, hiddenAt = null, hiddenReason = null, createdAt = Instant.now(),
        )
        repository.insert(post)
        return PostOutcome.Posted(post.view(userId, lazyOf(true), 0))
    }

    /**
     * 붙일 자리를 확인한다. **글쓴이 자신의, 이 문제의 제출**이어야 하고, 코드 구간은 실제
     * 줄 범위 안이어야 한다. 구간은 [MAX_EXCERPT_LINES] 줄까지 — 그보다 길면 구간이 아니라
     * 풀이 전체다.
     */
    private fun resolve(userId: String, problemId: String, request: AnchorRequest): Resolved {
        val lines = submissions.lines(userId, problemId, request.submissionId)
            ?: return Resolved.Invalid("내 것이고 이 문제의 제출만 붙일 수 있다")
        val from = request.lineFrom
        val to = request.lineTo
        var excerpt: String? = null
        if (from != null || to != null) {
            if (from == null || to == null) return Resolved.Invalid("코드 구간은 시작 줄과 끝 줄이 함께 있어야 한다")
            if (from < 1 || to < from || to > lines.size) return Resolved.Invalid("코드 구간이 제출의 줄 범위(1~${lines.size}) 밖이다")
            if (to - from + 1 > MAX_EXCERPT_LINES) return Resolved.Invalid("코드 구간은 ${MAX_EXCERPT_LINES}줄까지")
            excerpt = lines.subList(from - 1, to).joinToString("\n")
        }
        if (request.step != null && request.step < 0) return Resolved.Invalid("리플레이 시점은 0 이상")
        if (excerpt == null && request.step == null) return Resolved.Invalid("코드 구간이나 리플레이 시점 중 하나는 있어야 한다")
        return Resolved.Ok(Anchor(request.submissionId, from, to, request.step, excerpt))
    }

    /** 글을 신고한다. 보는 사람이 곧 신고할 수 있는 사람이다. 두 번째 신고는 조용히 무시된다. */
    @Transactional
    fun report(userId: String, postId: UUID, reason: String): ReportOutcome {
        val trimmed = reason.trim()
        if (trimmed.length < MIN_BODY) return ReportOutcome.Invalid("사유를 ${MIN_BODY}자 이상 적는다")
        val post = repository.find(postId)?.takeIf { it.status == PostStatus.VISIBLE }
            ?: return ReportOutcome.Invalid("그런 글이 없다")
        if (post.authorId == userId) return ReportOutcome.Invalid("자기 글은 신고할 것이 아니다")
        val report = DiscussionReport(
            id = UUID.randomUUID(), postId = postId, reporterId = userId, reason = trimmed,
            status = ReportStatus.OPEN, resolvedBy = null, resolvedAt = null, resolution = null, createdAt = Instant.now(),
        )
        return if (repository.insertReport(report)) ReportOutcome.Filed(report) else ReportOutcome.AlreadyFiled
    }

    // --- 검수자 -----------------------------------------------------------------

    /** 열린 신고와 그 글. 검수자는 글 전체를 본다 — 잠금은 검수자에게는 없다. */
    fun queue(): List<ReportedPost> = repository.openReports().map { ReportedPost(it, repository.find(it.postId)) }

    /**
     * 신고를 처리한다. 내리면 그 글의 열린 신고 전부가 함께 닫힌다. 질문을 내리면 답은
     * 남지만 목록에서 사라진다 — 질문이 없는 답은 열 길이 없다.
     */
    @Transactional
    fun resolve(reportId: UUID, reviewer: String, hide: Boolean, resolution: String): ReviewOutcome {
        val report = repository.findReport(reportId) ?: return ReviewOutcome.Rejected("그런 신고가 없다")
        if (report.status != ReportStatus.OPEN) return ReviewOutcome.Rejected("이미 처리된 신고다: ${report.status}")
        val post = repository.find(report.postId) ?: return ReviewOutcome.Rejected("신고된 글이 없다")
        val trimmed = resolution.trim()
        if (hide) {
            repository.hide(post.id, reviewer, trimmed)
            repository.resolveReports(post.id, ReportStatus.RETIRED, reviewer, trimmed)
        } else {
            repository.resolveReports(post.id, ReportStatus.DISMISSED, reviewer, trimmed)
        }
        return ReviewOutcome.Decided(repository.find(post.id)!!)
    }

    /**
     * 밖으로 나가는 모양. 글쓴이의 id 는 나가지 않고 "내 것인가"만 나간다. 풀이 노출인
     * 글은 맞힌 사람이 아니면 본문과 코드 구간이 비고 [PostView.locked] 가 선다.
     */
    private fun DiscussionPost.view(readerId: String, solved: Lazy<Boolean>, answerCount: Int): PostView {
        val mine = authorId == readerId
        val locked = spoiler && !mine && !solved.value
        return PostView(
            id = id, problemId = problemId, parentId = parentId, title = title,
            body = if (locked) "" else body,
            anchor = anchor?.let { if (locked) null else AnchorView(it.submissionId, it.lineFrom, it.lineTo, it.step, it.excerpt) },
            spoiler = spoiler, locked = locked, mine = mine, erased = authorId == null,
            answerCount = answerCount, createdAt = createdAt,
        )
    }

    data class PostView(
        val id: UUID,
        val problemId: String,
        val parentId: UUID?,
        val title: String?,
        val body: String,
        val anchor: AnchorView?,
        val spoiler: Boolean,
        val locked: Boolean,
        val mine: Boolean,
        val erased: Boolean,
        val answerCount: Int,
        val createdAt: Instant,
    )

    data class AnchorView(val submissionId: UUID, val lineFrom: Int?, val lineTo: Int?, val step: Int?, val excerpt: String?)
    data class AnchorRequest(val submissionId: UUID, val lineFrom: Int?, val lineTo: Int?, val step: Int?)
    data class Thread(val question: PostView, val answers: List<PostView>)
    data class ReportedPost(val report: DiscussionReport, val post: DiscussionPost?)

    private sealed interface Resolved {
        data class Ok(val anchor: Anchor) : Resolved
        data class Invalid(val reason: String) : Resolved
    }

    sealed interface PostOutcome {
        data class Posted(val post: PostView) : PostOutcome
        data class Invalid(val reason: String) : PostOutcome
    }

    sealed interface ReportOutcome {
        data class Filed(val report: DiscussionReport) : ReportOutcome
        data object AlreadyFiled : ReportOutcome
        data class Invalid(val reason: String) : ReportOutcome
    }

    sealed interface ReviewOutcome {
        data class Decided(val post: DiscussionPost) : ReviewOutcome
        data class Rejected(val reason: String) : ReviewOutcome
    }

    companion object {
        const val MIN_TITLE = 3
        const val MAX_TITLE = 120
        const val MIN_BODY = 10
        const val MAX_BODY = 5000
        const val MAX_EXCERPT_LINES = 40
        const val LIST_LIMIT = 50
    }
}
