// kind: MISSING_EDGE_CASE
// 비트 31 을 세지 않는다. 답이 음수면 부호가 사라진다.
fun singleNumberThrice(nums: IntArray): Int {
    var answer = 0
    for (bit in 0 until 31) {
        var count = 0
        for (x in nums) if ((x ushr bit) and 1 == 1) count += 1
        if (count % 3 != 0) answer = answer or (1 shl bit)
    }
    return answer
}
