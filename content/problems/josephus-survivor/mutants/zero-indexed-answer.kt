// kind: OFF_BY_ONE
// 0 부터 센 자리를 그대로 답한다. 사람의 번호는 1 부터다.
fun survivor(n: Int, k: Int): Int {
    var result = 0
    for (size in 2..n) result = (result + k) % size
    return result
}
