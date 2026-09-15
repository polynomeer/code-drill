// kind: WRONG_ALGORITHM
// 정렬해 첫 빈 자리를 찾는다. 끝까지 빈 자리가 없으면 0 을 답한다.
fun missingNumber(nums: IntArray): Int {
    val sorted = nums.sorted()
    for ((i, v) in sorted.withIndex()) if (v != i) return i
    return 0
}
