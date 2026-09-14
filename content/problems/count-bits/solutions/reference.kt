// 검증용 정답 (§6.1 solutions/). bits[i] = bits[i shr 1] + (i and 1).
fun countBits(n: Int): IntArray {
    val bits = IntArray(n + 1)
    for (i in 1..n) {
        bits[i] = bits[i shr 1] + (i and 1)
        Drill.write(i, bits[i])
    }
    return bits
}
