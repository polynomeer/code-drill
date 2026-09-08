// kind: PERFORMANCE
// 2 부터 value-1 까지 전부 나눠 본다. 소수 하나를 판정하는 데 값에 비례한다.
fun countPrimes(n: Int): Int {
    var count = 0
    for (value in 2..n) {
        var prime = true
        var d = 2
        while (d < value) {
            if (value % d == 0) { prime = false; break }
            d += 1
        }
        if (prime) count += 1
    }
    return count
}
