// kind: MISSING_EDGE_CASE
// 가장 큰 합을 0 에서 시작한다. 값이 모두 음수면 답은 0 이 아니라 가장 큰 값이다.
fun treeMaxPathSum(parent: IntArray, values: IntArray): Int {
    val n = parent.size
    val children = Array(n) { ArrayList<Int>() }
    var root = 0
    for (v in 0 until n) if (parent[v] == -1) root = v else children[parent[v]].add(v)
    val order = ArrayList<Int>(); order.add(root)
    var i = 0
    while (i < order.size) { order.addAll(children[order[i]]); i += 1 }
    val gain = IntArray(n)
    var best = 0
    for (k in n - 1 downTo 0) {
        val v = order[k]
        var first = 0; var second = 0
        for (c in children[v]) { val g = gain[c]; if (g > first) { second = first; first = g } else if (g > second) second = g }
        gain[v] = values[v] + first
        best = maxOf(best, values[v] + first + second)
    }
    return best
}
