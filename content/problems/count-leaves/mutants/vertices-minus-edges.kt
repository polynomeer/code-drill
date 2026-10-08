// kind: WRONG_ALGORITHM
// 정점 수에서 간선 수를 뺀다. 트리에서 그 값은 늘 1 이다 — 잎의 수가 아니다.
fun countLeaves(parent: IntArray): Int = parent.size - parent.count { it != -1 }
