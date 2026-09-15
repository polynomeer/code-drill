// kind: OFF_BY_ONE
// 1 번부터 앞 값과 비교해 남긴다. 첫 값은 비교 상대가 없어 빠진다.
fun removeDuplicates(nums: IntArray): IntArray {
    val out = ArrayList<Int>()
    for (i in 1 until nums.size) if (nums[i] != nums[i - 1]) out.add(nums[i])
    return out.toIntArray()
}
