// kind: PERFORMANCE
// 경로 압축이 없어 한 줄로 긴 그래프에서 매번 뿌리까지 걸어 올라간다.
fun countComponents(n: Int, edges: IntArray): Int {
    val parent = IntArray(n) { it }
    fun find(start: Int): Int {
        var node = start
        while (parent[node] != node) node = parent[node]
        return node
    }
    var count = n
    var i = 0
    while (i < edges.size) {
        val a = find(edges[i])
        val b = find(edges[i + 1])
        if (a != b) { parent[b] = a; count -= 1 }
        i += 2
    }
    return count
}
