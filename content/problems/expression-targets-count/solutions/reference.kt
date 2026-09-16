// 검증용 정답 (§6.1 solutions/). 마지막 항을 들고 다니는 백트래킹.
fun expressionTargets(num: String, target: Int): Int {
    val n = num.length
    var count = 0
    fun go(pos: Int, total: Long, last: Long) {
        if (pos == n) { if (total == target.toLong()) count += 1; return }
        var value = 0L
        for (end in pos until n) {
            if (end > pos && num[pos] == '0') break
            value = value * 10 + (num[end] - '0')
            if (pos == 0) {
                Drill.call("start")
                go(end + 1, value, value)
            } else {
                Drill.call("op")
                go(end + 1, total + value, value)
                go(end + 1, total - value, -value)
                go(end + 1, total - last + last * value, last * value)
            }
        }
    }
    go(0, 0L, 0L)
    return count
}
