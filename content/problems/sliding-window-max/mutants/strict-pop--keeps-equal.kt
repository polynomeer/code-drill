// kind: WRONG_BRANCH
// 같은 값을 버리지 않는다. 답은 같지만 덱이 커져 성능 그룹에서 드러나거나, 창을 벗어난 같은 값이 남아 틀린다.
fun windowMax(nums: IntArray, k: Int): IntArray {
    val out = IntArray(nums.size - k + 1)
    val dq = ArrayDeque<Int>()
    for (i in nums.indices) {
        while (dq.isNotEmpty() && nums[dq.last()] < nums[i]) dq.removeLast()
        dq.addLast(i)
        if (dq.first() < i - k) dq.removeFirst()
        if (i >= k - 1) out[i - k + 1] = nums[dq.first()]
    }
    return out
}
