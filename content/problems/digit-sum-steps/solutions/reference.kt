// 검증용 정답 (§6.1 solutions/). 두 자리 이상인 동안 자릿수를 더한다.
fun digitSumSteps(n: Int): Int {
    var value = n
    var steps = 0
    while (value >= 10) {
        var total = 0
        var rest = value
        while (rest > 0) { total += rest % 10; rest /= 10 }
        value = total
        steps += 1
        Drill.write(0, value)
    }
    return steps
}
