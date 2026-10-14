// kind: WRONG_ALGORITHM
// 과목 수를 k 로 나눠 올린다. 선수 관계가 학기를 늘릴 수 있다.
fun coursesWithLimit(n: Int, edges: IntArray, k: Int): Int = (n + k - 1) / k
