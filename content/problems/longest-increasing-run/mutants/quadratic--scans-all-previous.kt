// kind: PERFORMANCE
// 각 위치에서 앞을 전부 훑는 O(n^2) DP 다. 오름차순 입력이 최악이다.
fun longestIncreasing(nums: IntArray): Int {
    val best = IntArray(nums.size) { 1 }
    var answer = 1
    for (i in nums.indices) {
        for (j in 0 until i) {
            if (nums[j] < nums[i] && best[j] + 1 > best[i]) best[i] = best[j] + 1
        }
        if (best[i] > answer) answer = best[i]
    }
    return answer
}
