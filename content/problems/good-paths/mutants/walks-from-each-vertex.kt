// kind: PERFORMANCE
// 정점마다 트리를 훑으며 지나온 최댓값을 들고 같은 값의 정점을 센다. 정점 수의 제곱이다.
fun goodPaths(vals: IntArray, edges: IntArray): Int {
    val n = vals.size
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    var total = 0L
    val stack = IntArray(n); val from = IntArray(n)
    for (s in 0 until n) {
        total += 1
        var top = 0
        stack[top] = s; from[top] = -1; top += 1
        while (top > 0) {
            top -= 1
            val v = stack[top]; val p = from[top]
            for (u in adj[v]) {
                if (u == p || vals[u] > vals[s]) continue
                Drill.compare(s, u)
                if (u > s && vals[u] == vals[s]) total += 1
                stack[top] = u; from[top] = v; top += 1
            }
        }
    }
    return total.toInt()
}
