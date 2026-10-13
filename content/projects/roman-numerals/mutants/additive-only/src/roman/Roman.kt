package roman

// kind: WRONG_ALGORITHM
// 모든 기호를 더하기만 한다. 작은 기호가 큰 기호 앞에 오면 빼야 한다 — IV 는 4 다.

/**
 * 로마 숫자. 쓰기는 큰 값부터 탐욕으로, 읽기는 값을 구한 뒤 다시 써서 원래 글자와 같은지 본다 — 표준 꼴만 받는다.
 */
object Roman {

    private val values = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
    private val symbols = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
    private val digit = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)

    fun toRoman(n: Int): String {
        require(n in 1..3999) { "out of range: $n" }
        val out = StringBuilder()
        var rest = n
        for (i in values.indices) {
            while (rest >= values[i]) {
                out.append(symbols[i])
                rest -= values[i]
            }
        }
        return out.toString()
    }

    fun fromRoman(text: String): Int {
        require(text.isNotEmpty()) { "empty numeral" }
        var total = 0
        for (i in text.indices) {
            val value = digit[text[i]] ?: throw IllegalArgumentException("bad symbol: ${text[i]}")
            val next = if (i + 1 < text.length) digit[text[i + 1]] ?: 0 else 0
            total += value
        }
        // 값만 맞고 꼴이 다른 것(IIII, IC, VV)은 다시 썼을 때 달라진다
        require(total in 1..3999) { "out of range" }
        return total
    }
}
