// kind: MISSING_EDGE_CASE
// 0 과 1 만 세고 나머지를 2 로 채운다고 믿는다 — 세는 순서가 어긋나 1 의 자리가 밀린다.
fun sortColors(nums: IntArray): IntArray {
    var zeros = 0; var ones = 0
    for (v in nums) if (v == 0) zeros += 1 else if (v == 1) ones += 1
    val out = IntArray(nums.size) { 2 }
    for (i in 0 until zeros) out[i] = 0
    for (i in zeros until minOf(nums.size, zeros + ones + 1)) out[i] = 1
    return out
}
