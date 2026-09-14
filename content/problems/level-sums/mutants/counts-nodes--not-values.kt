// kind: WRONG_BRANCH
// 값 대신 정점 수를 더한다.
fun levelSums(parent: IntArray, values: IntArray): IntArray {
    val n = parent.size
    val depth = IntArray(n) { -1 }
    fun depthOf(v: Int): Int {
        if (depth[v] == -1) depth[v] = if (parent[v] == -1) 0 else depthOf(parent[v]) + 1
        return depth[v]
    }
    var height = 0
    for (v in 0 until n) height = maxOf(height, depthOf(v))
    val sums = IntArray(height + 1)
    for (v in 0 until n) sums[depth[v]] += 1
    return sums
}
