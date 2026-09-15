// kind: MISSING_EDGE_CASE
// 배열 순서대로 부모에 더한다. 자식이 부모보다 앞에 오면 아직 완성 안 된 값을 더한다.
fun subtreeSizes(parent: IntArray): IntArray {
    val n = parent.size
    val size = IntArray(n) { 1 }
    for (v in n - 1 downTo 0) if (parent[v] != -1) size[parent[v]] += size[v]
    return size
}
