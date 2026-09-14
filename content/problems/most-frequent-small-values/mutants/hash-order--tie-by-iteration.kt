// kind: MISSING_EDGE_CASE
// 해시맵을 만난 순서로 훑어 동률을 정한다. 순서가 값 순이라는 보장이 없다.
fun mostFrequent(nums: IntArray): Int {
    val counts = HashMap<Int, Int>()
    for (v in nums) counts[v] = (counts[v] ?: 0) + 1
    var best = -1
    var bestCount = -1
    for ((v, c) in counts) if (c > bestCount) { best = v; bestCount = c }
    return best
}
