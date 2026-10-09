// kind: MISSING_EDGE_CASE
// 시작 자리를 찾은 뒤 그 칸의 값을 바로 읽는다. 값이 모든 원소보다 크면 시작 자리가 배열 끝이라 범위를 벗어난다.
fun countInSorted(nums: IntArray, targets: IntArray): IntArray {
    fun firstAtLeast(key: Int, strict: Boolean): Int {
        var low = 0; var high = nums.size
        while (low < high) { val mid = (low + high) ushr 1; if (nums[mid] < key || (strict && nums[mid] == key)) low = mid + 1 else high = mid }
        return low
    }
    return IntArray(targets.size) {
        val start = firstAtLeast(targets[it], false)
        if (nums[start] != targets[it]) 0 else firstAtLeast(targets[it], true) - start
    }
}
