// kind: WRONG_BRANCH
// 종류별 학생 수와 샌드위치 수의 차를 답으로 낸다. 위에서 막힌 샌드위치 아래의 것은 아무도 못 먹는다.
fun lunchQueue(students: IntArray, sandwiches: IntArray): Int {
    val zeros = students.count { it == 0 } - sandwiches.count { it == 0 }
    return Math.abs(zeros)
}
