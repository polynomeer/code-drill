// kind: WRONG_ALGORITHM
// 음수인 자식 서브트리를 더하지 않는다. 그것은 '최대 경로'이지 서브트리 합이 아니다.
fun maxSubtreeSum(parent: IntArray, values: IntArray): Int {
    val n = parent.size
    val head = IntArray(n) { -1 }; val next = IntArray(n) { -1 }; var root = 0
    for (i in 0 until n) { val p = parent[i]; if (p == -1) root = i else { next[i] = head[p]; head[p] = i } }
    val order = IntArray(n); var size = 0
    val stack = ArrayDeque<Int>(); stack.addLast(root)
    while (stack.isNotEmpty()) { val v = stack.removeLast(); order[size++] = v; var c = head[v]; while (c != -1) { stack.addLast(c); c = next[c] } }
    val total = values.copyOf(); var best = Int.MIN_VALUE
    for (k in n - 1 downTo 0) {
        val v = order[k]
        if (total[v] > best) best = total[v]
        if (parent[v] != -1 && total[v] > 0) total[parent[v]] += total[v]
    }
    return best
}
