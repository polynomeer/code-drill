// kind: WRONG_ALGORITHM
// 자릿수를 떼어 낸 횟수를 센다. 세는 것은 '더한 횟수'다.
fun digitSumSteps(n: Int): Int {
    var value = n
    var steps = 0
    while (value >= 10) {
        var total = 0
        var rest = value
        while (rest > 0) { total += rest % 10; rest /= 10; steps += 1 }
        value = total
    }
    return steps
}
