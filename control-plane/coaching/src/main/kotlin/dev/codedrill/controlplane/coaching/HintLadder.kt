package dev.codedrill.controlplane.coaching

import dev.codedrill.platform.problempackage.Competency
import dev.codedrill.platform.problempackage.DefectKind
import dev.codedrill.platform.problempackage.MutantSource
import dev.codedrill.platform.problempackage.ProblemPackage
import dev.codedrill.platform.problempackage.ProblemPackageLoader

/**
 * 역량 하나에 대한 도움 사다리 (FR-802).
 *
 * 단계는 **방향 → 단서 → 방법**이다. 어느 단계에서도 코드를 주지 않는다 — 코드를 주는
 * 순간 그것은 도움이 아니라 정답이고, 그 뒤에 남는 증거는 아무것도 재지 못한다.
 *
 * **문제가 이미 갖고 있는 것에서 만든다.** 목표 복잡도는 카탈로그에 있고, "여기서 흔히
 * 무너진다"는 대표 오답마다 저작자가 한 줄로 적어 두었다 (§6.1). 그것을 힌트로 다시 쓰면
 * 같은 사실이 두 곳에 살고, 오답을 고쳤을 때 힌트만 옛말이 된다.
 *
 * 산문이 필요한 자리에는 `hints.yaml` 이 들어온다. 아직 없는 문제에서는 사다리가 짧아질
 * 뿐 비지 않는다 — 도움이 없다고 코칭을 못 여는 것보다, 얕은 도움이라도 있는 편이 낫다.
 */
class HintLadder(private val packages: ProblemPackageLoader) {

    fun of(problemId: String, competency: Competency): List<Hint> {
        val pkg = runCatching { packages.load(problemId) }.getOrNull() ?: return emptyList()
        val mutants = runCatching { packages.mutants(problemId) }.getOrDefault(emptyList())

        val derived = when (competency) {
            Competency.COMPLEXITY -> complexity(pkg)
            Competency.ALGORITHM_CHOICE, Competency.OPTIMIZATION -> approach(pkg)
            Competency.EDGE_CASES, Competency.CORRECTNESS, Competency.COUNTEREXAMPLE,
            Competency.TEST_DESIGN, Competency.DEBUGGING,
            -> defects(mutants)
            else -> emptyList()
        }

        // 만들 수 있는 것이 먼저, 쓴 것이 나중이다. 만든 것은 목표와 함정을 가리키는
        // **방향**이고 쓴 것은 그 방향에서 무엇을 해야 하는지인 **방법**이라, 순서가
        // 뒤집히면 사다리가 아래로 내려간다.
        val authored = runCatching { packages.hints(problemId) }.getOrDefault(emptyMap())
        val steps = derived + authored[competency].orEmpty()

        return steps.mapIndexed { index, text -> Hint(level = index + 1, text = text) }
    }

    /** 이 문제에서 도울 수 있는 역량. 사다리가 비면 초점에 넣지 않는다 (FR-802). */
    fun coachable(problemId: String, among: List<Competency>): List<Competency> =
        among.filter { of(problemId, it).isNotEmpty() }

    /**
     * 목표 비용을 말해 준다.
     *
     * 답을 주지 않는다 — "O(n) 이 목표다"는 **무엇을 만들지**가 아니라 **무엇을 버려야
     * 하는지**를 말한다. 지금 생각이 O(n²) 라면 안쪽 반복을 없앨 방법을 찾아야 한다는 뜻이고,
     * 그 방법은 여전히 사용자가 찾는다.
     */
    private fun complexity(pkg: ProblemPackage): List<String> = buildList {
        add("목표 시간 복잡도는 ${pkg.catalog.complexity.time.label} 입니다. 지금 떠올린 방법이 그보다 비싸다면, 반복 안에서 무엇을 다시 찾고 있는지 보세요.")
        val note = pkg.catalog.complexity.note
        if (note.isNotBlank() && note != pkg.catalog.complexity.time.label) {
            add("정확히는 $note 입니다.")
        }
        add("공간은 ${pkg.catalog.complexity.space.label} 까지 써도 됩니다. 시간을 줄이려고 공간을 쓰는 것이 이 문제에서 허용됩니다.")
    }

    /**
     * 어떤 계열의 접근인지.
     *
     * **기법 태그를 1단계에 두지 않는다.** 그것을 먼저 주면 사용자가 고를 것이 남지 않고,
     * 재려던 역량이 바로 그 "고르기"다.
     */
    private fun approach(pkg: ProblemPackage): List<String> = buildList {
        add("지금 생각한 방법의 비용을 목표 ${pkg.catalog.complexity.time.label} 와 비교해 보세요. 어긋난다면 접근을 바꿔야 한다는 신호입니다.")
        pkg.catalog.tags.takeIf { it.isNotEmpty() }?.let {
            add("이 문제가 속한 계열: ${it.joinToString(", ")}.")
        }
    }

    /**
     * 이 문제에서 실제로 무너지는 자리들 (§6.1 `mutants/`).
     *
     * 성능 오답은 뺀다. 이 역량들이 묻는 것은 답이 맞는가이고, "큰 입력에서 느리다"는
     * 거기에 아무 단서도 주지 않는다.
     */
    private fun defects(mutants: List<MutantSource>): List<String> =
        mutants.filter { it.kind != DefectKind.PERFORMANCE }
            .mapNotNull { mutant ->
                firstSentence(mutant.note)?.let { note -> "$note (${mutant.kind.label})" }
            }

    /**
     * 오답 설명의 첫 문장만 쓴다.
     *
     * 뒷문장은 **그 오답이 어떻게 구현돼 있는지**를 적은 것이라 저작자에게는 쓸모가 있지만
     * 힌트로는 혼란스럽다. 실제로 `row-wrap--connects-across-edge` 의 설명은 뒤 두 문장이
     * 오답 코드의 내부를 설명한다.
     */
    private fun firstSentence(note: String): String? =
        note.trim().takeIf { it.isNotEmpty() }
            ?.substringBefore(". ")
            ?.trimEnd('.')
            ?.plus(".")
}

/**
 * 도움 한 단계.
 *
 * [text] 는 펼치기 전에는 내려보내지 않는다. 목록만 받아 놓고 화면에서 가려 두면
 * 개발자 도구를 열 줄 아는 사람에게는 도움이 아니라 그냥 정답이고, 그 사람의 증거만
 * 조용히 부풀려진다.
 */
data class Hint(val level: Int, val text: String)
