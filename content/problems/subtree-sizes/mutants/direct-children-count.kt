// kind: WRONG_ALGORITHM
// 직접 자식의 수에 1 을 더한 값을 답한다. 손자 이하를 세지 않는다.
fun subtreeSizes(parent: IntArray): IntArray {
    val size = IntArray(parent.size) { 1 }
    for (v in parent.indices) if (parent[v] != -1) size[parent[v]] += 1
    return size
}
