// kind: MISSING_EDGE_CASE
// 0 을 빼고 남은 것만 돌려줘 길이가 줄어든다.
fun moveZeros(nums: IntArray): IntArray = nums.filter { it != 0 }.toIntArray()
