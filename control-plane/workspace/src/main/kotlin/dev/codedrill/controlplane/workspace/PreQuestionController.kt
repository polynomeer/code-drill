package dev.codedrill.controlplane.workspace

import dev.codedrill.platform.common.Principal
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

/**
 * 풀이 전 질문 API (PRD FR-803).
 *
 * 질문은 카탈로그에서 만들고 (PreQuestions), 정답은 **내려보내지 않는다.** 화면이 정답을
 * 알면 사용자도 알 수 있고, 그러면 예측이 아니라 받아쓰기가 된다.
 */
// 공개 문제 트리 아래에 두지 않는다. 그쪽은 로그인 없이 열려 있어 인증이 선택이고,
// 그러면 주체가 없는 요청이 여기까지 들어온다. 질문 응답은 처음부터 끝까지 사용자의
// 것이라 경로도 사용자 쪽에 둔다.
@RestController
@RequestMapping("/api/v1/prequestions/{problemId}")
class PreQuestionController(private val service: PreQuestionService) {

    /** 질문과, 이 사용자가 이미 답한 것. */
    @GetMapping
    fun get(
        @PathVariable problemId: String,
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
    ): ResponseEntity<PreQuestionSet> =
        service.of(principal.id, problemId)?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @PostMapping
    fun answer(
        @PathVariable problemId: String,
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @Valid @RequestBody request: AnswerRequest,
    ): ResponseEntity<AnsweredView> {
        val kind = runCatching { QuestionKind.valueOf(request.kind) }.getOrNull()
            ?: return ResponseEntity.badRequest().build()
        val answered = service.answer(principal.id, problemId, kind, request.answer, request.rationale)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(answered)
    }
}

data class AnswerRequest(
    @field:NotBlank val kind: String,
    @field:NotBlank val answer: String,
    /** 왜 그렇게 골랐는가. 없어도 된다 — 필수로 하면 질문 자체를 건너뛴다. */
    val rationale: String? = null,
)

/** 질문지. **정답은 들어 있지 않다.** */
data class PreQuestionSet(
    val questions: List<PreQuestion>,
    val answered: List<AnsweredView>,
)

/**
 * 답한 것 하나.
 *
 * 채점 결과는 답한 **뒤에** 알려 준다. 그래야 예측이 기록으로 남고, 사용자는 자기가 무엇을
 * 잘못 봤는지 바로 안다.
 */
data class AnsweredView(
    val kind: QuestionKind,
    val answer: String,
    val correct: Boolean,
    val misconception: Misconception?,
    /** 틀렸을 때만 정답을 보여준다. 맞았으면 이미 알고 있다. */
    val expected: String?,
)

@Service
class PreQuestionService(
    private val repository: PreQuestionRepository,
    private val packages: ProblemPackageLoader,
    private val questions: PreQuestions,
    private val learning: LearningSignals = LearningSignals.NONE,
) {

    fun of(userId: String, problemId: String): PreQuestionSet? {
        val pkg = runCatching { packages.load(problemId) }.getOrNull() ?: return null
        val answered = repository.latest(userId, problemId).map { view(pkg, it) }
        return PreQuestionSet(questions.of(pkg), answered)
    }

    @Transactional
    fun answer(
        userId: String,
        problemId: String,
        kind: QuestionKind,
        answer: String,
        rationale: String?,
    ): AnsweredView? {
        val pkg = runCatching { packages.load(problemId) }.getOrNull() ?: return null
        // 묻지 않은 질문의 답은 받지 않는다. 받으면 "무엇을 골라도 틀림"이 증거로 쌓이고,
        // 3단계는 그것을 그 사람의 오개념으로 읽는다.
        if (!questions.asks(pkg, kind)) return null
        val (correct, misconception) = questions.grade(pkg, kind, answer)

        val response = PreQuestionResponse(
            id = UUID.randomUUID(),
            userId = userId,
            problemId = problemId,
            kind = kind,
            answer = answer,
            correct = correct,
            rationale = rationale?.takeIf { it.isNotBlank() },
            misconception = misconception,
            answeredAt = Instant.now(),
        )
        repository.insert(response)

        // 예측이 맞았는지가 곧 증거다 (§8.2 — 알고리즘 선택·복잡도 예측). 실패해도 삼킨다:
        // 학습 기록이 질문에 답하는 일을 막아서는 안 된다.
        runCatching {
            learning.answered(
                userId = userId,
                problemId = problemId,
                competency = kind.competency(),
                correct = correct,
                reference = response.id.toString(),
                detail = buildString {
                    append(answer)
                    if (!correct) append(" — ").append(misconception?.name ?: "틀림")
                },
            )
        }
        return view(pkg, response)
    }

    private fun view(
        pkg: dev.codedrill.platform.problempackage.ProblemPackage,
        response: PreQuestionResponse,
    ) = AnsweredView(
        kind = response.kind,
        answer = response.answer,
        correct = response.correct,
        misconception = response.misconception,
        expected = if (response.correct) null else expectedOf(pkg, response.kind),
    )

    private fun expectedOf(
        pkg: dev.codedrill.platform.problempackage.ProblemPackage,
        kind: QuestionKind,
    ): String = when (kind) {
        QuestionKind.ALGORITHM_CHOICE -> questions.techniquesOf(pkg).sorted().joinToString(", ")
        // 실제 식이 있으면 그것을 보여준다. 등급만 보여주면 O(n·m) 문제에 O(n²) 라고
        // 적히고, 그건 맞지만 사용자가 배울 것은 아니다.
        QuestionKind.TIME_COMPLEXITY ->
            pkg.catalog.complexity.note.ifBlank { pkg.catalog.complexity.time.label }
        QuestionKind.SPACE_COMPLEXITY ->
            pkg.catalog.complexity.note.ifBlank { pkg.catalog.complexity.space.label }
    }
}
