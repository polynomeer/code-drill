// 검증용 정답 (§6.1 solutions/). 읽는 자리와 쓰는 자리가 따로 간다.
fun removeDuplicates(nums: IntArray): IntArray {
    if (nums.isEmpty()) return nums
    var write = 1
    for (read in 1 until nums.size) {
        Drill.pointer("read", read)
        if (nums[read] != nums[write - 1]) {
            nums[write] = nums[read]
            Drill.write(write, nums[read])
            write += 1
            Drill.pointer("write", write)
        }
    }
    return nums.copyOf(write)
}
