// kind: OFF_BY_ONE
// 제곱근까지 세고 짝을 두 배 한다. 제곱수의 가운데 약수를 두 번 센다.
fun divisorCounts(queries: IntArray): IntArray {
    return IntArray(queries.size) { i ->
        val n = queries[i]
        var count = 0
        var d = 1
        while (d * d <= n) { if (n % d == 0) count += 2; d += 1 }
        count
    }
}
