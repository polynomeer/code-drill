// kind: WRONG_BRANCH
// 올림이 끝난 뒤에도 앞자리에 계속 1 을 더한다.
fun plusOne(digits: IntArray): IntArray {
    val out = digits.copyOf()
    var carry = 1
    for (i in out.indices.reversed()) {
        out[i] += carry
        if (out[i] == 10) out[i] = 0 else carry = 1
    }
    return if (out[0] == 0 && carry == 1 && digits.all { it == 9 }) intArrayOf(1) + out else out
}
