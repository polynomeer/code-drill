// kind: WRONG_BRANCH
// 음수인 자식 이득도 붙인다. 손해인 갈래는 버려야 한다.
fun treeMaxPathSum(parent: IntArray, values: IntArray): Int {
    val n = parent.size
    val children = Array(n) { ArrayList<Int>() }
    var root = 0
    for (v in 0 until n) if (parent[v] == -1) root = v else children[parent[v]].add(v)
    val order = ArrayList<Int>(); order.add(root)
    var i = 0
    while (i < order.size) { order.addAll(children[order[i]]); i += 1 }
    val gain = IntArray(n)
    var best = Int.MIN_VALUE
    for (k in n - 1 downTo 0) {
        val v = order[k]
        val sorted = children[v].map { gain[it] }.sortedDescending()
        val first = sorted.getOrNull(0) ?: 0
        val second = sorted.getOrNull(1) ?: 0
        gain[v] = values[v] + first
        best = maxOf(best, values[v] + first + second)
    }
    return best
}
