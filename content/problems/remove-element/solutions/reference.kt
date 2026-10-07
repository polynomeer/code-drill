// 검증용 정답 (§6.1 solutions/). 읽는 자리와 쓰는 자리를 따로 둔다.
fun removeElement(nums: IntArray, value: Int): IntArray {
    var write = 0
    for (read in nums.indices) {
        if (nums[read] != value) {
            nums[write] = nums[read]
            Drill.write(write, nums[read])
            write += 1
        }
    }
    return nums.copyOf(write)
}
