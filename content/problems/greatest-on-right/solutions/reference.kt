// 검증용 정답 (§6.1 solutions/). 오른쪽 끝에서 왼쪽으로, 지금까지 본 가장 큰 값을 먼저 쓰고 그다음 갱신한다 — 제자리로.
fun greatestOnRight(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var best = -1
    for (i in a.indices.reversed()) {
        val original = a[i]
        a[i] = best
        Drill.write(i, best)
        if (original > best) best = original
    }
    return a
}
