// kind: OFF_BY_ONE
// 비트를 31 번만 꺼낸다. 32 비트를 모두 옮겨야 한다.
fun reverseBits(n: Int): Int {
    var x = n
    var result = 0
    for (i in 0 until 31) { result = (result shl 1) or (x and 1); x = x ushr 1 }
    return result
}
