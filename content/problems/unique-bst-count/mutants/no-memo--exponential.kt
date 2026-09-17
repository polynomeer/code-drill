// kind: PERFORMANCE
// 기억하지 않는다. 같은 크기를 지수 번 다시 센다.
fun uniqueBstCount(n: Int): Int {
    fun go(k: Int): Int {
        if (k <= 1) return 1
        var total = 0
        for (root in 1..k) { Drill.compare(root - 1, k - root); total += go(root - 1) * go(k - root) }
        return total
    }
    return go(n)
}
