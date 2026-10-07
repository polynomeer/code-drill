// kind: MISSING_EDGE_CASE
// 재귀로 내려간다. 트리가 20 만 깊이의 사슬이면 스택이 넘친다.
fun treeMaxPathSum(parent: IntArray, values: IntArray): Int {
    val n = parent.size
    val children = Array(n) { ArrayList<Int>() }
    var root = 0
    for (v in 0 until n) if (parent[v] == -1) root = v else children[parent[v]].add(v)
    var best = Int.MIN_VALUE
    fun gain(v: Int): Int {
        var first = 0; var second = 0
        for (c in children[v]) { val g = gain(c); if (g > first) { second = first; first = g } else if (g > second) second = g }
        best = maxOf(best, values[v] + first + second)
        return values[v] + first
    }
    gain(root)
    return best
}
