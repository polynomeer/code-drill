// kind: WRONG_ALGORITHM
// 자기 서브트리 안의 거리만 더한다. 위로 올라가는 쪽을 빼먹는다.
fun distanceSums(parent: IntArray): IntArray {
    val n = parent.size
    val size = IntArray(n) { 1 }
    val inside = LongArray(n)
    for (i in n - 1 downTo 1) { val p = parent[i]; size[p] += size[i]; inside[p] += inside[i] + size[i] }
    return IntArray(n) { inside[it].toInt() }
}
