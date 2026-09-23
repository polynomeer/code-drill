// kind: PERFORMANCE
// 가운데 값을 하나씩 정하고 오른쪽을 전부 훑는다. O(n^2).
fun hasPattern132(nums: IntArray): Int {
    var smallest = Int.MAX_VALUE
    for (j in nums.indices) {
        if (j > 0 && nums[j - 1] < smallest) smallest = nums[j - 1]
        for (k in j + 1 until nums.size) {
            Drill.compare(j, k)
            if (smallest < nums[k] && nums[k] < nums[j]) return 1
        }
    }
    return 0
}
