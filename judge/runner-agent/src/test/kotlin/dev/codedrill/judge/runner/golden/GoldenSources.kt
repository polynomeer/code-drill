package dev.codedrill.judge.runner.golden

import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.Verdict

/**
 * 판정 정확성 코퍼스 (기술 설계서 §14.2).
 *
 * 언어 × 판정 유형의 행렬이다. 각 언어의 최소 재현 코드를 한곳에 모아 두면, 새 언어를
 * 붙일 때 무엇을 만족시켜야 하는지가 목록으로 드러난다.
 *
 * 여기서 한 칸이라도 어긋나면 출시 차단 사유다 (§14.4 Correctness).
 */
object GoldenSources {

    data class Case(
        val language: Language,
        val scenario: String,
        val source: String,
        val expected: Verdict,
    )

    /** 계측 SDK 를 쓰는 정답. 판정 모드에서 no-op 으로 컴파일되는지도 함께 검증한다. */
    val ACCEPTED: Map<Language, String> = mapOf(
        Language.KOTLIN to """
            fun twoSum(nums: IntArray, target: Int): IntArray {
                val seen = HashMap<Int, Int>()
                for (i in nums.indices) {
                    Drill.visit(i, nums[i])
                    val j = seen[target - nums[i]]
                    if (j != null) {
                        Drill.match(j, i)
                        return intArrayOf(j, i)
                    }
                    Drill.compare(i, target - nums[i])
                    seen.putIfAbsent(nums[i], i)
                }
                error("정답은 항상 존재한다")
            }
        """.trimIndent(),

        Language.JAVA to """
            import java.util.HashMap;
            import java.util.Map;

            class Solution {
                public int[] twoSum(int[] nums, int target) {
                    Map<Integer, Integer> seen = new HashMap<>();
                    for (int i = 0; i < nums.length; i++) {
                        Drill.visit(i, nums[i]);
                        Integer j = seen.get(target - nums[i]);
                        if (j != null) {
                            Drill.match(j, i);
                            return new int[] { j, i };
                        }
                        Drill.compare(i, target - nums[i]);
                        seen.putIfAbsent(nums[i], i);
                    }
                    throw new IllegalStateException("정답은 항상 존재한다");
                }
            }
        """.trimIndent(),

        Language.PYTHON to """
            from drill import Drill


            def twoSum(nums, target):
                seen = {}
                for i, value in enumerate(nums):
                    Drill.visit(i, value)
                    j = seen.get(target - value)
                    if j is not None:
                        Drill.match(j, i)
                        return [j, i]
                    Drill.compare(i, target - value)
                    seen.setdefault(value, i)
                raise AssertionError("정답은 항상 존재한다")
        """.trimIndent(),
    )

    val WRONG_ANSWER: Map<Language, String> = mapOf(
        Language.KOTLIN to "fun twoSum(nums: IntArray, target: Int): IntArray = intArrayOf(0, 0)",
        Language.JAVA to """
            class Solution {
                public int[] twoSum(int[] nums, int target) { return new int[] { 0, 0 }; }
            }
        """.trimIndent(),
        Language.PYTHON to "def twoSum(nums, target):\n    return [0, 0]",
    )

    val COMPILE_ERROR: Map<Language, String> = mapOf(
        Language.KOTLIN to "fun twoSum(nums: IntArray, target: Int): IntArray { 이건 코드가 아니다 }",
        Language.JAVA to "class Solution { public int[] twoSum(int[] nums, int target) { 이건 코드가 아니다 } }",
        // 파이썬은 문법 오류만 컴파일 단계에서 잡힌다.
        Language.PYTHON to "def twoSum(nums, target)\n    return [0, 0]",
    )

    val RUNTIME_ERROR: Map<Language, String> = mapOf(
        Language.KOTLIN to """
            fun twoSum(nums: IntArray, target: Int): IntArray {
                throw IllegalStateException("의도적 실패")
            }
        """.trimIndent(),
        Language.JAVA to """
            class Solution {
                public int[] twoSum(int[] nums, int target) {
                    throw new IllegalStateException("의도적 실패");
                }
            }
        """.trimIndent(),
        Language.PYTHON to "def twoSum(nums, target):\n    raise ValueError(\"의도적 실패\")",
    )

    val TIME_LIMIT: Map<Language, String> = mapOf(
        Language.KOTLIN to """
            fun twoSum(nums: IntArray, target: Int): IntArray {
                while (true) { }
            }
        """.trimIndent(),
        Language.JAVA to """
            class Solution {
                public int[] twoSum(int[] nums, int target) {
                    while (true) { }
                }
            }
        """.trimIndent(),
        Language.PYTHON to "def twoSum(nums, target):\n    while True:\n        pass",
    )

    val MEMORY_LIMIT: Map<Language, String> = mapOf(
        Language.KOTLIN to """
            fun twoSum(nums: IntArray, target: Int): IntArray {
                val hog = ArrayList<IntArray>()
                while (true) { hog.add(IntArray(1_000_000)) }
            }
        """.trimIndent(),
        Language.JAVA to """
            import java.util.ArrayList;
            import java.util.List;

            class Solution {
                public int[] twoSum(int[] nums, int target) {
                    List<int[]> hog = new ArrayList<>();
                    while (true) { hog.add(new int[1_000_000]); }
                }
            }
        """.trimIndent(),
        Language.PYTHON to """
            def twoSum(nums, target):
                hog = []
                while True:
                    hog.append(bytearray(4 * 1024 * 1024))
        """.trimIndent(),
    )

    val OUTPUT_LIMIT: Map<Language, String> = mapOf(
        Language.KOTLIN to """
            fun twoSum(nums: IntArray, target: Int): IntArray {
                repeat(20_000) { println("noise noise noise noise") }
                return intArrayOf(0, 1)
            }
        """.trimIndent(),
        Language.JAVA to """
            class Solution {
                public int[] twoSum(int[] nums, int target) {
                    for (int i = 0; i < 20000; i++) { System.out.println("noise noise noise noise"); }
                    return new int[] { 0, 1 };
                }
            }
        """.trimIndent(),
        Language.PYTHON to """
            def twoSum(nums, target):
                for _ in range(20000):
                    print("noise noise noise noise")
                return [0, 1]
        """.trimIndent(),
    )

    /**
     * 문법은 맞지만 요구된 함수가 없다.
     *
     * 컴파일 단계가 잡는 언어(Kotlin/Java)와 하네스가 잡는 언어(Python)가 갈리지만,
     * 사용자에게 보이는 판정은 같아야 한다.
     */
    val MISSING_FUNCTION: Map<Language, String> = mapOf(
        Language.KOTLIN to "fun somethingElse(): Int = 1",
        Language.JAVA to "class Solution { public int somethingElse() { return 1; } }",
        Language.PYTHON to "def something_else():\n    return 1",
    )

    val languages = listOf(Language.KOTLIN, Language.JAVA, Language.PYTHON)

    /** 언어 × 판정 행렬 전체. */
    fun matrix(): List<Case> = buildList {
        fun add(sources: Map<Language, String>, scenario: String, expected: Verdict) {
            for (language in languages) {
                sources[language]?.let { add(Case(language, scenario, it, expected)) }
            }
        }
        add(ACCEPTED, "정답", Verdict.ACCEPTED)
        add(WRONG_ANSWER, "오답", Verdict.WRONG_ANSWER)
        add(COMPILE_ERROR, "컴파일 실패", Verdict.COMPILE_ERROR)
        add(RUNTIME_ERROR, "런타임 예외", Verdict.RUNTIME_ERROR)
        add(TIME_LIMIT, "무한 루프", Verdict.TIME_LIMIT)
        add(MEMORY_LIMIT, "메모리 폭식", Verdict.MEMORY_LIMIT)
        add(OUTPUT_LIMIT, "출력 폭주", Verdict.OUTPUT_LIMIT)
        add(MISSING_FUNCTION, "함수 없음", Verdict.COMPILE_ERROR)
    }
}
