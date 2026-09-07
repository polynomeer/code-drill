// 검증용 정답 (§6.1 solutions/). 에라토스테네스의 체.
//
// 나눠 보는 대신 지워 나간다. i 의 배수를 지울 때 i*i 부터 시작하는 것이 요령이다 —
// 그보다 작은 배수는 더 작은 소수가 이미 지웠다.
fun countPrimes(n: Int): Int {
    if (n < 2) return 0

    val composite = BooleanArray(n + 1)
    var count = 0

    for (value in 2..n) {
        if (composite[value]) continue
        count += 1
        Drill.visit(value, 1)

        if (value.toLong() * value > n) continue
        var multiple = value.toLong() * value
        while (multiple <= n) {
            composite[multiple.toInt()] = true
            multiple += value
        }
    }
    return count
}
