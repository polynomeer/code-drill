// kind: WRONG_BRANCH
// 접두사 집합을 첫 비트에서 한 번만 만든다. 아래 비트의 접두사가 집합에 없어 답이 멈춘다.
fun maxXorPair(nums: IntArray): Int {
    var best = 0
    var mask = 0
    val prefixes = HashSet<Int>()
    for (bit in 29 downTo 0) {
        mask = mask or (1 shl bit)
        if (bit == 29) for (x in nums) prefixes.add(x and mask)
        val candidate = best or (1 shl bit)
        for (p in prefixes) if ((candidate xor p) in prefixes) { best = candidate; break }
    }
    return best
}
