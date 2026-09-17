// 검증용 정답 (§6.1 solutions/). 32 비트 자리마다 개수 % 3.
fun singleNumberThrice(nums: IntArray): Int {
    var answer = 0
    for (bit in 0 until 32) {
        var count = 0
        for (x in nums) if ((x ushr bit) and 1 == 1) count += 1
        Drill.compare(bit, count)
        if (count % 3 != 0) answer = answer or (1 shl bit)
    }
    Drill.write(0, answer)
    return answer
}
