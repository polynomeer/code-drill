// 검증용 정답 (§6.1 solutions/). Long 으로 뒤집고 범위를 본다.
fun reverseInteger(n: Int): Int {
    var rest = n.toLong()
    var result = 0L
    while (rest != 0L) {
        val digit = rest % 10
        result = result * 10 + digit
        Drill.visit(digit.toInt(), result.toInt())
        rest /= 10
    }
    return if (result < Int.MIN_VALUE || result > Int.MAX_VALUE) 0 else result.toInt()
}
