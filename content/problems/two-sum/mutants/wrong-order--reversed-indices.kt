// kind: WRONG_BRANCH
// 인덱스를 내림차순으로 반환한다. "오름차순" 조건을 검사하지 않으면 통과해버린다.
fun twoSum(nums: IntArray, target: Int): IntArray {
    val seen = HashMap<Int, Int>()
    for (i in nums.indices) {
        val j = seen[target - nums[i]]
        if (j != null) return intArrayOf(i, j)
        seen.putIfAbsent(nums[i], i)
    }
    error("정답은 항상 존재한다")
}
