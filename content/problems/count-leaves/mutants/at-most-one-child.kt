// kind: WRONG_BRANCH
// 자식이 하나 이하인 정점을 센다. 잎은 자식이 없는 정점이다.
fun countLeaves(parent: IntArray): Int {
    val children = IntArray(parent.size)
    for (p in parent) if (p != -1) children[p] += 1
    return children.count { it <= 1 }
}
