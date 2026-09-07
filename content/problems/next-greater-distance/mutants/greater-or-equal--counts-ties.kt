// kind: WRONG_BRANCH
// 같은 값도 '더 크다'로 본다. 값이 이어지는 구간에서 틀린다.
fun nextGreater(values: IntArray): IntArray {
    val out = IntArray(values.size)
    val stack = ArrayDeque<Int>()
    for (index in values.indices) {
        while (stack.isNotEmpty() && values[stack.last()] <= values[index]) {
            val waiting = stack.removeLast()
            out[waiting] = index - waiting
        }
        stack.addLast(index)
    }
    return out
}
