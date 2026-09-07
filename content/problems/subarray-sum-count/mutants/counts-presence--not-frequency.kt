// kind: WRONG_BRANCH
// 같은 합이 몇 번 나왔는지 세지 않고 있었는지만 본다.
fun countSubarrays(nums: IntArray, k: Int): Int {
    val seen = HashSet<Int>()
    seen.add(0)
    var total = 0
    var count = 0
    for (value in nums) {
        total += value
        if (seen.contains(total - k)) count += 1
        seen.add(total)
    }
    return count
}
