// kind: PERFORMANCE
// 세 배수열을 병합하듯 n 번 앞으로 간다. 걸음은 n 번이라 n 이 10 억이면 끝나지 않는다.
fun nthDivisible(n: Int, a: Int, b: Int, c: Int): Int {
    var x = a.toLong(); var y = b.toLong(); var z = c.toLong()
    var current = 0L
    repeat(n) {
        current = minOf(x, minOf(y, z))
        if (x == current) x += a
        if (y == current) y += b
        if (z == current) z += c
    }
    return current.toInt()
}
