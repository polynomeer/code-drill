"""정렬·탐색 (역량: 순서를 이용해 후보를 반으로 줄이기)."""

from author import Problem, standard_groups, perf_groups, randoms, shuffled

PROBLEMS = []


# --- 10. 삽입 위치 찾기 ------------------------------------------------------

def _insert_position(nums, target):
    low, high = 0, len(nums)
    while low < high:
        mid = (low + high) // 2
        if nums[mid] < target:
            low = mid + 1
        else:
            high = mid
    return low


PROBLEMS.append(Problem(
    id="binary-search-insert",
    title="정렬 배열에서 삽입 위치",
    summary="""
오름차순으로 정렬된 배열 `nums` 와 정수 `target` 이 주어진다. `target` 이 들어갈
자리의 인덱스를 반환한다. 이미 있으면 **가장 왼쪽** 자리다.
""",
    notes="""
같은 값이 여러 개일 때 어느 자리를 답으로 볼지가 이 문제의 전부다. "찾으면 바로
반환"하는 이분 탐색은 가운데 어딘가를 돌려주므로 답이 흔들린다.
""",
    drill_doc="""
Drill.pointer("low", low)    // 후보 구간의 왼쪽
Drill.pointer("high", high)  // 후보 구간의 오른쪽
Drill.visit(mid, value)      // 가운데를 봤다
""",
    constraints="""
- `1 <= nums.size <= 200_000`
- `-10^9 <= nums[i], target <= 10^9`, `nums` 는 오름차순
""",
    signature=dict(name="insertPosition", parameters=[("nums", "INT_ARRAY"), ("target", "INT")],
                   returns="INT"),
    groups=standard_groups(),
    reference=_insert_position,
    cases={
        "sample": [
            ("01", [[1, 3, 5, 6], 5]),
            ("02", [[1, 3, 5, 6], 2]),
        ],
        "boundary": [
            # 모든 원소보다 작다. 0 이 답이다.
            ("01-before-all", [[10, 20, 30], 5]),
            # 모든 원소보다 크다. 배열 길이가 답이다 — 범위를 벗어나기 쉬운 자리다.
            ("02-after-all", [[10, 20, 30], 40]),
            ("03-single-smaller", [[7], 3]),
            ("04-single-larger", [[7], 9]),
            ("05-single-equal", [[7], 7]),
            # 같은 값이 이어진다. 가장 왼쪽이 아니면 틀린다.
            ("06-duplicates", [[1, 2, 2, 2, 2, 3], 2]),
        ],
        "hidden": [
            ("01-all-same-hit", [[4] * 20, 4]),
            ("02-all-same-miss", [[4] * 20, 5]),
            ("03-long", [list(range(0, 2000, 2)), 1001]),
            ("04-exact-tail", [list(range(0, 100)), 99]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 왼쪽 경계를 찾는 이분 탐색.
//
// 찾자마자 반환하지 않는다. 같은 값이 여러 개일 때 "가장 왼쪽"을 답으로 하려면
// 구간을 계속 좁혀 low 와 high 가 만나는 자리를 봐야 한다.
//
// high 를 nums.size 에서 시작하는 것도 의도다. 모든 원소보다 큰 값의 답이 곧
// nums.size 이므로, size - 1 에서 시작하면 그 답을 만들 수 없다.
fun insertPosition(nums: IntArray, target: Int): Int {
    var low = 0
    var high = nums.size

    while (low < high) {
        val mid = low + (high - low) / 2
        Drill.pointer("low", low)
        Drill.pointer("high", high)
        Drill.visit(mid, nums[mid])

        if (nums[mid] < target) low = mid + 1 else high = mid
    }
    return low
}
""",
    mutants=[
        ("returns-any-match--not-leftmost", "WRONG_BRANCH",
         "찾자마자 반환해 같은 값이 여러 개일 때 가운데를 돌려준다.",
         """
fun insertPosition(nums: IntArray, target: Int): Int {
    var low = 0
    var high = nums.size - 1
    while (low <= high) {
        val mid = low + (high - low) / 2
        if (nums[mid] == target) return mid
        if (nums[mid] < target) low = mid + 1 else high = mid - 1
    }
    return low
}
"""),
        ("high-off-by-one--misses-tail", "OFF_BY_ONE",
         "high 를 size - 1 에서 시작해 모든 원소보다 큰 값의 자리를 만들지 못한다.",
         """
fun insertPosition(nums: IntArray, target: Int): Int {
    var low = 0
    var high = nums.size - 1
    while (low < high) {
        val mid = low + (high - low) / 2
        if (nums[mid] < target) low = mid + 1 else high = mid
    }
    return low
}
"""),
        ("strict-compare--skips-equal", "WRONG_BRANCH",
         "같은 값을 왼쪽으로 밀어 한 칸 뒤를 돌려준다.",
         """
fun insertPosition(nums: IntArray, target: Int): Int {
    var low = 0
    var high = nums.size
    while (low < high) {
        val mid = low + (high - low) / 2
        if (nums[mid] <= target) low = mid + 1 else high = mid
    }
    return low
}
"""),
    ],
))


# --- 11. k 번째 큰 수 --------------------------------------------------------

def _kth_largest(nums, k):
    return sorted(nums, reverse=True)[k - 1]


PROBLEMS.append(Problem(
    id="kth-largest",
    title="k 번째로 큰 수",
    summary="""
정수 배열 `nums` 와 정수 `k` 가 주어진다. **정렬했을 때** `k` 번째로 큰 값을 반환한다.

중복은 따로 세지 않는다. `[3, 3, 1]` 에서 2번째로 큰 값은 `3` 이다.
""",
    drill_doc="""
Drill.visit(index, value)  // 원소를 봤다
Drill.compare(left, right) // 크기를 견줬다
Drill.write(0, value)      // 현재 k 번째 후보
""",
    constraints="""
- `1 <= k <= nums.size <= 200_000`
- `-10^9 <= nums[i] <= 10^9`
""",
    signature=dict(name="kthLargest", parameters=[("nums", "INT_ARRAY"), ("k", "INT")],
                   returns="INT"),
    groups=perf_groups(),
    reference=_kth_largest,
    cases={
        "sample": [
            ("01", [[3, 2, 1, 5, 6, 4], 2]),
            ("02", [[3, 2, 3, 1, 2, 4, 5, 5, 6], 4]),
        ],
        "boundary": [
            ("01-k-one", [[7, 2, 9], 1]),
            ("02-k-equals-n", [[7, 2, 9], 3]),
            ("03-single", [[42], 1]),
            # 중복을 따로 세면 여기서 틀린다.
            ("04-duplicates", [[3, 3, 1], 2]),
            ("05-all-same", [[5, 5, 5, 5], 3]),
            ("06-negative", [[-1, -5, -3, -2], 2]),
        ],
        "hidden": [
            ("01-sorted-ascending", [list(range(1, 51)), 7]),
            ("02-sorted-descending", [list(range(50, 0, -1)), 7]),
            ("03-random", [randoms(300, -1000, 1000, salt=51), 150]),
            ("04-extremes", [[-1000000000, 1000000000, 0], 2]),
        ],
        "performance": [
            ("01-small", [randoms(5000, -1000000000, 1000000000, salt=52), 2500]),
            ("02-medium", [randoms(50000, -1000000000, 1000000000, salt=53), 25000]),
            ("03-large", [randoms(200000, -1000000000, 1000000000, salt=54), 100000]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 정렬 후 인덱스.
//
// O(n log n) 이다. 선택 알고리즘으로 O(n) 까지 줄일 수 있지만, 이 문제의 성능 그룹은
// O(n^2) 를 걸러 내는 것이 목적이라 정렬이면 충분하다.
fun kthLargest(nums: IntArray, k: Int): Int {
    val sorted = nums.sortedDescending()
    for (index in 0 until minOf(k, sorted.size)) {
        Drill.visit(index, sorted[index])
    }
    Drill.write(0, sorted[k - 1])
    return sorted[k - 1]
}
""",
    mutants=[
        ("kth-smallest--wrong-end", "WRONG_BRANCH",
         "작은 쪽에서 k 번째를 센다.",
         """
fun kthLargest(nums: IntArray, k: Int): Int = nums.sorted()[k - 1]
"""),
        ("distinct-only--drops-duplicates", "MISSING_EDGE_CASE",
         "중복을 없애고 세어 같은 값이 여러 개면 틀린다.",
         """
fun kthLargest(nums: IntArray, k: Int): Int {
    val distinct = nums.distinct().sortedDescending()
    return distinct[minOf(k, distinct.size) - 1]
}
"""),
        ("selection-sort--quadratic", "PERFORMANCE",
         "선택 정렬로 k 번 훑어 O(n*k) 다. 작은 입력은 통과한다.",
         """
fun kthLargest(nums: IntArray, k: Int): Int {
    val values = nums.copyOf()
    for (round in 0 until k) {
        var best = round
        for (i in round + 1 until values.size) {
            if (values[i] > values[best]) best = i
        }
        val swap = values[round]
        values[round] = values[best]
        values[best] = swap
    }
    return values[k - 1]
}
"""),
    ],
))


# --- 12. 역순 쌍 세기 --------------------------------------------------------

def _inversions(nums):
    def sort(values):
        if len(values) <= 1:
            return values, 0
        mid = len(values) // 2
        left, a = sort(values[:mid])
        right, b = sort(values[mid:])
        merged, count = [], a + b
        i = j = 0
        while i < len(left) and j < len(right):
            if left[i] <= right[j]:
                merged.append(left[i]); i += 1
            else:
                merged.append(right[j]); j += 1
                count += len(left) - i
        merged += left[i:] + right[j:]
        return merged, count
    return sort(list(nums))[1]


PROBLEMS.append(Problem(
    id="count-inversions",
    title="역순 쌍의 개수",
    summary="""
정수 배열 `nums` 에서 `i < j` 이면서 `nums[i] > nums[j]` 인 쌍의 개수를 반환한다.
"얼마나 정렬되어 있지 않은가"를 재는 값이다.
""",
    notes="""
같은 값은 역순이 아니다. 두 겹으로 세면 O(n^2) 이고, 병합 정렬로 세면 O(n log n) 이다 —
병합할 때 오른쪽 값이 먼저 나오면, 왼쪽에 남은 원소 수만큼 역순 쌍이 한꺼번에 생긴다.
""",
    drill_doc="""
Drill.compare(left, right)  // 두 쪽의 앞을 견줬다
Drill.write(index, value)   // 병합 결과에 썼다
Drill.call("sort")          // 재귀로 내려갔다
""",
    constraints="""
- `1 <= nums.size <= 120_000`
- `-10^9 <= nums[i] <= 10^9`
- 정답은 `Int` 범위를 넘지 않는다
""",
    signature=dict(name="countInversions", parameters=[("nums", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(),
    reference=_inversions,
    cases={
        "sample": [
            ("01", [[2, 4, 1, 3, 5]]),
            ("02", [[5, 4, 3, 2, 1]]),
        ],
        "boundary": [
            ("01-single", [[1]]),
            ("02-sorted", [[1, 2, 3, 4, 5]]),
            # 같은 값은 역순이 아니다. `>=` 로 센 풀이가 걸린다.
            ("03-all-equal", [[3, 3, 3, 3]]),
            ("04-two-swapped", [[2, 1]]),
            ("05-duplicates-mixed", [[2, 1, 2, 1]]),
        ],
        "hidden": [
            ("01-reverse-long", [list(range(60, 0, -1))]),
            ("02-random", [randoms(200, -50, 50, salt=61)]),
            ("03-nearly-sorted", [shuffled(list(range(100)), salt=62)]),
        ],
        "performance": [
            ("01-small", [randoms(3000, -1000000, 1000000, salt=63)]),
            ("02-medium", [randoms(30000, -1000000, 1000000, salt=64)]),
            # 두 겹 풀이의 비용은 **답과 무관하게** 모든 쌍을 보는 것이다. 그래서
            # 이미 정렬된 큰 배열이면 답은 0 이면서 비교는 n^2/2 번 일어난다.
            #
            # 내림차순으로 크게 잡을 수는 없다. 역순 쌍이 n^2/2 이라 n 이 65_536 만
            # 넘어도 답이 Int 를 넘어, 제약에 적은 것과 어긋난다.
            ("03-large-sorted", [list(range(1, 120001))]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 병합 정렬로 세기.
//
// 병합할 때 오른쪽 값이 먼저 나오면, 왼쪽에 남아 있는 원소는 전부 그 값보다 크다.
// 그래서 한 번에 "남은 개수"만큼 역순 쌍을 더할 수 있고, 이것이 O(n^2) 를 O(n log n)
// 으로 줄이는 지점이다.
fun countInversions(nums: IntArray): Int {
    val buffer = IntArray(nums.size)
    val values = nums.copyOf()
    var count = 0

    fun sort(from: Int, to: Int) {
        if (to - from <= 1) return
        val mid = (from + to) / 2
        Drill.call("sort")
        sort(from, mid)
        sort(mid, to)

        var left = from
        var right = mid
        var write = from

        while (left < mid && right < to) {
            Drill.compare(left, right)
            if (values[left] <= values[right]) {
                buffer[write] = values[left]
                left += 1
            } else {
                buffer[write] = values[right]
                right += 1
                // 왼쪽에 남은 것은 전부 이 값보다 크다.
                count += mid - left
            }
            Drill.write(write, buffer[write])
            write += 1
        }
        while (left < mid) { buffer[write] = values[left]; left += 1; write += 1 }
        while (right < to) { buffer[write] = values[right]; right += 1; write += 1 }
        for (i in from until to) values[i] = buffer[i]
        Drill.ret("sort", count)
    }

    sort(0, values.size)
    return count
}
""",
    mutants=[
        ("counts-ties--uses-ge", "WRONG_BRANCH",
         "같은 값도 역순으로 센다.",
         """
fun countInversions(nums: IntArray): Int {
    var count = 0
    for (i in nums.indices) {
        for (j in i + 1 until nums.size) {
            if (nums[i] >= nums[j]) count += 1
        }
    }
    return count
}
"""),
        ("adjacent-only--misses-pairs", "MISSING_EDGE_CASE",
         "이웃한 쌍만 센다.",
         """
fun countInversions(nums: IntArray): Int {
    var count = 0
    for (i in 0 until nums.size - 1) if (nums[i] > nums[i + 1]) count += 1
    return count
}
"""),
        ("quadratic--checks-every-pair", "PERFORMANCE",
         "모든 쌍을 본다. 작은 입력은 통과한다.",
         """
fun countInversions(nums: IntArray): Int {
    var count = 0
    for (i in nums.indices) {
        for (j in i + 1 until nums.size) {
            if (nums[i] > nums[j]) count += 1
        }
    }
    return count
}
"""),
    ],
))
