// kind: OFF_BY_ONE
// 자릿수를 경계와 견줄 때 등호를 잘못 둔다. 10, 100 같은 10 의 거듭제곱이 한 자리 적게 세어진다.
fun evenDigitCount(nums: IntArray): Int {
    var count = 0
    for (v in nums) {
        val x = Math.abs(v.toLong())
        var digits = 1
        var bound = 10L
        while (x > bound) { digits += 1; bound *= 10 }
        if (digits % 2 == 0) count += 1
    }
    return count
}
