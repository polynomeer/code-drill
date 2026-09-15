// kind: MISSING_EDGE_CASE
// 같은 값을 하나로 합친다. 각각 남겨야 한다.
fun mergeSorted(sizes: IntArray, values: IntArray): IntArray = values.toSortedSet().toIntArray()
