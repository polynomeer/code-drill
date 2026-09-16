// kind: WRONG_BRANCH
// 뺄셈 뒤의 곱셈에서 마지막 항의 부호를 잃는다. 1-2*3 을 1-(-2*3) 처럼 계산한다.
fun expressionTargets(num: String, target: Int): Int {
    val n = num.length
    var count = 0
    fun go(pos: Int, total: Long, last: Long) {
        if (pos == n) { if (total == target.toLong()) count += 1; return }
        var value = 0L
        for (end in pos until n) {
            if (end > pos && num[pos] == '0') break
            value = value * 10 + (num[end] - '0')
            if (pos == 0) go(end + 1, value, value) else {
                go(end + 1, total + value, value); go(end + 1, total - value, value); go(end + 1, total - last + last * value, last * value)
            }
        }
    }
    go(0, 0L, 0L)
    return count
}
