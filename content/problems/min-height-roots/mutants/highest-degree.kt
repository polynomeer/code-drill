// kind: WRONG_ALGORITHM
// 차수가 가장 큰 정점을 고른다. 이웃이 많다고 가운데인 것은 아니다.
fun minHeightRoots(n: Int, edges: IntArray): IntArray {
    val degree = IntArray(n)
    for (v in edges) degree[v] += 1
    val top = degree.maxOrNull() ?: 0
    return (0 until n).filter { degree[it] == top }.toIntArray()
}
