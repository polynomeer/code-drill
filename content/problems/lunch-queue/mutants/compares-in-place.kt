// kind: WRONG_ALGORITHM
// 같은 자리의 학생과 샌드위치가 다르면 못 먹은 것으로 센다. 학생은 줄 뒤로 가서 다시 기다린다.
fun lunchQueue(students: IntArray, sandwiches: IntArray): Int = students.indices.count { students[it] != sandwiches[it] }
