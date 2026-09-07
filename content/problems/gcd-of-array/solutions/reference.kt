// 검증용 정답 (§6.1 solutions/). 유클리드 호제법을 접어 간다.
//
// gcd(0, x) = x 이므로 0 에서 시작하면 특별한 경우가 없다. 모두 0 이면 결과도 0 이고,
// 그것이 이 문제가 정의한 답이다.
fun gcdOfArray(nums: IntArray): Int {
    fun gcd(first: Int, second: Int): Int {
        var a = first
        var b = second
        while (b != 0) {
            val next = a % b
            a = b
            b = next
        }
        return a
    }

    var current = 0
    for (index in nums.indices) {
        Drill.visit(index, nums[index])
        val value = if (nums[index] < 0) -nums[index] else nums[index]
        current = gcd(current, value)
        Drill.write(0, current)
        // 1 보다 작아질 수 없다. 더 볼 필요가 없다.
        if (current == 1) break
    }
    return current
}
