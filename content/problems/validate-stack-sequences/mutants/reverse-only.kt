// kind: WRONG_ALGORITHM
// 꺼낸 순서가 넣은 순서의 정반대일 때만 가능하다고 본다. 넣는 도중에도 꺼낼 수 있다.
fun validateStackSequences(pushed: IntArray, popped: IntArray): Int =
    if (pushed.reversedArray().contentEquals(popped)) 1 else 0
