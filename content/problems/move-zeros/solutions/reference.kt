// 검증용 정답 (§6.1 solutions/). 쓰기 위치를 따로 든 한 번 훑기.
//
// 읽는 위치와 쓰는 위치를 나누면 추가 배열이 필요 없다. 쓰기 위치는 읽기 위치를
// 앞지르지 않으므로 아직 읽지 않은 값을 덮어쓸 일도 없다.
fun moveZeros(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    var write = 0

    for (index in nums.indices) {
        Drill.visit(index, nums[index])
        if (nums[index] != 0) {
            out[write] = nums[index]
            Drill.write(write, nums[index])
            write += 1
            Drill.pointer("write", write)
        }
    }
    return out
}
