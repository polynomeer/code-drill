// kind: WRONG_BRANCH
// 루트를 옮길 때 가까워지는 쪽과 멀어지는 쪽을 바꿔 쓴다.
fun distanceSums(parent: IntArray): IntArray {
    val n = parent.size
    val size = IntArray(n) { 1 }
    val inside = LongArray(n)
    for (i in n - 1 downTo 1) { val p = parent[i]; size[p] += size[i]; inside[p] += inside[i] + size[i] }
    val out = IntArray(n)
    out[0] = inside[0].toInt()
    for (i in 1 until n) out[i] = out[parent[i]] - n + 2 * size[i]
    return out
}
