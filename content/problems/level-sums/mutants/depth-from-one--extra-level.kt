// kind: OFF_BY_ONE
// 루트의 깊이를 1 로 둔다. 앞에 0 인 칸이 하나 생긴다.
fun levelSums(parent: IntArray, values: IntArray): IntArray {
    val n = parent.size
    val depth = IntArray(n) { -1 }
    fun depthOf(v: Int): Int {
        if (depth[v] == -1) depth[v] = if (parent[v] == -1) 1 else depthOf(parent[v]) + 1
        return depth[v]
    }
    val sums = IntArray(n + 1)
    var height = 0
    for (v in 0 until n) { val d = depthOf(v); sums[d] += values[v]; if (d > height) height = d }
    return sums.copyOf(height + 1)
}
