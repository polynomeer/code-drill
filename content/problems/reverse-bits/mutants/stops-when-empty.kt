// kind: MISSING_EDGE_CASE
// 남은 비트가 0 이 되면 멈춘다. 그 0 들이 결과의 아래쪽으로 가도록 끝까지 밀어야 한다.
fun reverseBits(n: Int): Int {
    var x = n
    var result = 0
    while (x != 0) { result = (result shl 1) or (x and 1); x = x ushr 1 }
    return result
}
