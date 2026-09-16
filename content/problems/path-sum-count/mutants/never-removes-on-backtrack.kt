// kind: WRONG_BRANCH
// 되돌아올 때 누적합을 해시맵에서 빼지 않는다. 다른 가지의 정점을 조상으로 센다.
fun pathSumCount(parent: IntArray, values: IntArray, target: Int): Int {
    val n = parent.size
    val prefix = LongArray(n)
    val seen = HashMap<Long, Int>()
    seen[0L] = 1
    var count = 0
    for (i in 0 until n) {
        prefix[i] = (if (parent[i] == -1) 0L else prefix[parent[i]]) + values[i]
        count += seen[prefix[i] - target] ?: 0
        seen[prefix[i]] = (seen[prefix[i]] ?: 0) + 1
    }
    return count
}
