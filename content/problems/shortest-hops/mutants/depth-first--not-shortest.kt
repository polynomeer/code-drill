// kind: WRONG_BRANCH
// 깊이 우선으로 먼저 닿은 경로를 답으로 삼는다. 지름길을 놓친다.
fun shortestHops(n: Int, edges: IntArray, target: Int): Int {
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < edges.size) {
        graph[edges[i]].add(edges[i + 1])
        graph[edges[i + 1]].add(edges[i])
        i += 2
    }
    val distance = IntArray(n) { -1 }
    val stack = ArrayDeque<Int>()
    distance[0] = 0
    stack.addLast(0)
    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        for (next in graph[node]) {
            if (distance[next] != -1) continue
            distance[next] = distance[node] + 1
            stack.addLast(next)
        }
    }
    return distance[target]
}
