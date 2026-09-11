// kind: MISSING_EDGE_CASE
// 0 을 만나면 무조건 갇혔다고 본다. 마지막 칸의 0 은 도착이다.
fun canJump(steps: IntArray): Int {
    var reach = 0
    for (i in steps.indices) {
        if (i > reach) return 0
        if (steps[i] == 0) return 0
        reach = maxOf(reach, i + steps[i])
    }
    return 1
}
