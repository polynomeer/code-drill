// kind: MISSING_EDGE_CASE
// 자리를 이어 붙이는 경우를 빼고 한 자리씩만 쓴다.
fun expressionTargets(num: String, target: Int): Int {
    val n = num.length
    var count = 0
    fun go(pos: Int, total: Long, last: Long) {
        if (pos == n) { if (total == target.toLong()) count += 1; return }
        val value = (num[pos] - '0').toLong()
        if (pos == 0) go(pos + 1, value, value) else {
            go(pos + 1, total + value, value); go(pos + 1, total - value, -value); go(pos + 1, total - last + last * value, last * value)
        }
    }
    go(0, 0L, 0L)
    return count
}
