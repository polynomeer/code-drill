// 검증용 정답 (§6.1 solutions/). 비트를 32 번 꺼내 결과의 오른쪽에 붙인다.
fun reverseBits(n: Int): Int {
    var x = n
    var result = 0
    for (i in 0 until 32) {
        val bit = x and 1
        Drill.write(i, bit)
        result = (result shl 1) or bit
        x = x ushr 1
    }
    return result
}
