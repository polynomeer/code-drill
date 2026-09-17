// 검증용 정답 (§6.1 solutions/). 연산자마다 나누고 양쪽의 값을 조합한다.
fun differentWaysToCompute(expression: String): IntArray {
    val memo = HashMap<Long, List<Int>>()
    fun go(lo: Int, hi: Int): List<Int> {
        val key = lo.toLong() * 100 + hi
        memo[key]?.let { return it }
        Drill.compare(lo, hi)
        val result = ArrayList<Int>()
        var split = false
        for (i in lo until hi) {
            val op = expression[i]
            if (op == '+' || op == '-' || op == '*') {
                split = true
                for (a in go(lo, i)) for (b in go(i + 1, hi)) {
                    result.add(when (op) { '+' -> a + b; '-' -> a - b; else -> a * b })
                }
            }
        }
        if (!split) result.add(expression.substring(lo, hi).toInt())
        memo[key] = result
        return result
    }
    val out = go(0, expression.length).sorted().toIntArray()
    out.forEachIndexed { i, v -> Drill.write(i, v) }
    return out
}
