// kind: WRONG_BRANCH
// 밀려난 원소를 버리지 않고 늘어난 배열을 돌려준다. 길이는 그대로여야 한다.
fun duplicateZeros(arr: IntArray): IntArray {
    val out = ArrayList<Int>()
    for (x in arr) { out.add(x); if (x == 0) out.add(0) }
    return out.toIntArray()
}
