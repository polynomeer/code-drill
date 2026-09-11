// kind: PERFORMANCE
// 정점마다 서브트리를 다시 훑어 합한다. 사슬에서 O(n²).
fun maxSubtreeSum(parent: IntArray, values: IntArray): Int {
    val n = parent.size
    var best = Int.MIN_VALUE
    for (root in 0 until n) {
        var sum = 0
        for (v in 0 until n) {
            var u = v
            while (u != -1 && u != root) { Drill.edge(u.toString(), parent[u].toString()); u = parent[u] }
            if (u == root) sum += values[v]
        }
        if (sum > best) best = sum
    }
    return best
}
