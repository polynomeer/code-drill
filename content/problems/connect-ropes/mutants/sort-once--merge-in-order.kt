// kind: WRONG_ALGORITHM
// 정렬한 뒤 앞에서부터 차례로 잇는다. 이은 결과가 남은 것보다 길어지는 순간을 놓친다.
fun connectCost(lengths: IntArray): Int {
    val sorted = lengths.sorted()
    var total = 0
    var current = sorted[0]
    for (i in 1 until sorted.size) { current += sorted[i]; total += current }
    return total
}
