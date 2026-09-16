// kind: WRONG_ALGORITHM
// 나가는 간선이 없는 정점만 안전하다고 본다. 끝 정점으로만 가는 정점도 안전하다.
fun safeNodes(n: Int, edges: IntArray): IntArray {
    val outdegree = IntArray(n)
    var i = 0
    while (i < edges.size) { outdegree[edges[i]] += 1; i += 2 }
    return (0 until n).filter { outdegree[it] == 0 }.toIntArray()
}
