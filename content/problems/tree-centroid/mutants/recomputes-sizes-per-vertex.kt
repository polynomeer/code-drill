// kind: PERFORMANCE
// 정점마다 서브트리 크기를 다시 센다. O(n²).
fun treeCentroid(parent: IntArray): Int {
    val n = parent.size
    var best = -1; var bestLargest = Int.MAX_VALUE
    for (v in 0 until n) {
        val size = IntArray(n) { 1 }
        for (u in n - 1 downTo 1) { Drill.compare(v, u); size[parent[u]] += size[u] }
        var largest = n - size[v]
        for (u in 1 until n) if (parent[u] == v && size[u] > largest) largest = size[u]
        if (largest < bestLargest) { bestLargest = largest; best = v }
    }
    return best
}
