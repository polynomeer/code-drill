// kind: PERFORMANCE
// 매번 목록을 훑어 가장 짧은 둘을 찾는다. O(n²).
fun connectCost(lengths: IntArray): Int {
    val list = lengths.toMutableList()
    var total = 0
    while (list.size > 1) {
        var a = 0
        for (i in 1 until list.size) { Drill.compare(i, a); if (list[i] < list[a]) a = i }
        val first = list.removeAt(a)
        var b = 0
        for (i in 1 until list.size) if (list[i] < list[b]) b = i
        val second = list.removeAt(b)
        total += first + second
        list.add(first + second)
    }
    return total
}
