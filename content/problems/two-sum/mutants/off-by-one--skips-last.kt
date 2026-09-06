// kind: OFF_BY_ONE
// 마지막 원소를 훑지 않는다. 답이 배열 끝에 있는 케이스만 잡아낸다.
fun twoSum(nums: IntArray, target: Int): IntArray {
    val seen = HashMap<Int, Int>()
    for (i in 0 until nums.size - 1) {
        val j = seen[target - nums[i]]
        if (j != null) return intArrayOf(j, i)
        seen.putIfAbsent(nums[i], i)
    }
    return intArrayOf(0, 1)
}
