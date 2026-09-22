// kind: PERFORMANCE
// 갱신마다 구간을 훑는다. O(n × 갱신 수).
fun applyRangeUpdates(n: Int, updates: IntArray): IntArray {
    val out = IntArray(n)
    for (i in updates.indices step 3) {
        for (j in updates[i]..updates[i + 1]) { Drill.compare(j, i); out[j] += updates[i + 2] }
    }
    return out
}
