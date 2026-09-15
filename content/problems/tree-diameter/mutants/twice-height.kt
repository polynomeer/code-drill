// kind: WRONG_ALGORITHM
// 루트의 높이 두 배를 답한다. 지름이 루트를 지나지 않으면 틀린다.
fun treeDiameter(parent: IntArray): Int {
    val n = parent.size
    val depth = IntArray(n) { -1 }
    fun depthOf(v: Int): Int { if (depth[v] == -1) depth[v] = if (parent[v] == -1) 0 else depthOf(parent[v]) + 1; return depth[v] }
    var height = 0
    for (v in 0 until n) height = maxOf(height, depthOf(v))
    return minOf(height * 2, n - 1)
}
