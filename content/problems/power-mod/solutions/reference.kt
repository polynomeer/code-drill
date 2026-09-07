// 검증용 정답 (§6.1 solutions/). 분할 제곱.
//
// 지수를 반으로 접는다. 지수의 비트를 훑으며 켜져 있으면 곱하고, 매번 밑을 제곱한다.
// 곱셈 횟수가 지수가 아니라 지수의 자릿수만큼이라 1_000_000_000 도 30번이면 끝난다.
//
// Long 으로 곱한다. 두 수가 각각 10^9 에 가까우면 곱은 10^18 이라 Int 로는 넘친다.
fun powerMod(base: Int, exponent: Int): Int {
    val mod = 1_000_000_007L
    var result = 1L
    var current = base.toLong() % mod
    var remaining = exponent
    var bit = 0

    while (remaining > 0) {
        if (remaining and 1 == 1) {
            result = result * current % mod
            Drill.visit(bit, 1)
            Drill.write(0, result.toInt())
        }
        current = current * current % mod
        remaining = remaining shr 1
        bit += 1
    }
    return result.toInt()
}
