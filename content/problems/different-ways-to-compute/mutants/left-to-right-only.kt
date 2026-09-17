// kind: WRONG_ALGORITHM
// 왼쪽에서 오른쪽으로 한 번 계산한 값 하나만 낸다.
fun differentWaysToCompute(expression: String): IntArray {
    var value = expression[0] - '0'
    var i = 1
    while (i < expression.length) {
        val b = expression[i + 1] - '0'
        value = when (expression[i]) { '+' -> value + b; '-' -> value - b; else -> value * b }
        i += 2
    }
    return intArrayOf(value)
}
