// kind: WRONG_BRANCH
// 거리가 아니라 인덱스를 적는다.
fun nextGreater(values: IntArray): IntArray {
    val out = IntArray(values.size)
    val stack = ArrayDeque<Int>()
    for (index in values.indices) {
        while (stack.isNotEmpty() && values[stack.last()] < values[index]) {
            out[stack.removeLast()] = index
        }
        stack.addLast(index)
    }
    return out
}
