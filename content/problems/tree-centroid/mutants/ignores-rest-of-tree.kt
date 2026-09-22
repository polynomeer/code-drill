// kind: MISSING_EDGE_CASE
// 나머지 조각(n − size[v])을 보지 않는다. 루트가 늘 중심이 된다.
fun treeCentroid(parent: IntArray): Int {
    val n = parent.size
    val size = IntArray(n) { 1 }
    for (v in n - 1 downTo 1) size[parent[v]] += size[v]
    val largestChild = IntArray(n)
    for (v in 1 until n) if (size[v] > largestChild[parent[v]]) largestChild[parent[v]] = size[v]
    var best = -1; var bestLargest = Int.MAX_VALUE
    for (v in 0 until n) { val largest = largestChild[v]; if (largest < bestLargest) { bestLargest = largest; best = v } }
    return best
}
