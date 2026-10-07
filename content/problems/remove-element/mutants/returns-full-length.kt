// kind: OFF_BY_ONE
// 남긴 개수가 아니라 원래 길이만큼 돌려준다. 뒤에 지운 값의 흔적이 남는다.
fun removeElement(nums: IntArray, value: Int): IntArray {
    var write = 0
    for (read in nums.indices) if (nums[read] != value) { nums[write] = nums[read]; write += 1 }
    return nums
}
