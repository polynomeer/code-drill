// kind: WRONG_ALGORITHM
// 자식 서브트리 크기의 합(= size − 1)을 조각으로 본다. 조각은 하나씩이다.
fun treeCentroid(parent: IntArray): Int {
    val n = parent.size
    val size = IntArray(n) { 1 }
    for (v in n - 1 downTo 1) size[parent[v]] += size[v]
    var best = -1; var bestLargest = Int.MAX_VALUE
    for (v in 0 until n) { val largest = maxOf(size[v] - 1, n - size[v]); if (largest < bestLargest) { bestLargest = largest; best = v } }
    return best
}
