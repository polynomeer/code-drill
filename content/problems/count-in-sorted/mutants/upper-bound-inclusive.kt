// kind: OFF_BY_ONE
// 개수를 마지막 자리에서 시작 자리를 뺀 값으로 센다. 양 끝을 다 넣으려면 1 을 더해야 한다.
fun countInSorted(nums: IntArray, targets: IntArray): IntArray {
    fun firstAtLeast(key: Int, strict: Boolean): Int {
        var low = 0; var high = nums.size
        while (low < high) { val mid = (low + high) ushr 1; if (nums[mid] < key || (strict && nums[mid] == key)) low = mid + 1 else high = mid }
        return low
    }
    return IntArray(targets.size) {
        val start = firstAtLeast(targets[it], false)
        if (start == nums.size || nums[start] != targets[it]) 0 else firstAtLeast(targets[it], true) - 1 - start
    }
}
