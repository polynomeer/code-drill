// 검증용 정답 (§6.1 solutions/). 단조 감소 스택.
//
// 아직 답을 못 찾은 위치를 스택에 쌓아 둔다. 새 값이 들어오면 그보다 작은 것들의
// 답이 한꺼번에 정해진다. 각 위치는 한 번 쌓이고 한 번 빠지므로 O(n) 이다.
fun nextGreater(values: IntArray): IntArray {
    val out = IntArray(values.size)
    val stack = ArrayDeque<Int>()

    for (index in values.indices) {
        Drill.visit(index, values[index])

        while (stack.isNotEmpty() && values[stack.last()] < values[index]) {
            val waiting = stack.removeLast()
            Drill.pop(waiting)
            out[waiting] = index - waiting
            Drill.write(waiting, out[waiting])
        }
        stack.addLast(index)
        Drill.push(index)
    }
    return out
}
