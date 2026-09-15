// 검증용 정답 (§6.1 solutions/). 5 의 거듭제곱마다 배수의 수를 더한다.
fun trailingZeros(n: Int): Int {
    var count = 0
    var power = 5L
    while (power <= n) {
        count += (n / power).toInt()
        Drill.visit(power.toInt(), count)
        power *= 5
    }
    return count
}
