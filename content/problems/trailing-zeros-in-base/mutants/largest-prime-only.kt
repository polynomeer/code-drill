// kind: WRONG_ALGORITHM
// 가장 큰 소인수만 본다. 작은 소인수가 더 적게 들어 있으면 그것이 답을 정한다.
fun trailingZerosInBase(n: Int, base: Int): Int {
    var b = base; var largest = 1; var exponent = 0
    var d = 2
    while (d.toLong() * d <= b) {
        if (b % d == 0) { var e = 0; while (b % d == 0) { b /= d; e += 1 }; largest = d; exponent = e }
        d += 1
    }
    if (b > 1) { largest = b; exponent = 1 }
    var q = n; var count = 0
    while (q > 0) { q /= largest; count += q }
    return count / exponent
}
