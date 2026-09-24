// kind: WRONG_BRANCH
// 나머지를 더하기 전에 자리를 먼저 떨어뜨린다. 맨 뒷자리가 빠진다.
fun digitSumSteps(n: Int): Int {
    var value = n
    var steps = 0
    while (value >= 10) {
        var total = 0
        var rest = value
        while (rest > 0) { rest /= 10; total += rest % 10 }
        value = total
        steps += 1
    }
    return steps
}
