// kind: WRONG_BRANCH
// 이미 본 정점에 다시 닿으면 순환이라고 본다. 다이아몬드에서 틀린다.
fun hasCycle(n: Int, edges: IntArray): Int {
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < edges.size) { graph[edges[i]].add(edges[i + 1]); i += 2 }
    val seen = BooleanArray(n)
    for (root in 0 until n) {
        if (seen[root]) continue
        val stack = ArrayDeque<Int>()
        stack.addLast(root)
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            if (seen[node]) return 1
            seen[node] = true
            for (next in graph[node]) stack.addLast(next)
        }
    }
    return 0
}
