// kind: WRONG_BRANCH
// 절댓값을 뒤집고 부호를 잊는다.
fun reverseInteger(n: Int): Int {
    var rest = Math.abs(n.toLong())
    var result = 0L
    while (rest != 0L) { result = result * 10 + rest % 10; rest /= 10 }
    return if (result > Int.MAX_VALUE) 0 else result.toInt()
}
