// kind: WRONG_BRANCH
// 작은 쪽에서 k 번째를 센다.
fun kthLargest(nums: IntArray, k: Int): Int = nums.sorted()[k - 1]
