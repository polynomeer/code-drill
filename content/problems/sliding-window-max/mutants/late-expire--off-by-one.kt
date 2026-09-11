// kind: OFF_BY_ONE
// 창을 벗어난 원소를 한 칸 늦게 버린다. 앞쪽 큰 값이 한 창 더 살아남는다.
fun windowMax(nums: IntArray, k: Int): IntArray {
    val out = IntArray(nums.size - k + 1)
    val dq = ArrayDeque<Int>()
    for (i in nums.indices) {
        while (dq.isNotEmpty() && nums[dq.last()] <= nums[i]) dq.removeLast()
        dq.addLast(i)
        if (dq.first() < i - k) dq.removeFirst()
        if (i >= k - 1) out[i - k + 1] = nums[dq.first()]
    }
    return out
}
