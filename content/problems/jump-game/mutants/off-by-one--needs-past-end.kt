// kind: OFF_BY_ONE
// 마지막 칸을 넘어야 도착으로 본다. 정확히 닿는 경우를 실패로 낸다.
fun canJump(steps: IntArray): Int {
    var reach = 0
    for (i in steps.indices) {
        if (i > reach) return 0
        reach = maxOf(reach, i + steps[i])
    }
    return if (reach > steps.size - 1) 1 else 0
}
