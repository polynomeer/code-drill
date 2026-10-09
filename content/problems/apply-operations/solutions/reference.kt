// 검증용 정답 (§6.1 solutions/). 바뀐 값으로 차례로 비교하고, 쓰기 포인터로 0 이 아닌 값을 앞으로 모은다.
fun applyOperations(nums: IntArray): IntArray {
    val a = nums.copyOf()
    for (i in 0 until a.size - 1) {
        if (a[i] == a[i + 1]) { a[i] *= 2; a[i + 1] = 0; Drill.write(i, a[i]) }
    }
    var write = 0
    for (v in a) if (v != 0) { a[write] = v; write += 1; Drill.pointer("write", write) }
    while (write < a.size) { a[write] = 0; write += 1 }
    return a
}
