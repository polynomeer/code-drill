// kind: PERFORMANCE
// 1 부터 후보를 올리며 후보마다 배열 전체를 훑는다. O(n^2).
fun firstMissingPositive(nums: IntArray): Int {
    var candidate = 1
    while (true) {
        var found = false
        for (v in nums) { Drill.compare(candidate, v); if (v == candidate) { found = true; break } }
        if (!found) return candidate
        candidate += 1
    }
}
