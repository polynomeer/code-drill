// 검증용 정답 (§6.1 solutions/). 유니온 파인드 + 경로 압축.
//
// 묶음 수는 정점 수에서 시작해, **서로 다른 두 묶음을 실제로 합쳤을 때만** 줄인다.
// 간선마다 줄이면 같은 간선이 반복되거나 자기 자신을 가리킬 때 음수가 된다.
//
// 경로 압축이 없으면 한 줄로 긴 그래프에서 매번 뿌리까지 걸어 올라가 O(n^2) 가 된다.
fun countComponents(n: Int, edges: IntArray): Int {
    val parent = IntArray(n) { it }

    fun find(start: Int): Int {
        var node = start
        while (parent[node] != node) {
            parent[node] = parent[parent[node]]
            node = parent[node]
        }
        return node
    }

    var count = n
    var i = 0
    while (i < edges.size) {
        val a = find(edges[i])
        val b = find(edges[i + 1])
        Drill.node("v${edges[i]}")
        if (a != b) {
            parent[a] = b
            count -= 1
            Drill.edge("v${edges[i]}", "v${edges[i + 1]}")
            Drill.write(0, count)
        }
        i += 2
    }
    return count
}
