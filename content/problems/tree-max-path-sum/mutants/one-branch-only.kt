// kind: WRONG_ALGORITHM
// 꼭대기에서 한 갈래로만 내려가는 경로를 본다. 두 갈래로 꺾는 경로가 더 클 수 있다.
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
        val first = children[v].maxOfOrNull { maxOf(gain[it], 0) } ?: 0
        gain[v] = values[v] + first
        best = maxOf(best, gain[v])
    }
    return best
}
