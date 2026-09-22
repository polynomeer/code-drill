// kind: OFF_BY_ONE
// 차분을 r 에서 뺀다. 구간의 마지막 원소가 빠진다.
fun applyRangeUpdates(n: Int, updates: IntArray): IntArray {
    val diff = IntArray(n + 1)
    for (i in updates.indices step 3) { diff[updates[i]] += updates[i + 2]; diff[updates[i + 1]] -= updates[i + 2] }
    val out = IntArray(n); var running = 0
    for (i in 0 until n) { running += diff[i]; out[i] = running }
    return out
}
