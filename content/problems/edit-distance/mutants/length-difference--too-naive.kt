// kind: WRONG_BRANCH
// 길이 차이만 답으로 삼는다.
fun editDistance(source: IntArray, target: IntArray): Int {
    val diff = source.size - target.size
    return if (diff < 0) -diff else diff
}
