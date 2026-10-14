// kind: PERFORMANCE
// 쌍을 모두 본다. 맞지만, 20 만이면 200 억 쌍이다.
fun dominatingPairs(ys: IntArray, zs: IntArray): Int {
    var total = 0L
    for (i in ys.indices) for (j in i + 1 until ys.size) if (ys[i] < ys[j] && zs[i] < zs[j]) total += 1
    return (total % 1_000_000_007).toInt()
}
