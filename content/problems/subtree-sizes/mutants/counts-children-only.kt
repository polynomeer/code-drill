// kind: OFF_BY_ONE
// 자기 자신을 세지 않는다. 잎이 0 이 된다.
fun subtreeSizes(parent: IntArray): IntArray {
    val n = parent.size
    val children = Array(n) { mutableListOf<Int>() }
    var root = 0
    for (v in 0 until n) if (parent[v] == -1) root = v else children[parent[v]].add(v)
    val size = IntArray(n)
    fun walk(v: Int): Int { var s = 0; for (c in children[v]) s += walk(c) + 1; size[v] = s; return s }
    walk(root)
    return size
}
