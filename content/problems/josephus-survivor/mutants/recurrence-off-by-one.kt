// kind: WRONG_BRANCH
// 점화식에 k 가 아니라 k-1 을 더한다. 내보낸 사람 다음부터 다시 센다는 것을 잊었다.
fun survivor(n: Int, k: Int): Int {
    var result = 0
    for (size in 2..n) result = (result + k - 1) % size
    return result + 1
}
