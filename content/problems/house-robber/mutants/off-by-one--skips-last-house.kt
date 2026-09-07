// kind: OFF_BY_ONE
// 마지막 집을 고려하지 않는다.
fun rob(values: IntArray): Int {
    var skip = 0
    var take = 0
    for (index in 0 until values.size - 1) {
        val nextSkip = maxOf(skip, take)
        val nextTake = skip + values[index]
        skip = nextSkip
        take = nextTake
    }
    return maxOf(skip, take)
}
