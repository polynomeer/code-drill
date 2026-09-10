package dev.codedrill.controlplane.workspace

import dev.codedrill.platform.problempackage.Complexity
import dev.codedrill.platform.problempackage.ProblemPackage
import java.time.Instant
import java.util.UUID

/**
 * 풀이 전 질문 (PRD FR-803, 기획서 §4.3).
 *
 * 코드를 쓰기 **전에** 접근과 비용을 말해 보게 한다. 다 풀고 나서 "이게 O(n log n)이었나"를
 * 묻는 것과는 다른 질문이다 — 앞에서 물으면 예측이고, 뒤에서 물으면 회상이다. 역량으로
 * 재려는 것은 예측 쪽이다.
 *
 * **틀려도 풀이를 막지 않는다.** 관문으로 쓰면 사용자는 답을 맞히는 데 집중하고, 그러면
 * 예측이 아니라 시험이 된다.
 */
data class PreQuestion(
    val kind: QuestionKind,
    val prompt: String,
    /** 고를 수 있는 값. 정답이 어느 것인지는 내려보내지 않는다. */
    val choices: List<String>,
)

enum class QuestionKind {
    /** 어떤 기법으로 풀 것인가 (역량: ALGORITHM_CHOICE). */
    ALGORITHM_CHOICE,

    /** 시간 비용은 얼마인가 (역량: COMPLEXITY). */
    TIME_COMPLEXITY,

    /** 공간 비용은 얼마인가 (역량: COMPLEXITY). */
    SPACE_COMPLEXITY,
}

/**
 * 응답 하나.
 *
 * 정답 여부만이 아니라 [rationale] 과 [misconception] 을 함께 남긴다. 맞았다/틀렸다만
 * 남기면 3단계가 읽을 수 있는 것도 그것뿐이고, 코칭은 겨눌 지점을 못 찾는다.
 */
data class PreQuestionResponse(
    val id: UUID,
    val userId: String,
    val problemId: String,
    val kind: QuestionKind,
    val answer: String,
    val correct: Boolean,
    val rationale: String?,
    val misconception: Misconception?,
    val answeredAt: Instant,
)

/**
 * 오개념 분류.
 *
 * 문제마다 손으로 적지 않는다. **고른 답과 정답의 관계**에서 나오는 것만 분류하며, 그
 * 관계는 문제와 무관하게 같은 뜻을 갖는다 — 실제보다 싸게 본 것과 비싸게 본 것은 고쳐야
 * 할 곳이 다르다.
 *
 * 문제별 오개념(예: "이분탐색의 경계를 닫힌 구간으로 본다")은 여기 없다. 그것은 문제마다
 * 사람이 적어야 하는 것이고, 적지 않은 채로 분류하는 척하면 3단계가 없는 신호를 읽는다.
 */
enum class Misconception {
    /** 정답보다 싼 등급을 골랐다 — 어딘가의 비용을 세지 않았다. */
    UNDER_ESTIMATED,

    /** 정답보다 비싼 등급을 골랐다 — 필요 없는 비용을 가정했다. */
    OVER_ESTIMATED,

    /** 이 문제가 쓰지 않는 기법을 골랐다. */
    WRONG_TECHNIQUE,
}

/**
 * 문제에서 질문을 만든다.
 *
 * 정답은 카탈로그가 갖고 있다 — 태그가 기법을, `complexity` 가 비용을 말한다. 문제마다
 * 질문지를 손으로 쓰지 않는 이유는, 38문제 × 3질문을 손으로 쓰면 카탈로그와 어긋나는
 * 순간부터 **채점이 조용히 틀리기** 때문이다.
 */
class PreQuestions(
    /**
     * 고를 수 있는 기법 전부. `content/tags.yaml` 의 어휘에서 만든다.
     *
     * 목록을 코드에 또 적지 않는다. 두 곳에 적으면 태그를 하나 늘렸을 때 선택지에는
     * 없는 정답이 생기고, 그러면 사용자는 **무엇을 골라도 틀리는 질문**을 받는다.
     */
    private val techniques: List<String>,
) {

    fun of(pkg: ProblemPackage): List<PreQuestion> {
        val answers = techniquesOf(pkg)
        val complexities = Complexity.entries.map { it.label }

        return buildList {
            // 태그에 기법이 하나도 없는 문제가 있다 (자료구조만 붙은 경우). 정답이 없는
            // 질문을 내지 않는다 — 무엇을 골라도 틀리는 질문은 예측을 재지 못한다.
            if (answers.isNotEmpty()) {
                add(
                    PreQuestion(
                        QuestionKind.ALGORITHM_CHOICE,
                        "이 문제를 어떤 기법으로 풀 생각인가?",
                        techniques,
                    ),
                )
            }
            add(PreQuestion(QuestionKind.TIME_COMPLEXITY, "그 풀이의 시간 복잡도는?", complexities))
            add(PreQuestion(QuestionKind.SPACE_COMPLEXITY, "추가로 쓰는 공간은?", complexities))
        }
    }

    /** 이 문제가 실제로 쓰는 기법. */
    fun techniquesOf(pkg: ProblemPackage): Set<String> =
        pkg.catalog.tags.filter { it in techniques }.toSet()

    /** 이 문제에 실제로 내는 질문인가. 묻지 않은 질문의 답은 받지 않는다. */
    fun asks(pkg: ProblemPackage, kind: QuestionKind): Boolean =
        of(pkg).any { it.kind == kind }

    /**
     * 채점.
     *
     * 복잡도는 **등급의 순서**로 방향까지 본다. 기법은 여러 개가 정답일 수 있다 — 한
     * 문제를 BFS 로도 DFS 로도 풀 수 있으면 둘 다 맞다.
     */
    fun grade(pkg: ProblemPackage, kind: QuestionKind, answer: String): Pair<Boolean, Misconception?> =
        when (kind) {
            QuestionKind.ALGORITHM_CHOICE -> {
                val correct = answer in techniquesOf(pkg)
                correct to if (correct) null else Misconception.WRONG_TECHNIQUE
            }

            QuestionKind.TIME_COMPLEXITY -> compare(answer, pkg.catalog.complexity.time)
            QuestionKind.SPACE_COMPLEXITY -> compare(answer, pkg.catalog.complexity.space)
        }

    private fun compare(answer: String, correct: Complexity): Pair<Boolean, Misconception?> {
        val chosen = Complexity.entries.firstOrNull { it.label == answer }
            // 목록에 없는 값이 왔다. 틀렸다고만 하고 방향은 말하지 않는다 — 어디에
            // 있는지 모르는 값의 방향을 지어내면 3단계가 없는 신호를 읽는다.
            ?: return false to null

        return when {
            chosen == correct -> true to null
            chosen.ordinal < correct.ordinal -> false to Misconception.UNDER_ESTIMATED
            else -> false to Misconception.OVER_ESTIMATED
        }
    }
}
