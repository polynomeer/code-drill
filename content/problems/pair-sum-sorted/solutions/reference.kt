// 검증용 정답 (§6.1 solutions/). 투 포인터.
//
// 합이 작으면 왼쪽을 오른쪽으로, 크면 오른쪽을 왼쪽으로 민다. 정렬되어 있으므로
// 이 한 번의 이동이 "그 포인터가 만들 수 있는 다른 모든 짝"을 함께 버린다.
fun pairSum(nums: IntArray, target: Int): IntArray {
    var left = 0
    var right = nums.size - 1

    while (left < right) {
        Drill.pointer("left", left)
        Drill.pointer("right", right)
        Drill.compare(left, right)

        val total = nums[left] + nums[right]
        when {
            total == target -> {
                Drill.match(left, right)
                return intArrayOf(left, right)
            }
            total < target -> left += 1
            else -> right -= 1
        }
    }
    error("정답은 항상 존재한다")
}
