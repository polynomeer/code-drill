// kind: WRONG_BRANCH
// 같은 값을 하나로 합친다. 다른 괄호가 같은 값을 내면 그 수만큼 들어가야 한다.
fun differentWaysToCompute(expression: String): IntArray {
    fun go(lo: Int, hi: Int): List<Int> {
        val result = ArrayList<Int>()
        var split = false
        for (i in lo until hi) {
            val op = expression[i]
            if (op == '+' || op == '-' || op == '*') {
                split = true
                for (a in go(lo, i)) for (b in go(i + 1, hi)) result.add(when (op) { '+' -> a + b; '-' -> a - b; else -> a * b })
            }
        }
        if (!split) result.add(expression.substring(lo, hi).toInt())
        return result
    }
    return go(0, expression.length).distinct().sorted().toIntArray()
}
