// kind: OFF_BY_ONE
// 다음 값과 다를 때만 남긴다. 마지막 원소가 빠진다.
fun removeDuplicates(nums: IntArray): IntArray {
    val out = ArrayList<Int>()
    for (i in 0 until nums.size - 1) if (nums[i] != nums[i + 1]) out.add(nums[i])
    return out.toIntArray()
}
