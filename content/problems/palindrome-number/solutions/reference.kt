// 검증용 정답 (§6.1 solutions/). 수를 뒤집어 견준다. Long 으로.
fun isPalindromeNumber(n: Int): Int {
    if (n < 0) return 0
    var rest = n.toLong()
    var reversed = 0L
    var i = 0
    while (rest > 0) {
        reversed = reversed * 10 + rest % 10
        rest /= 10
        Drill.compare(i, 0)
        i += 1
    }
    return if (reversed == n.toLong()) 1 else 0
}
