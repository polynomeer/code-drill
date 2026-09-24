// kind: OFF_BY_ONE
// 10 보다 클 때만 더한다. 10 자신이 한 자리로 취급된다.
fun digitSumSteps(n: Int): Int {
    var value = n
    var steps = 0
    while (value > 10) {
        var total = 0
        var rest = value
        while (rest > 0) { total += rest % 10; rest /= 10 }
        value = total
        steps += 1
    }
    return steps
}
