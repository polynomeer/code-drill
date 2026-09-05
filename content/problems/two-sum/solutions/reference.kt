// 검증용 정답 (§6.1 solutions/). 콘텐츠 파이프라인이 전체 테스트에 이 코드를 돌린다.
fun twoSum(nums: IntArray, target: Int): IntArray {
    val seen = HashMap<Int, Int>(nums.size * 2)
    for (i in nums.indices) {
        val complement = target - nums[i]
        val j = seen[complement]
        if (j != null) return intArrayOf(j, i)
        seen.putIfAbsent(nums[i], i)
    }
    error("문제 정의상 정답은 항상 존재한다")
}
