// kind: WRONG_ALGORITHM
// 빼기를 오른쪽에서 왼쪽으로 뺀다.
fun differentWaysToCompute(expression: String): IntArray {
    fun go(lo: Int, hi: Int): List<Int> {
        val result = ArrayList<Int>()
        var split = false
        for (i in lo until hi) {
            val op = expression[i]
            if (op == '+' || op == '-' || op == '*') {
                split = true
                for (a in go(lo, i)) for (b in go(i + 1, hi)) result.add(when (op) { '+' -> a + b; '-' -> b - a; else -> a * b })
            }
        }
        if (!split) result.add(expression.substring(lo, hi).toInt())
        return result
    }
    return go(0, expression.length).sorted().toIntArray()
}
