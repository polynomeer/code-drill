// kind: WRONG_ALGORITHM
// 이웃한 잎 쌍의 곱을 모두 더한다. 묶인 덩어리는 큰 잎 하나로 남아 그다음 곱에 쓰인다.
fun minCostTreeFromLeaves(arr: IntArray): Int {
    var total = 0L
    for (i in 0 until arr.size - 1) total += arr[i].toLong() * arr[i + 1]
    return total.toInt()
}
