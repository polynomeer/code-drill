// kind: WRONG_ALGORITHM
// 가장 작은 풍선부터 터뜨린다. 국소 선택이 전체 최적이 아니다.
fun burstBalloons(nums: IntArray): Int {
    val list = ArrayList<Int>().apply { add(1); nums.forEach { add(it) }; add(1) }
    var total = 0
    while (list.size > 2) {
        var at = 1
        for (i in 1 until list.size - 1) if (list[i] < list[at]) at = i
        total += list[at - 1] * list[at] * list[at + 1]
        list.removeAt(at)
    }
    return total
}
