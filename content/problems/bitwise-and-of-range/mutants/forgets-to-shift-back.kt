// kind: MISSING_EDGE_CASE
// 같아질 때까지 밀고 되돌리지 않는다.
fun bitwiseAndOfRange(left: Int, right: Int): Int {
    var lo = left; var hi = right
    while (lo != hi) { lo = lo ushr 1; hi = hi ushr 1 }
    return lo
}
