// 검증용 정답 (§6.1 solutions/). 높은 비트부터 접두사 집합으로 결정한다.
fun maxXorPair(nums: IntArray): Int {
    var best = 0
    var mask = 0
    val prefixes = HashSet<Int>()
    for (bit in 29 downTo 0) {
        mask = mask or (1 shl bit)
        prefixes.clear()
        for (x in nums) prefixes.add(x and mask)
        val candidate = best or (1 shl bit)
        for (p in prefixes) {
            if ((candidate xor p) in prefixes) { best = candidate; Drill.write(0, best); break }
        }
        Drill.compare(bit, best)
    }
    return best
}
