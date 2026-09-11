// 검증용 정답 (§6.1 solutions/). 인덱스의 단조 감소 덱.
fun windowMax(nums: IntArray, k: Int): IntArray {
    val out = IntArray(nums.size - k + 1)
    val dq = ArrayDeque<Int>()
    for (i in nums.indices) {
        while (dq.isNotEmpty() && nums[dq.last()] <= nums[i]) {
            Drill.pop(nums[dq.removeLast()])
        }
        dq.addLast(i)
        Drill.enqueue(nums[i])
        if (dq.first() <= i - k) {
            Drill.dequeue(nums[dq.removeFirst()])
        }
        if (i >= k - 1) {
            out[i - k + 1] = nums[dq.first()]
            Drill.write(i - k + 1, out[i - k + 1])
        }
    }
    return out
}
