// kind: WRONG_ALGORITHM
// 순서를 구분해 센다. 2+3 과 3+2 가 둘이 된다.
fun combinationSumCount(candidates: IntArray, target: Int): Int {
    val ways = IntArray(target + 1)
    ways[0] = 1
    for (t in 1..target) for (c in candidates) if (c <= t) ways[t] += ways[t - c]
    return ways[target]
}
