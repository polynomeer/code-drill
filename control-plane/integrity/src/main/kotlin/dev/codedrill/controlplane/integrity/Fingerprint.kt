package dev.codedrill.controlplane.integrity

/**
 * 소스의 지문 (기술 설계서 §11.4 부정행위 방어, 기획서 §10.4 정책).
 *
 * 표절은 이름을 바꾸고 줄을 옮기는 것으로 숨기므로, 지문은 **글자가 아니라 구조**에서
 * 뜬다. 식별자는 전부 같은 토큰으로, 숫자와 문자열도 각각 하나로 접은 뒤, 토큰 k개씩의
 * 해시를 창마다 하나씩 고른다 (winnowing). 두 지문의 자카드 유사도가 신호다.
 *
 * **신호는 판정이 아니다** (§10.4 "탐지 신호를 단독 유죄 근거로 사용하지 않고"). 여기서
 * 나가는 것은 검수자가 볼 후보 쌍이고, 결정은 사람이 두 소스를 나란히 보고 한다.
 */
object Fingerprint {

    const val K = 5
    const val WINDOW = 4

    /** 지문. 해시 집합과, 짧은 소스를 거르기 위한 토큰 수. */
    data class Print(val hashes: Set<Int>, val tokenCount: Int)

    fun of(source: String): Print {
        val tokens = tokenize(source)
        if (tokens.size < K) return Print(emptySet(), tokens.size)
        val grams = IntArray(tokens.size - K + 1) { i ->
            var h = 17
            for (j in 0 until K) h = h * 31 + tokens[i + j].hashCode()
            h
        }
        // 창마다 최솟값 하나 — 같은 값이 이어지면 한 번만. 삽입·삭제에 지문이 통째로 밀리지 않는다.
        val picked = LinkedHashSet<Int>()
        if (grams.size <= WINDOW) {
            picked.add(grams.min())
        } else {
            for (start in 0..grams.size - WINDOW) {
                var best = grams[start]
                for (j in start + 1 until start + WINDOW) if (grams[j] < best) best = grams[j]
                picked.add(best)
            }
        }
        return Print(picked, tokens.size)
    }

    /** 자카드 유사도. 둘 다 비어 있으면 0 — 비교할 것이 없는 것은 같은 것이 아니다. */
    fun similarity(a: Set<Int>, b: Set<Int>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val common = a.count { it in b }
        return common.toDouble() / (a.size + b.size - common)
    }

    /**
     * 구조만 남기는 토큰화. 주석을 지우고, 식별자는 `id`, 숫자는 `num`, 문자열은 `str` 로
     * 접는다. 키워드까지 접으면 모든 루프가 같아지므로 **키워드는 그대로** 둔다 — 언어의
     * 예약어 목록을 세 언어에 맞춰 두는 대신, 흔한 것들만 안다.
     */
    fun tokenize(source: String): List<String> {
        val text = stripComments(source)
        val out = ArrayList<String>()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c.isWhitespace() -> i += 1
                c == '"' || c == '\'' -> {
                    val quote = c
                    i += 1
                    while (i < text.length && text[i] != quote) { if (text[i] == '\\') i += 1; i += 1 }
                    i += 1
                    out.add("str")
                }
                c.isDigit() -> {
                    while (i < text.length && (text[i].isLetterOrDigit() || text[i] == '.' || text[i] == '_')) i += 1
                    out.add("num")
                }
                c.isLetter() || c == '_' -> {
                    val start = i
                    while (i < text.length && (text[i].isLetterOrDigit() || text[i] == '_')) i += 1
                    val word = text.substring(start, i)
                    out.add(if (word in KEYWORDS) word else "id")
                }
                else -> { out.add(c.toString()); i += 1 }
            }
        }
        return out
    }

    private fun stripComments(source: String): String {
        val sb = StringBuilder(source.length)
        var i = 0
        while (i < source.length) {
            when {
                source.startsWith("//", i) || source.startsWith("#", i) && (i == 0 || source[i - 1] == '\n') -> {
                    while (i < source.length && source[i] != '\n') i += 1
                }
                source.startsWith("/*", i) -> {
                    val end = source.indexOf("*/", i + 2)
                    i = if (end < 0) source.length else end + 2
                }
                else -> { sb.append(source[i]); i += 1 }
            }
        }
        return sb.toString()
    }

    private val KEYWORDS = setOf(
        "fun", "val", "var", "if", "else", "for", "while", "return", "in", "when", "is", "true", "false", "null",
        "class", "object", "import", "package", "break", "continue", "do", "until", "downTo", "step", "as",
        "public", "private", "static", "int", "long", "boolean", "char", "double", "void", "new", "this",
        "def", "elif", "not", "and", "or", "None", "True", "False", "lambda", "yield", "pass", "with", "try", "except",
    )
}
