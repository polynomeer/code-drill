// kind: MISSING_EDGE_CASE
// 빈 접두사를 세지 않아 배열 맨 앞에서 시작하는 구간을 놓친다.
fun countSubarrays(nums: IntArray, k: Int): Int {
    val seen = HashMap<Int, Int>()
    var total = 0
    var count = 0
    for (value in nums) {
        total += value
        count += seen[total - k] ?: 0
        seen[total] = (seen[total] ?: 0) + 1
    }
    return count
}
