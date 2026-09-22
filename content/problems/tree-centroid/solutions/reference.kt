// 검증용 정답 (§6.1 solutions/). 서브트리 크기 한 번, 정점마다 조각의 최댓값.
fun treeCentroid(parent: IntArray): Int {
    val n = parent.size
    val size = IntArray(n) { 1 }
    for (v in n - 1 downTo 1) size[parent[v]] += size[v]
    // 자식 서브트리 중 가장 큰 것.
    val largestChild = IntArray(n)
    for (v in 1 until n) if (size[v] > largestChild[parent[v]]) largestChild[parent[v]] = size[v]
    var best = -1; var bestLargest = Int.MAX_VALUE
    for (v in 0 until n) {
        val largest = maxOf(largestChild[v], n - size[v])
        Drill.compare(v, largest)
        if (largest < bestLargest) { bestLargest = largest; best = v; Drill.write(0, v) }
    }
    return best
}
