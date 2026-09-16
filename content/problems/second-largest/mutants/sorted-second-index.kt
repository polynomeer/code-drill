// kind: WRONG_ALGORITHM
// 정렬해 끝에서 둘째를 답한다. 같은 값을 거르지 않는다.
fun secondLargest(nums: IntArray): Int {
    if (nums.size < 2) return -1
    val sorted = nums.sorted()
    return sorted[sorted.size - 2]
}
