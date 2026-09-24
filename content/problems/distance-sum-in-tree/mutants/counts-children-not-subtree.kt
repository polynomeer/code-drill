// kind: MISSING_EDGE_CASE
// 서브트리 크기 대신 자식 수를 쓴다. 손자 아래가 세어지지 않는다.
fun distanceSums(parent: IntArray): IntArray {
    val n = parent.size
    val size = IntArray(n) { 1 }
    val children = IntArray(n)
    val inside = LongArray(n)
    for (i in n - 1 downTo 1) { val p = parent[i]; size[p] += size[i]; children[p] += 1; inside[p] += inside[i] + size[i] }
    val out = IntArray(n)
    out[0] = inside[0].toInt()
    for (i in 1 until n) out[i] = out[parent[i]] + n - 2 * (children[i] + 1)
    return out
}
