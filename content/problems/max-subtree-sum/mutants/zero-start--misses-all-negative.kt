// kind: MISSING_EDGE_CASE
// 최댓값을 0 에서 시작한다. 전부 음수면 존재하지 않는 빈 서브트리를 답으로 낸다.
fun maxSubtreeSum(parent: IntArray, values: IntArray): Int {
    val n = parent.size
    val head = IntArray(n) { -1 }; val next = IntArray(n) { -1 }; var root = 0
    for (i in 0 until n) { val p = parent[i]; if (p == -1) root = i else { next[i] = head[p]; head[p] = i } }
    val order = IntArray(n); var size = 0
    val stack = ArrayDeque<Int>(); stack.addLast(root)
    while (stack.isNotEmpty()) { val v = stack.removeLast(); order[size++] = v; var c = head[v]; while (c != -1) { stack.addLast(c); c = next[c] } }
    val total = values.copyOf(); var best = 0
    for (k in n - 1 downTo 0) {
        val v = order[k]
        if (total[v] > best) best = total[v]
        if (parent[v] != -1) total[parent[v]] += total[v]
    }
    return best
}
