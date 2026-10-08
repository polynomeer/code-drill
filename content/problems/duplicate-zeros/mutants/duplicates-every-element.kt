// kind: WRONG_BRANCH
// 0 이 아닌 원소도 두 번 쓴다. 겹쳐 쓰는 것은 0 뿐이다.
fun duplicateZeros(arr: IntArray): IntArray {
    val out = ArrayList<Int>()
    for (x in arr) { out.add(x); out.add(x) }
    return out.subList(0, arr.size).toIntArray()
}
