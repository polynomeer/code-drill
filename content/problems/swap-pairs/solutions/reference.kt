// 검증용 정답 (§6.1 solutions/). 둘씩 묶어 제자리에서 맞바꾼다. 짝이 없는 마지막 원소는 그대로다.
fun swapPairs(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var i = 0
    while (i + 1 < a.size) {
        val t = a[i]; a[i] = a[i + 1]; a[i + 1] = t
        Drill.swap(i, i + 1)
        i += 2
    }
    return a
}
