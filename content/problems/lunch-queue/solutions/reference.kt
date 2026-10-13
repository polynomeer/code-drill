// 검증용 정답 (§6.1 solutions/). 줄의 순서는 상관없다 — 종류별로 원하는 학생 수를 세고, 더미를 위에서부터 내려가다 막히면 멈춘다.
fun lunchQueue(students: IntArray, sandwiches: IntArray): Int {
    val want = IntArray(2)
    for (s in students) want[s] += 1
    for (k in sandwiches.indices) {
        val s = sandwiches[k]
        if (want[s] == 0) return sandwiches.size - k
        want[s] -= 1
        Drill.dequeue(s)
    }
    return 0
}
