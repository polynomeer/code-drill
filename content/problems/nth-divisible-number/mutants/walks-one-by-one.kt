// kind: PERFORMANCE
// 1 부터 하나씩 올라가며 센다. 답이 20 억이면 20 억 걸음이다.
fun nthDivisible(n: Int, a: Int, b: Int, c: Int): Int {
    var x = 0
    var k = 0
    while (k < n) { x += 1; if (x % a == 0 || x % b == 0 || x % c == 0) k += 1 }
    return x
}
