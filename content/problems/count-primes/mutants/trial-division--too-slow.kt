// kind: PERFORMANCE
// 수마다 나눠 본다. 작은 n 은 통과한다.
fun countPrimes(n: Int): Int {
    var count = 0
    for (value in 2..n) {
        var prime = true
        var d = 2
        while (d.toLong() * d <= value) {
            if (value % d == 0) { prime = false; break }
            d += 1
        }
        if (prime) count += 1
    }
    return count
}
