// kind: MISSING_EDGE_CASE
// 0 을 만나면 결과를 0 으로 만든다. gcd(0, x) = x 를 쓰지 않았다.
fun gcdOfArray(nums: IntArray): Int {
    fun gcd(first: Int, second: Int): Int {
        var a = first; var b = second
        while (b != 0) { val next = a % b; a = b; b = next }
        return a
    }
    var current = if (nums[0] < 0) -nums[0] else nums[0]
    for (index in 1 until nums.size) {
        val value = if (nums[index] < 0) -nums[index] else nums[index]
        if (value == 0) return 0
        current = gcd(current, value)
    }
    return current
}
