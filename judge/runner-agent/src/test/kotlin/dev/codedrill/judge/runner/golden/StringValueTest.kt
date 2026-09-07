package dev.codedrill.judge.runner.golden

import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.judge.runner.execution.ExecutionEngine
import dev.codedrill.judge.runner.execution.adapter.JavaAdapter
import dev.codedrill.judge.runner.execution.adapter.KotlinAdapter
import dev.codedrill.judge.runner.execution.adapter.PythonAdapter
import dev.codedrill.judge.runner.execution.sandbox.ProcessSandbox
import dev.codedrill.platform.problempackage.ProblemPackage
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 문자열 값 타입의 왕복 (기술 설계서 §5.3 prepare/check, §14.2 코퍼스).
 *
 * 콘텐츠 검증(§6.3)은 **코틀린 참조 구현만** 돌린다. 그래서 자바와 파이썬 하네스의
 * 문자열 인코딩이 어긋나도 30문제가 전부 통과할 수 있다 — 그 어긋남은 사용자가
 * 그 언어로 처음 제출할 때 드러난다.
 *
 * 여기서 네 방향을 모두 지난다.
 *
 * | 문제 | 입력 → 출력 |
 * |---|---|
 * | is-palindrome | STRING → INT |
 * | longest-common-prefix | STRING_ARRAY → STRING |
 * | reverse-words | STRING_ARRAY → STRING_ARRAY |
 * | count-letters | STRING → INT_ARRAY |
 *
 * **탭·쉼표·비ASCII 케이스가 핵심이다.** 프로토콜은 탭으로 필드를, 쉼표로 배열 원소를
 * 나누므로, 그 문자가 값 안에 있을 때 깨지지 않아야 인코딩이 의미가 있다. 각 문제의
 * boundary 그룹에 그런 케이스가 들어 있으므로 전체 케이스를 통과한다는 것이 곧 증명이다.
 */
class StringValueTest {

    private val engine = ExecutionEngine(
        adapters = listOf(KotlinAdapter(), JavaAdapter(), PythonAdapter())
            .associateBy { it.language },
        sandboxes = { ProcessSandbox() },
    )

    @Test
    fun `세 언어가 문자열 입력과 출력을 똑같이 다룬다`() {
        for ((problemId, sources) in SOLUTIONS) {
            val pkg = ProblemPackageLoader(CONTENT).load(problemId)

            for ((language, source) in sources) {
                val result = engine.execute(request(pkg, language, source))

                assertEquals(
                    null, result.terminalVerdict,
                    "$problemId/$language 가 시작도 못 했다: ${result.compileLog}",
                )
                val failed = result.cases.filter { it.verdict != Verdict.ACCEPTED }
                assertTrue(
                    failed.isEmpty(),
                    "$problemId/$language 실패: " +
                        failed.joinToString { "${it.groupId}/${it.caseId}=${it.verdict}" },
                )
            }
        }
    }

    @Test
    fun `같은 문자열 답은 언어와 무관하게 같은 digest 를 낸다`() {
        // digest 가 갈리면 같은 제출이 언어에 따라 다른 결과로 기록된다. 인코딩이
        // 한 언어에서만 다르면 여기서 드러난다.
        val pkg = ProblemPackageLoader(CONTENT).load("reverse-words")
        val digests = SOLUTIONS.getValue("reverse-words").map { (language, source) ->
            engine.execute(request(pkg, language, source)).resultDigest
        }

        assertEquals(1, digests.distinct().size, "언어별 digest 가 갈렸다: $digests")
    }

    private fun request(pkg: ProblemPackage, language: Language, source: String) = ExecutionRequest(
        executionId = "exec-string",
        submissionId = "sub-string",
        attempt = 1,
        fencingToken = FencingToken(1),
        correlationId = "corr-string",
        problemVersionId = pkg.problemVersionId,
        packageDigest = pkg.packageDigest,
        language = language,
        source = source,
        signature = pkg.manifest.signature,
        limits = pkg.manifest.limits,
        groups = pkg.groups.map { RequestedGroup(it.policy, it.cases) },
    )

    private companion object {
        val CONTENT: Path = Path.of("../../content/problems")

        /**
         * 문제별 세 언어 풀이.
         *
         * 코틀린은 패키지의 참조 구현을 그대로 쓰지 않고 여기 다시 적는다. 참조 구현이
         * 바뀌면 이 테스트도 함께 봐야 한다는 뜻이며, 그 편이 낫다 — 이 테스트가 지키는
         * 것은 정답이 아니라 **세 언어가 같은 형식으로 값을 주고받는다**는 사실이다.
         */
        val SOLUTIONS: Map<String, Map<Language, String>> = mapOf(
            // STRING → INT
            "is-palindrome" to mapOf(
                Language.KOTLIN to """
                    fun isPalindrome(text: String): Int {
                        val kept = text.filter { it.isLetterOrDigit() }.lowercase()
                        return if (kept == kept.reversed()) 1 else 0
                    }
                """.trimIndent(),

                Language.JAVA to """
                    class Solution {
                        public int isPalindrome(String text) {
                            StringBuilder kept = new StringBuilder();
                            for (char c : text.toCharArray()) {
                                if (Character.isLetterOrDigit(c)) kept.append(Character.toLowerCase(c));
                            }
                            String forward = kept.toString();
                            String backward = kept.reverse().toString();
                            return forward.equals(backward) ? 1 : 0;
                        }
                    }
                """.trimIndent(),

                Language.PYTHON to """
                    def isPalindrome(text):
                        kept = [c.lower() for c in text if c.isalnum()]
                        return 1 if kept == kept[::-1] else 0
                """.trimIndent(),
            ),

            // STRING_ARRAY → STRING
            "longest-common-prefix" to mapOf(
                Language.KOTLIN to """
                    fun commonPrefix(words: Array<String>): String {
                        var shortest = words[0]
                        for (word in words) if (word.length < shortest.length) shortest = word
                        for (index in shortest.indices) {
                            for (word in words) {
                                if (word[index] != shortest[index]) return shortest.substring(0, index)
                            }
                        }
                        return shortest
                    }
                """.trimIndent(),

                Language.JAVA to """
                    class Solution {
                        public String commonPrefix(String[] words) {
                            String shortest = words[0];
                            for (String word : words) {
                                if (word.length() < shortest.length()) shortest = word;
                            }
                            for (int i = 0; i < shortest.length(); i++) {
                                for (String word : words) {
                                    if (word.charAt(i) != shortest.charAt(i)) return shortest.substring(0, i);
                                }
                            }
                            return shortest;
                        }
                    }
                """.trimIndent(),

                Language.PYTHON to """
                    def commonPrefix(words):
                        shortest = min(words, key=len)
                        for i, ch in enumerate(shortest):
                            for word in words:
                                if word[i] != ch:
                                    return shortest[:i]
                        return shortest
                """.trimIndent(),
            ),

            // STRING_ARRAY → STRING_ARRAY
            "reverse-words" to mapOf(
                Language.KOTLIN to """
                    fun reverseWords(words: Array<String>): Array<String> =
                        Array(words.size) { words[words.size - 1 - it].reversed() }
                """.trimIndent(),

                Language.JAVA to """
                    class Solution {
                        public String[] reverseWords(String[] words) {
                            String[] out = new String[words.length];
                            for (int i = 0; i < words.length; i++) {
                                out[i] = new StringBuilder(words[words.length - 1 - i]).reverse().toString();
                            }
                            return out;
                        }
                    }
                """.trimIndent(),

                Language.PYTHON to """
                    def reverseWords(words):
                        return [w[::-1] for w in reversed(words)]
                """.trimIndent(),
            ),

            // STRING → INT_ARRAY
            "count-letters" to mapOf(
                Language.KOTLIN to """
                    fun countLetters(text: String): IntArray {
                        val counts = IntArray(26)
                        for (ch in text) {
                            val lower = ch.lowercaseChar()
                            if (lower in 'a'..'z') counts[lower - 'a'] += 1
                        }
                        return counts
                    }
                """.trimIndent(),

                Language.JAVA to """
                    class Solution {
                        public int[] countLetters(String text) {
                            int[] counts = new int[26];
                            for (char c : text.toCharArray()) {
                                char lower = Character.toLowerCase(c);
                                if (lower >= 'a' && lower <= 'z') counts[lower - 'a'] += 1;
                            }
                            return counts;
                        }
                    }
                """.trimIndent(),

                Language.PYTHON to """
                    def countLetters(text):
                        counts = [0] * 26
                        for ch in text.lower():
                            if "a" <= ch <= "z":
                                counts[ord(ch) - ord("a")] += 1
                        return counts
                """.trimIndent(),
            ),
        )
    }
}
