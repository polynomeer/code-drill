// kind: OFF_BY_ONE
// 비트 29 를 보지 않는다. 최상위 비트가 갈리는 쌍의 답이 절반이 된다.
fun maxXorPair(nums: IntArray): Int {
    var best = 0
    var mask = 0
    val prefixes = HashSet<Int>()
    for (bit in 28 downTo 0) {
        mask = mask or (1 shl bit)
        prefixes.clear()
        for (x in nums) prefixes.add(x and mask)
        val candidate = best or (1 shl bit)
        for (p in prefixes) if ((candidate xor p) in prefixes) { best = candidate; break }
    }
    return best
}
