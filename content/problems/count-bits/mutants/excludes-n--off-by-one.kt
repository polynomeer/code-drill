// kind: OFF_BY_ONE
// n 을 빼고 n-1 까지만 만든다. 길이가 하나 모자란다.
fun countBits(n: Int): IntArray {
    val bits = IntArray(n)
    for (i in 1 until n) bits[i] = bits[i shr 1] + (i and 1)
    return bits
}
