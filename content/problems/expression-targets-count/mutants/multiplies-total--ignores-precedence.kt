// kind: WRONG_ALGORITHM
// 곱셈을 지금까지의 합 전체에 적용한다. 곱셈이 먼저라는 규칙을 어긴다.
fun expressionTargets(num: String, target: Int): Int {
    val n = num.length
    var count = 0
    fun go(pos: Int, total: Long) {
        if (pos == n) { if (total == target.toLong()) count += 1; return }
        var value = 0L
        for (end in pos until n) {
            if (end > pos && num[pos] == '0') break
            value = value * 10 + (num[end] - '0')
            if (pos == 0) go(end + 1, value) else { go(end + 1, total + value); go(end + 1, total - value); go(end + 1, total * value) }
        }
    }
    go(0, 0L)
    return count
}
