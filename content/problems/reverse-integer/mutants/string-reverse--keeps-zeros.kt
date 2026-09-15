// kind: WRONG_ALGORITHM
// 문자열로 뒤집어 그대로 둔다. 앞에 온 0 을 지우지 못하는 것이 아니라, 부호 문자가 끝으로 간다.
fun reverseInteger(n: Int): Int {
    val reversed = n.toString().reversed()
    return reversed.toLongOrNull()?.let { if (it < Int.MIN_VALUE || it > Int.MAX_VALUE) 0 else it.toInt() } ?: 0
}
