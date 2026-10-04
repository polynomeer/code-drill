// kind: WRONG_ALGORITHM
// 오른쪽 끝마다 지금까지 가장 작은 값의 자리와만 짝짓는다. 더 앞에 있는 조금 큰 값이 더 멀리 짝지을 수 있다.
fun maxWidthRamp(nums: IntArray): Int {
    var minAt = 0
    var best = 0
    for (j in 1 until nums.size) {
        if (nums[j] < nums[minAt]) minAt = j else best = maxOf(best, j - minAt)
    }
    return best
}
