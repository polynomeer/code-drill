// kind: OFF_BY_ONE
// 같으면 번호가 큰 것을 고른다.
fun treeCentroid(parent: IntArray): Int {
    val n = parent.size
    val size = IntArray(n) { 1 }
    for (v in n - 1 downTo 1) size[parent[v]] += size[v]
    val largestChild = IntArray(n)
    for (v in 1 until n) if (size[v] > largestChild[parent[v]]) largestChild[parent[v]] = size[v]
    var best = -1; var bestLargest = Int.MAX_VALUE
    for (v in 0 until n) { val largest = maxOf(largestChild[v], n - size[v]); if (largest <= bestLargest) { bestLargest = largest; best = v } }
    return best
}
