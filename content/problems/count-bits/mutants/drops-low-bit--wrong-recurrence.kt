// kind: WRONG_BRANCH
// 밀어낸 마지막 비트를 더하지 않는다. 홀수마다 1 씩 모자란다.
fun countBits(n: Int): IntArray {
    val bits = IntArray(n + 1)
    for (i in 1..n) bits[i] = bits[i shr 1]
    return bits
}
