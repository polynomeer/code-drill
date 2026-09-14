// kind: OFF_BY_ONE
// 빈 접두사(나머지 0 하나)를 세어 두지 않는다. 처음부터 시작하는 부분배열을 놓친다.
fun divisibleSubarrays(nums: IntArray, k: Int): Int {
    val counts = IntArray(k)
    var prefix = 0
    var total = 0
    for (v in nums) {
        prefix = ((prefix + v) % k + k) % k
        total += counts[prefix]
        counts[prefix] += 1
    }
    return total
}
