// kind: MISSING_EDGE_CASE
// 지운 자리로 당겨 온 값을 건너뛴다. 같은 값이 연달아 있으면 하나가 남는다.
fun removeElement(nums: IntArray, value: Int): IntArray {
    val list = nums.toMutableList()
    var i = 0
    while (i < list.size) {
        if (list[i] == value) list.removeAt(i)
        i += 1
    }
    return list.toIntArray()
}
