// kind: PERFORMANCE
// 선택 정렬로 k 번 훑어 O(n*k) 다. 작은 입력은 통과한다.
fun kthLargest(nums: IntArray, k: Int): Int {
    val values = nums.copyOf()
    for (round in 0 until k) {
        var best = round
        for (i in round + 1 until values.size) {
            if (values[i] > values[best]) best = i
        }
        val swap = values[round]
        values[round] = values[best]
        values[best] = swap
    }
    return values[k - 1]
}
