package dev.codedrill.platform.problempackage

/**
 * 문제를 **찾고 고르는 데** 쓰는 메타데이터 (기획서 부록 A 문제 도메인, PRD FR-201~203).
 *
 * ## 왜 manifest 가 아니라 따로 있나
 *
 * [ProblemPackage.packageDigest] 는 manifest 와 테스트에서 계산하며, **판정을 재현하는
 * 근거**다. 이미 등록된 버전과 digest 가 다르면 등록이 거절된다 — 고친 패키지는 새
 * 버전이어야 한다 (§6.1).
 *
 * 난이도와 태그는 판정을 바꾸지 않는다. 그것을 manifest 에 넣으면 **오타 하나를 고치는
 * 데도 문제 버전이 올라가고**, 버전 번호는 "무엇으로 채점됐는가"를 뜻하지 않게 된다.
 * 그래서 카탈로그는 digest 밖에 둔다.
 *
 * 경계는 한 문장으로 선다: **digest 에 드는 것은 판정을 바꾸는 것뿐이다.**
 */
data class ProblemCatalog(
    val difficulty: Difficulty,

    /**
     * 알고리즘·자료구조 태그. 사용자가 목록에서 거르는 값이다.
     *
     * 자유 문자열이지만 아무 값이나 되지는 않는다 — `content/tags.yaml` 의 어휘에
     * 있어야 하고, 검증 파이프라인이 그것을 확인한다. 어휘를 파일로 둔 이유는 태그가
     * 계속 늘기 때문이고, 그럼에도 검사하는 이유는 `two-pointer` 와 `two-pointers` 가
     * 나뉘면 필터가 조용히 반만 찾기 때문이다.
     */
    val tags: List<String>,

    /**
     * 이 문제가 **증거를 만들어 줄 수 있는** 역량 (기획서 §4.2).
     *
     * "이 문제가 무엇을 가르치나"가 아니라 "이 문제를 푼 기록에서 무엇을 읽어낼 수
     * 있나"다. 둘은 다르다 — 모든 문제가 구현력의 증거를 만들지만, 복잡도 예측의
     * 증거는 제한이 그것을 강제하는 문제에서만 나온다.
     *
     * 3단계 Competency 모듈이 이 값으로 증거를 역량에 붙인다. 비어 있으면 그 문제를
     * 아무리 풀어도 숙련도가 움직이지 않는다.
     */
    val competencies: List<Competency>,

    /**
     * 먼저 풀어 두면 좋은 문제 (기획서 §8.1 선수 관계).
     *
     * 학습 경로 추천이 이 관계를 탄다. 순환이면 경로가 만들어지지 않으므로 검증에서
     * 막는다.
     */
    val prerequisites: List<String> = emptyList(),

    /**
     * 의도한 풀이의 시간·공간 복잡도 (PRD FR-803 복잡도 예측).
     *
     * 사전 질문이 이것을 정답으로 쓴다. 등급으로 두는 이유는 **틀린 방향을 알기 위해서**다 —
     * 사용자가 실제보다 싸게 봤는지 비싸게 봤는지가 다른 오개념이고, 문자열로 두면 그 둘을
     * 가릴 수 없다.
     */
    val complexity: ComplexityNote,
) {
    init {
        require(tags.isNotEmpty()) { "태그가 최소 하나는 있어야 한다 — 없으면 목록에서 찾을 수 없다" }
        require(tags.distinct().size == tags.size) { "태그가 중복된다: $tags" }
        require(competencies.isNotEmpty()) {
            "역량이 최소 하나는 있어야 한다 — 없으면 풀어도 숙련도가 움직이지 않는다"
        }
        require(competencies.distinct().size == competencies.size) { "역량이 중복된다" }
        require(prerequisites.distinct().size == prerequisites.size) { "선수 문제가 중복된다" }
    }
}

/**
 * 복잡도 (PRD FR-803).
 *
 * [time] 과 [space] 는 채점에 쓰는 **등급**이고, [note] 는 사람에게 보여줄 실제 식이다.
 * 둘을 나눈 이유는 두 변수를 쓰는 문제 때문이다 — 편집 거리는 `O(n·m)`, 배낭은 `O(n·W)`
 * 인데 등급으로는 둘 다 제곱이다. 등급만 두면 화면이 거짓을 말하고, 식만 두면 "더 싸게
 * 봤나 더 비싸게 봤나"를 가릴 수 없다.
 */
data class ComplexityNote(
    val time: Complexity,
    val space: Complexity,
    /** 실제 식. 비우면 등급의 이름을 그대로 쓴다. */
    val note: String = "",
)

/**
 * 복잡도 등급.
 *
 * 순서가 뜻을 갖는다 — 앞이 싸고 뒤가 비싸다. 사용자의 답이 정답보다 앞이면 **비용을
 * 덜 센 것**이고 뒤면 **더 센 것**이며, 그 둘은 고쳐야 할 곳이 다르다.
 */
enum class Complexity(val label: String) {
    CONSTANT("O(1)"),
    LOGARITHMIC("O(log n)"),
    LINEAR("O(n)"),
    LINEARITHMIC("O(n log n)"),
    QUADRATIC("O(n²)"),
    CUBIC("O(n³)"),
    EXPONENTIAL("O(2^n)"),
}

/**
 * 내부 난이도 (기획서 §10.2).
 *
 * **사용자 성과와 분리해 관리한다.** 정답률은 측정값이고 이것은 저작자의 판단이며, 둘이
 * 어긋나는 것 자체가 신호다 — 쉽다고 적은 문제의 정답률이 낮으면 문제나 지문을 봐야 한다.
 *
 * 단계를 "쉬움/어려움" 같은 느낌이 아니라 **무엇이 필요한가**로 가른다. 느낌으로 매기면
 * 저작자마다 기준이 달라지고, 몇 달 뒤에는 같은 사람도 달라진다.
 */
enum class Difficulty {
    /** 자료구조 하나를 한 번 순회하면 된다. */
    INTRO,

    /** 표준 패턴 하나를 그대로 적용하면 된다. */
    EASY,

    /** 패턴 둘을 조합하거나, 관찰 하나가 있어야 풀린다. */
    MEDIUM,

    /** 비자명한 관찰이나 증명이 필요하다. */
    HARD,

    /** 관찰 여럿을 엮어야 하고, 각각이 이미 어렵다. */
    EXPERT,
}

/**
 * 역량 온톨로지 (기획서 §4.2, PRD §3.4).
 *
 * 다섯 역량군의 세부 역량이다. **이 목록은 함부로 바꾸지 않는다** — 3단계부터 모든 증거가
 * 이 위에 쌓이므로, 뒤에 이름을 바꾸면 지난 증거를 다시 해석해야 한다.
 *
 * 확장군(설명·전이·메타인지·AI 협업)은 문제가 아니라 **문제를 푼 뒤의 활동**에서 증거가
 * 나온다. 지금은 그 활동이 없어 어느 문제도 달지 않지만, 온톨로지에서 빼 두면 4단계에서
 * 다시 넣을 때 역량군 번호가 밀린다.
 */
enum class Competency(val group: CompetencyGroup, val label: String) {
    /** 지문에서 무엇을 묻는지 읽어내기. */
    READING(CompetencyGroup.UNDERSTANDING, "문제 독해"),

    /** 제약과 조건을 빠짐없이 뽑아내기. */
    CONSTRAINTS(CompetencyGroup.UNDERSTANDING, "조건 추출"),

    /** 문제를 다룰 수 있는 구조로 옮기기. */
    MODELING(CompetencyGroup.DESIGN, "모델링"),

    /** 제약에 맞는 접근 고르기. */
    ALGORITHM_CHOICE(CompetencyGroup.DESIGN, "알고리즘 선택"),

    /** 왜 맞는지 말할 수 있기 — 불변식과 증명. */
    CORRECTNESS(CompetencyGroup.DESIGN, "논리·정확성"),

    /** 정한 접근을 코드로 옮기기. */
    IMPLEMENTATION(CompetencyGroup.EXECUTION, "구현력"),

    /** 시간·공간 비용을 미리 맞히기. */
    COMPLEXITY(CompetencyGroup.EXECUTION, "복잡도 예측"),

    /** 맞는 풀이를 더 빠르거나 가볍게 만들기. */
    OPTIMIZATION(CompetencyGroup.EXECUTION, "최적화"),

    /** 무엇을 시험해야 하는지 정하기. */
    TEST_DESIGN(CompetencyGroup.VERIFICATION, "테스트 설계"),

    /** 경계에서 무너지는 자리 찾기. */
    EDGE_CASES(CompetencyGroup.VERIFICATION, "엣지케이스"),

    /** 틀렸다는 것을 보이는 입력 만들기. */
    COUNTEREXAMPLE(CompetencyGroup.VERIFICATION, "반례"),

    /** 어디서부터 어긋났는지 짚기. */
    DEBUGGING(CompetencyGroup.VERIFICATION, "디버깅"),

    /** 자기 풀이를 남에게 설명하기. */
    EXPLANATION(CompetencyGroup.EXTENSION, "설명"),

    /** 배운 것을 다른 문제로 옮기기. */
    TRANSFER(CompetencyGroup.EXTENSION, "전이"),

    /** 자기가 무엇을 모르는지 알기. */
    METACOGNITION(CompetencyGroup.EXTENSION, "메타인지"),

    /** AI 결과를 검증하고 책임지기. */
    AI_COLLABORATION(CompetencyGroup.EXTENSION, "AI 협업"),
}

/** 역량군 (기획서 §4.2). 화면에서 묶어 보여주는 단위다. */
enum class CompetencyGroup { UNDERSTANDING, DESIGN, EXECUTION, VERIFICATION, EXTENSION }
