// kind: OFF_BY_ONE
// 되돌릴 때 한 자리 덜 민다.
fun bitwiseAndOfRange(left: Int, right: Int): Int {
    var lo = left; var hi = right; var shift = 0
    while (lo != hi) { lo = lo ushr 1; hi = hi ushr 1; shift += 1 }
    return if (shift == 0) lo else lo shl (shift - 1)
}
