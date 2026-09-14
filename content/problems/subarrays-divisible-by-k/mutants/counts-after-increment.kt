// kind: WRONG_BRANCH
// 개수를 올린 뒤에 더한다. 자기 자신과의 쌍을 센다.
fun divisibleSubarrays(nums: IntArray, k: Int): Int {
    val counts = IntArray(k)
    counts[0] = 1
    var prefix = 0
    var total = 0
    for (v in nums) {
        prefix = ((prefix + v) % k + k) % k
        counts[prefix] += 1
        total += counts[prefix]
    }
    return total
}
