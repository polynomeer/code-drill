// kind: WRONG_ALGORITHM
// 루트에서 시작하는 경로만 센다.
fun pathSumCount(parent: IntArray, values: IntArray, target: Int): Int {
    val n = parent.size
    val prefix = LongArray(n)
    var count = 0
    for (i in 0 until n) {
        prefix[i] = (if (parent[i] == -1) 0L else prefix[parent[i]]) + values[i]
        if (prefix[i] == target.toLong()) count += 1
    }
    return count
}
