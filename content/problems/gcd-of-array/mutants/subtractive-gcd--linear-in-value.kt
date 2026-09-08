// kind: PERFORMANCE
// 나눗셈 대신 뺄셈으로 유클리드를 돈다. 값이 크면 값에 비례해 느려진다.
fun gcdOfArray(nums: IntArray): Int {
    var result = 0
    for (value in nums) {
        var a = if (value < 0) -value else value
        if (a == 0) continue
        var b = result
        if (b == 0) {
            result = a
            continue
        }
        while (a != b) {
            if (a > b) a -= b else b -= a
        }
        result = a
    }
    return result
}
