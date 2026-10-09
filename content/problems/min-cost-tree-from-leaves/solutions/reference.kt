// 검증용 정답 (§6.1 solutions/). 감소하는 스택 — 이웃이 확정된 작은 잎을 두 이웃 중 작은 쪽과 묶는다.
fun minCostTreeFromLeaves(arr: IntArray): Int {
    val stack = IntArray(arr.size + 1)
    var size = 0
    stack[size++] = Int.MAX_VALUE
    var total = 0L
    for (v in arr) {
        while (stack[size - 1] <= v) {
            val mid = stack[--size]
            total += mid.toLong() * minOf(stack[size - 1], v)
            Drill.pop(mid)
        }
        stack[size++] = v
        Drill.push(v)
    }
    while (size > 2) { val top = stack[--size]; total += top.toLong() * stack[size - 1] }
    return total.toInt()
}
