// 검증용 정답 (§6.1 solutions/). 값이 처음 나오는 자리와 그보다 큰 값이 처음 나오는 자리의 차.
fun countInSorted(nums: IntArray, targets: IntArray): IntArray {
    // nums 에서 key 보다 작지 않은(strict 면 큰) 첫 자리
    fun firstAtLeast(key: Int, strict: Boolean): Int {
        var low = 0
        var high = nums.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (nums[mid] < key || (strict && nums[mid] == key)) low = mid + 1 else high = mid
            Drill.pointer("low", low); Drill.pointer("high", high)
        }
        return low
    }
    return IntArray(targets.size) { firstAtLeast(targets[it], true) - firstAtLeast(targets[it], false) }
}
