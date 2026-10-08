// kind: WRONG_ALGORITHM
// 앞에서부터 제자리에서 0 을 하나 더 쓴다. 아직 읽지 않은 원소를 덮어써 뒤가 모두 0 이 된다.
fun duplicateZeros(arr: IntArray): IntArray {
    var i = 0
    while (i < arr.size) {
        if (arr[i] == 0 && i + 1 < arr.size) { arr[i + 1] = 0; i += 2 } else i += 1
    }
    return arr
}
