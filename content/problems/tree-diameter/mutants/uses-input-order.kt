// kind: MISSING_EDGE_CASE
// 배열 순서를 뒤에서부터 훑으며 부모에 올린다. 자식이 부모보다 앞에 오면 아직 안 올라온 값을 쓴다.
fun treeDiameter(parent: IntArray): Int {
    val n = parent.size
    val down = IntArray(n)
    var best = 0
    for (v in n - 1 downTo 0) {
        val p = parent[v]
        if (p == -1) continue
        best = maxOf(best, down[p] + down[v] + 1)
        down[p] = maxOf(down[p], down[v] + 1)
    }
    return best
}
