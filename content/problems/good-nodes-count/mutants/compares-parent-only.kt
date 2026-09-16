// kind: WRONG_ALGORITHM
// 부모 하나와만 비교한다. 조상 중에 더 큰 것이 있어도 좋다고 센다.
fun goodNodes(parent: IntArray, values: IntArray): Int {
    var count = 0
    for (i in parent.indices) if (parent[i] == -1 || values[i] >= values[parent[i]]) count += 1
    return count
}
