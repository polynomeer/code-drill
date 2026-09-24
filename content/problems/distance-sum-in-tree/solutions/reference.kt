// 검증용 정답 (§6.1 solutions/). 서브트리 크기 한 번, 루트 하나의 답, 그리고 루트 옮기기.
fun distanceSums(parent: IntArray): IntArray {
    val n = parent.size
    val size = IntArray(n) { 1 }
    val inside = LongArray(n)
    for (i in n - 1 downTo 1) {
        val p = parent[i]
        size[p] += size[i]
        inside[p] += inside[i] + size[i]
        Drill.visit(p, size[p])
    }
    val out = IntArray(n)
    out[0] = inside[0].toInt()
    for (i in 1 until n) {
        out[i] = out[parent[i]] + n - 2 * size[i]
        Drill.write(i, out[i])
    }
    return out
}
