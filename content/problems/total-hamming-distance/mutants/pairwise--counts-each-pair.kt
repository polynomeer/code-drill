// kind: PERFORMANCE
// 쌍마다 xor 의 비트를 하나씩 센다. O(n² · 비트 수).
fun totalHamming(nums: IntArray): Int {
    var total = 0
    for (i in nums.indices) {
        for (j in i + 1 until nums.size) {
            var x = nums[i] xor nums[j]
            Drill.compare(i, j)
            while (x != 0) { total += x and 1; x = x shr 1 }
        }
    }
    return total
}
