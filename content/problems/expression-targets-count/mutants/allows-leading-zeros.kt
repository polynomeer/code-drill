// kind: MISSING_EDGE_CASE
// 앞에 0 이 오는 수를 허용한다. 05 를 5 로 센다.
fun expressionTargets(num: String, target: Int): Int {
    val n = num.length
    var count = 0
    fun go(pos: Int, total: Long, last: Long) {
        if (pos == n) { if (total == target.toLong()) count += 1; return }
        var value = 0L
        for (end in pos until n) {
            value = value * 10 + (num[end] - '0')
            if (pos == 0) go(end + 1, value, value) else {
                go(end + 1, total + value, value); go(end + 1, total - value, -value); go(end + 1, total - last + last * value, last * value)
            }
        }
    }
    go(0, 0L, 0L)
    return count
}
