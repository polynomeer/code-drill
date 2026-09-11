// 검증용 정답 (§6.1 solutions/). 자식 목록을 만들고 루트에서 DFS 순서를 뽑아 뒤에서 합친다.
fun maxSubtreeSum(parent: IntArray, values: IntArray): Int {
    val n = parent.size
    val head = IntArray(n) { -1 }
    val next = IntArray(n) { -1 }
    var root = 0
    for (i in 0 until n) {
        val p = parent[i]
        if (p == -1) { root = i } else { next[i] = head[p]; head[p] = i }
    }
    // 반복 DFS 로 방문 순서를 얻는다. 재귀는 20만 깊이의 사슬에서 스택이 넘친다.
    val order = IntArray(n)
    var size = 0
    val stack = ArrayDeque<Int>()
    stack.addLast(root)
    while (stack.isNotEmpty()) {
        val v = stack.removeLast()
        order[size++] = v
        var c = head[v]
        while (c != -1) { stack.addLast(c); c = next[c] }
    }
    val total = values.copyOf()
    var best = Int.MIN_VALUE
    for (k in n - 1 downTo 0) {
        val v = order[k]
        Drill.node(v.toString())
        if (total[v] > best) { best = total[v]; Drill.match(v, best) }
        if (parent[v] != -1) {
            total[parent[v]] += total[v]
            Drill.edge(v.toString(), parent[v].toString())
        }
    }
    return best
}
