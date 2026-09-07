// kind: WRONG_BRANCH
// 같은 값도 증가로 본다.
fun longestIncreasing(nums: IntArray): Int {
    val tails = IntArray(nums.size)
    var length = 0
    for (value in nums) {
        var low = 0
        var high = length
        while (low < high) {
            val mid = low + (high - low) / 2
            if (tails[mid] <= value) low = mid + 1 else high = mid
        }
        tails[low] = value
        if (low == length) length += 1
    }
    return length
}
