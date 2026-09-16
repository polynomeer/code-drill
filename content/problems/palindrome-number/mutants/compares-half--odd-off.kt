// kind: OFF_BY_ONE
// 앞 절반과 뒤 절반을 견주되 가운데 자리를 뒤 절반에 넣는다. 홀수 자리에서 틀린다.
fun isPalindromeNumber(n: Int): Int {
    if (n < 0) return 0
    val s = n.toString()
    val half = s.length / 2
    val front = s.substring(0, half)
    val back = s.substring(half).reversed()
    return if (front == back) 1 else 0
}
