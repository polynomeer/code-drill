// kind: MISSING_EDGE_CASE
// 음수의 절댓값으로 본다. -121 이 회문이 된다.
fun isPalindromeNumber(n: Int): Int {
    val s = Math.abs(n.toLong()).toString()
    return if (s == s.reversed()) 1 else 0
}
