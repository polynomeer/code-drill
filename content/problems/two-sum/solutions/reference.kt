// 검증용 정답 (§6.1 solutions/). 콘텐츠 파이프라인이 전체 테스트에 이 코드를 돌린다.
//
// Drill.* 호출은 학습용 트레이스 계측이다 (§0.3). 채점 실행에서는 no-op 으로 컴파일되어
// 판정 시간에 영향을 주지 않는다.
fun twoSum(nums: IntArray, target: Int): IntArray {
    val seen = HashMap<Int, Int>(nums.size * 2)
    for (i in nums.indices) {
        Drill.visit(i, nums[i])
        val complement = target - nums[i]
        val j = seen[complement]
        if (j != null) {
            Drill.match(j, i)
            return intArrayOf(j, i)
        }
        Drill.compare(i, complement)
        seen.putIfAbsent(nums[i], i)
    }
    error("문제 정의상 정답은 항상 존재한다")
}
