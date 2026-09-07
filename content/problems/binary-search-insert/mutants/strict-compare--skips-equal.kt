// kind: WRONG_BRANCH
// 같은 값을 왼쪽으로 밀어 한 칸 뒤를 돌려준다.
fun insertPosition(nums: IntArray, target: Int): Int {
    var low = 0
    var high = nums.size
    while (low < high) {
        val mid = low + (high - low) / 2
        if (nums[mid] <= target) low = mid + 1 else high = mid
    }
    return low
}
