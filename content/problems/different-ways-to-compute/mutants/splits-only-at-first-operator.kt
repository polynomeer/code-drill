// kind: MISSING_EDGE_CASE
// 첫 연산자에서만 나눈다. 왼쪽이 먼저 계산되는 괄호만 센다.
fun differentWaysToCompute(expression: String): IntArray {
    fun go(lo: Int, hi: Int): List<Int> {
        val result = ArrayList<Int>()
        for (i in lo until hi) {
            val op = expression[i]
            if (op == '+' || op == '-' || op == '*') {
                for (a in go(lo, i)) for (b in go(i + 1, hi)) result.add(when (op) { '+' -> a + b; '-' -> a - b; else -> a * b })
                return result
            }
        }
        result.add(expression.substring(lo, hi).toInt())
        return result
    }
    return go(0, expression.length).sorted().toIntArray()
}
