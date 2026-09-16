// kind: OFF_BY_ONE
// 조상과 같은 값을 좋지 않다고 본다. '크지 않으면' 좋은 것이다.
fun goodNodes(parent: IntArray, values: IntArray): Int {
    val n = parent.size
    val best = IntArray(n)
    var count = 0
    for (i in 0 until n) {
        if (parent[i] == -1) { best[i] = values[i]; count += 1 } else {
            val above = best[parent[i]]
            best[i] = maxOf(above, values[i])
            if (values[i] > above) count += 1
        }
    }
    return count
}
