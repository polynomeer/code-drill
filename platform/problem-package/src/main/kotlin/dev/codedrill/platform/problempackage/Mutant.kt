package dev.codedrill.platform.problempackage

/**
 * 저작자가 함께 넣어 둔 **대표 오답** (기술 설계서 §6.1 `mutants/`).
 *
 * 원래는 콘텐츠 검증만 이것을 썼다 — 문제의 테스트가 이 오답들을 전부 잡는지 보는
 * 용도였다 (§6.3 3단계). 같은 오답 묶음을 **사용자의 테스트에 겨누면** 그 테스트가
 * 무엇을 잡고 무엇을 놓치는지가 나온다 (PRD FR-804). 잣대는 하나면 된다.
 *
 * [source] 는 사용자에게 절대 나가지 않는다. 오답 코드를 보여 주면 정답의 골격이 그대로
 * 보이기 때문이다. 밖으로 나가는 것은 [kind] 뿐이다.
 */
data class MutantSource(
    val name: String,
    val kind: DefectKind,
    val source: String,
    /**
     * 이 오답이 무엇을 잘못하는지 한 줄로 (`// kind:` 다음 주석 줄).
     *
     * 저작자가 자기가 보려고 적어 둔 것이었는데, 코칭이 이것을 쓴다 (FR-802) — 문제마다
     * "여기서 흔히 무너진다"를 사람이 직접 쓴 문장이 이미 있고, 그것이 곧 힌트다.
     * 새로 쓰면 같은 사실이 두 곳에 살고, 오답을 고쳤을 때 힌트만 옛말이 된다.
     */
    val note: String = "",
)

/**
 * 결함의 종류 (§6.1).
 *
 * 사용자에게 돌려주는 유일한 값이라 **개별 오답이 아니라 무리를 가리켜야 한다.**
 * "off-by-one--skips-last 를 놓쳤다"는 그 오답의 내용을 알려 주는 것이지만, "경계에서
 * 하나 어긋나는 결함을 놓쳤다"는 다음에 무엇을 시험해야 하는지를 알려 준다.
 */
enum class DefectKind(val label: String) {
    /** 경계에서 하나 어긋난다. */
    OFF_BY_ONE("경계 어긋남"),

    /** 특정 모양의 입력을 아예 다루지 않는다. */
    MISSING_EDGE_CASE("빠진 경계 입력"),

    /** 조건을 잘못 갈라 일부 입력에서만 틀린다. */
    WRONG_BRANCH("잘못된 분기"),

    /** 접근 자체가 틀렸다. */
    WRONG_ALGORITHM("잘못된 접근"),

    /**
     * 답은 맞지만 한도 안에 끝내지 못한다.
     *
     * **손으로 적는 케이스로는 잡히지 않는다.** 잡으려면 한도를 넘길 만큼 큰 입력이
     * 필요한데 그런 입력은 테스트 패널에 손으로 적을 수 있는 것이 아니다. 그래서 이
     * 종류는 사용자 테스트 채점에서 빠진다 — 빼지 않으면 아무도 100% 를 받을 수 없고,
     * 그 감점은 사용자가 고칠 수 없는 것에 대한 감점이다.
     */
    PERFORMANCE("성능"),

    /** `// kind:` 줄이 없거나 아는 이름이 아니다. */
    UNSPECIFIED("분류되지 않음");

    /** 사용자 테스트로 잡을 수 있는 종류인가. */
    val reachableByHandWrittenCase: Boolean get() = this != PERFORMANCE && this != UNSPECIFIED

    companion object {
        /**
         * `// kind: <NAME>` 첫 줄에서 읽는다. 별도 색인을 두면 파일과 어긋난다.
         * 프로젝트형의 Python 오답은 `# kind:` 다 — 주석 기호만 다르고 약속은 같다.
         *
         * 모르는 이름이면 [UNSPECIFIED] 로 떨어뜨린다. 여기서 예외를 올리면 오답 파일
         * 하나의 오타가 **문제 전체를 못 읽게** 만들고, 그러면 채점까지 선다.
         */
        fun readFrom(source: String): DefectKind =
            KIND_LINE.find(source)?.groupValues?.get(1)
                ?.let { name -> entries.firstOrNull { it.name == name } }
                ?: UNSPECIFIED

        private val KIND_LINE = Regex("""(?://|#)\s*kind:\s*(\w+)""")
    }
}
