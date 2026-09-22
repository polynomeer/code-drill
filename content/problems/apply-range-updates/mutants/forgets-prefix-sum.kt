// kind: WRONG_ALGORITHM
// 차분을 그대로 돌려준다. 누적합을 안 한다.
fun applyRangeUpdates(n: Int, updates: IntArray): IntArray {
    val diff = IntArray(n + 1)
    for (i in updates.indices step 3) { diff[updates[i]] += updates[i + 2]; diff[updates[i + 1] + 1] -= updates[i + 2] }
    return diff.copyOf(n)
}
