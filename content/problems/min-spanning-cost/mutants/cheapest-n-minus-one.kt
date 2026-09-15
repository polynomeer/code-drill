// kind: WRONG_ALGORITHM
// 가장 싼 간선 n-1 개의 비용을 더한다. 그것들이 순환을 만들면 정점을 잇지 못한다.
fun minSpanningCost(n: Int, edges: IntArray): Int {
    val m = edges.size / 3
    if (m < n - 1) return -1
    val weights = (0 until m).map { edges[3 * it + 2] }.sorted()
    return weights.take(n - 1).sum()
}
