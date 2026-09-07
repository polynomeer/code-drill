// kind: PERFORMANCE
// 창마다 k 개를 다시 더해 O(n*k) 다. 작은 입력은 통과한다.
fun maxWindowSum(nums: IntArray, k: Int): Int {
    var best = Int.MIN_VALUE
    for (start in 0..nums.size - k) {
        var total = 0
        for (i in start until start + k) total += nums[i]
        if (total > best) best = total
    }
    return best
}
