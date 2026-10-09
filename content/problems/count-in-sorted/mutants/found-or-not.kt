// kind: WRONG_ALGORITHM
// 값이 있는지만 보고 1 이나 0 을 낸다. 같은 값이 여럿일 수 있다.
fun countInSorted(nums: IntArray, targets: IntArray): IntArray =
    IntArray(targets.size) { if (java.util.Arrays.binarySearch(nums, targets[it]) >= 0) 1 else 0 }
