// kind: MISSING_EDGE_CASE
// 차분 배열을 n 칸으로 둔다. r 이 마지막이면 배열 밖을 쓴다.
fun applyRangeUpdates(n: Int, updates: IntArray): IntArray {
    val diff = IntArray(n)
    for (i in updates.indices step 3) { diff[updates[i]] += updates[i + 2]; diff[updates[i + 1] + 1] -= updates[i + 2] }
    val out = IntArray(n); var running = 0
    for (i in 0 until n) { running += diff[i]; out[i] = running }
    return out
}
