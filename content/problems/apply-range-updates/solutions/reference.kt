// 검증용 정답 (§6.1 solutions/). 차분 두 칸, 누적합 한 번.
fun applyRangeUpdates(n: Int, updates: IntArray): IntArray {
    val diff = IntArray(n + 1)
    for (i in updates.indices step 3) {
        val l = updates[i]; val r = updates[i + 1]; val v = updates[i + 2]
        diff[l] += v; diff[r + 1] -= v
        Drill.write(l, v)
    }
    val out = IntArray(n)
    var running = 0
    for (i in 0 until n) { running += diff[i]; out[i] = running; Drill.compare(i, running) }
    return out
}
