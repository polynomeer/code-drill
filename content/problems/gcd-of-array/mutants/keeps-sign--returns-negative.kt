// kind: WRONG_BRANCH
// 절댓값을 취하지 않아 음수만 있으면 음수를 돌려준다.
fun gcdOfArray(nums: IntArray): Int {
    fun gcd(first: Int, second: Int): Int {
        var a = first; var b = second
        while (b != 0) { val next = a % b; a = b; b = next }
        return a
    }
    var current = 0
    for (value in nums) current = gcd(current, value)
    return current
}
