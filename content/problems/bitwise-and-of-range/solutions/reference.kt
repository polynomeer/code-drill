// 검증용 정답 (§6.1 solutions/). 같아질 때까지 밀고 되돌린다.
fun bitwiseAndOfRange(left: Int, right: Int): Int {
    var lo = left; var hi = right; var shift = 0
    while (lo != hi) { Drill.compare(lo, hi); lo = lo ushr 1; hi = hi ushr 1; shift += 1; Drill.write(0, shift) }
    return lo shl shift
}
