// kind: WRONG_ALGORITHM
// 정렬한 뒤 이웃끼리 짝지어 잇는다. 짧은 것 셋을 먼저 잇는 편이 싼 경우를 놓친다.
fun connectCost(lengths: IntArray): Int {
    var current = lengths.sorted()
    var total = 0
    while (current.size > 1) {
        val next = mutableListOf<Int>()
        var i = 0
        while (i + 1 < current.size) { total += current[i] + current[i + 1]; next.add(current[i] + current[i + 1]); i += 2 }
        if (i < current.size) next.add(current[i])
        current = next.sorted()
    }
    return total
}
