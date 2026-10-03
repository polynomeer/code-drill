package dev.codedrill.platform.problempackage

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * 문제 번호 — 사람이 부르는 이름 (docs/ui-overhaul.md §6.1).
 *
 * slug 는 주소에 쓰는 이름이고, 사람은 "1042번"처럼 번호로 부르고 건넨다. 번호는
 * `content/problems/numbers.yaml` 한 파일에 한 줄씩 산다. 카탈로그마다 흩어 두면 겹친 번호와
 * "다음 번호"를 알려면 190개 파일을 다 열어야 한다.
 *
 * **한 번 준 번호는 바꾸지 않는다.** 번호가 옮겨 가면 사람들이 적어 둔 번호가 다른 문제를
 * 가리킨다. 그래서 이 파일은 맨 아래에 한 줄씩 더하기만 한다. 겹친 번호·겹친 slug 는 읽을 때
 * 막고, 빠진 문제와 없는 slug 는 검증 파이프라인이 막는다 (저장소 전체를 봐야 알 수 있다).
 *
 * 판정을 바꾸지 않으므로 digest 밖이다 (카탈로그와 같은 이유).
 */
class ProblemNumbers private constructor(private val bySlug: Map<String, Int>) {

    fun of(slug: String): Int? = bySlug[slug]

    /** 번호로 slug 를 찾는다. 검색창에 "1042" 를 친 사람을 위해서다. */
    fun slugOf(number: Int): String? = bySlug.entries.firstOrNull { it.value == number }?.key

    val slugs: Set<String> get() = bySlug.keys

    companion object {
        const val FILE = "numbers.yaml"

        private val LINE = Regex("""^\s*(\d+)\s*:\s*([a-z0-9][a-z0-9-]*)\s*(#.*)?$""")

        /** 파일이 없으면 빈 번호표다 — 번호가 없을 뿐 문제는 그대로 보인다. */
        fun load(problemsRoot: Path): ProblemNumbers {
            val file = problemsRoot.resolve(FILE)
            return if (file.exists()) parse(file.readText()) else ProblemNumbers(emptyMap())
        }

        /**
         * `1042: two-sum` 줄을 읽는다. 구조가 이것뿐이라 YAML 파서를 들이지 않는다 — 들이면
         * 숫자 키가 문자열인지 정수인지를 파서 설정에 맡기게 된다.
         */
        fun parse(text: String): ProblemNumbers {
            val bySlug = linkedMapOf<String, Int>()
            val seen = mutableMapOf<Int, String>()
            text.lineSequence().forEachIndexed { index, raw ->
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
                val match = requireNotNull(LINE.matchEntire(line)) {
                    "$FILE ${index + 1}번 줄을 읽지 못했다: `$raw` — `1042: slug` 모양이어야 한다"
                }
                val number = match.groupValues[1].toInt()
                val slug = match.groupValues[2]
                require(number !in seen) { "$FILE: 번호 $number 가 ${seen[number]} 과 $slug 에 겹친다" }
                require(slug !in bySlug) { "$FILE: $slug 에 번호가 둘이다 (${bySlug[slug]}, $number)" }
                seen[number] = slug
                bySlug[slug] = number
            }
            return ProblemNumbers(bySlug)
        }
    }
}
