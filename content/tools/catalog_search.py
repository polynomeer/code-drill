"""정렬·탐색 (역량: 순서를 이용해 후보를 반으로 줄이기)."""

from author import Problem, standard_groups, perf_groups, randoms, shuffled, flat

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
            Drill.compare(i, best)
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
    # v2: 성능 케이스를 키웠다. 두 겹 풀이가 한도를 배로만 넘겨, 한가한 머신에서는
    # 통과하고 바쁜 머신에서만 잡혔다 (§12.1 재현성).
    version=2,
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
            ("03-large-sorted", [list(range(1, 300001))]),
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
            Drill.compare(i, j)
            if (nums[i] > nums[j]) count += 1
        }
    }
    return count
}
"""),
    ],
))


# --- 66. 정수 제곱근 ------------------------------------------------------------------

def _isqrt(n):
    import math
    return math.isqrt(n)


PROBLEMS.append(Problem(
    id="integer-square-root",
    title="정수 제곱근",
    summary="""
0 이상의 정수 `n` 이 주어진다. 제곱이 `n` 을 넘지 않는 **가장 큰 정수**를 반환한다.
즉 `√n` 을 내림한 값이다. 예: `8` → `2`, `9` → `3`, `2147395599` → `46339`.

부동소수점 제곱근 함수는 쓰지 않는다 — 큰 수에서 반올림이 어긋난다.
""",
    notes="""
답은 0 부터 n 사이에서 "제곱이 n 이하인가"가 참에서 거짓으로 한 번 바뀌는 지점이다.
이분 탐색이 맞고, 가운데 값의 제곱이 **Int 를 넘칠 수 있다**는 것이 이 문제의 함정이다.
""",
    drill_doc="""
Drill.compare(lo, hi)         // 탐색 구간
Drill.visit(mid, 0)           // 가운데를 시험했다
""",
    constraints="""
- `0 <= n <= 2^31 - 1`
""",
    signature=dict(name="isqrt", parameters=[("n", "INT")], returns="INT"),
    groups=standard_groups(),
    reference=_isqrt,
    cases={
        "sample": [
            ("01", [8]),
            ("02", [9]),
        ],
        "boundary": [
            ("01-zero", [0]),
            ("02-one", [1]),
            ("03-two", [2]),
            ("04-three", [3]),
            ("05-four", [4]),
            # 완전제곱수 직전과 직후.
            ("06-before-square", [99]),
            ("07-after-square", [101]),
            # 46340² = 2147395600 은 Int 안이고, 46341² 은 넘친다. 그 사이의 값들.
            ("08-near-overflow-below", [2147395599]),
            ("09-near-overflow-exact", [2147395600]),
            ("10-max-int", [2147483647]),
        ],
        "hidden": [
            ("01-random-small", [randoms(1, 0, 1000, salt=2601)[0]]),
            ("02-random-large", [randoms(1, 0, 2147483647, salt=2602)[0]]),
            ("03-square", [1000000 * 1000000 // 1000000 * 1000]),
            ("04-large-square", [46340 * 46340]),
            ("05-large-plus-one", [46340 * 46340 + 1]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 답에 대한 이분 탐색, 제곱은 Long 으로.
fun isqrt(n: Int): Int {
    var lo = 0
    var hi = minOf(n.toLong(), 46340L).toInt()
    while (lo < hi) {
        val mid = (lo + hi + 1) / 2
        Drill.compare(lo, hi)
        Drill.visit(mid, 0)
        if (mid.toLong() * mid <= n) lo = mid else hi = mid - 1
    }
    return lo
}
""",
    mutants=[
        ("int-square--overflows", "MISSING_EDGE_CASE",
         "가운데 값의 제곱을 Int 로 계산한다. 46341 부터 넘쳐 음수가 되고 조건이 뒤집힌다.",
         """
fun isqrt(n: Int): Int {
    var lo = 0
    var hi = n
    while (lo < hi) {
        val mid = (lo + hi + 1) / 2
        if (mid * mid <= n) lo = mid else hi = mid - 1
    }
    return lo
}
"""),
        ("rounds-up", "OFF_BY_ONE",
         "제곱이 n 을 처음 넘는 값을 답한다. 완전제곱수가 아니면 하나 크다.",
         """
fun isqrt(n: Int): Int {
    var lo = 0
    var hi = 46341
    while (lo < hi) {
        val mid = (lo + hi) / 2
        if (mid.toLong() * mid < n) lo = mid + 1 else hi = mid
    }
    return lo
}
"""),
        ("float-sqrt", "WRONG_ALGORITHM",
         "부동소수점 제곱근을 내림한다. 큰 수에서 반올림이 어긋난다 — 그리고 문제가 쓰지 말라고 했다.",
         """
fun isqrt(n: Int): Int = Math.sqrt(n.toFloat().toDouble()).toInt()
"""),
    ],
))


# --- 77. 정렬된 격자에서 k 번째로 작은 값 (답에 대한 이분 탐색) --------------------------------

def _kth_in_sorted_matrix(grid, k):
    n = len(grid)
    lo, hi = grid[0][0], grid[n - 1][n - 1]
    while lo < hi:
        mid = (lo + hi) // 2
        count = 0
        col = n - 1
        for row in grid:
            while col >= 0 and row[col] > mid:
                col -= 1
            count += col + 1
        if count < k:
            lo = mid + 1
        else:
            hi = mid
    return lo


def _sorted_grid(n, salt):
    values = sorted(randoms(n * n, -100000, 100000, salt=salt))
    grid = [[0] * n for _ in range(n)]
    # 행과 열이 각각 오름차순이 되도록 대각선 순으로 채운다.
    cells = sorted(((r + c, r, c) for r in range(n) for c in range(n)))
    for value, (_, r, c) in zip(values, cells):
        grid[r][c] = value
    return grid


PROBLEMS.append(Problem(
    id="kth-in-sorted-matrix",
    title="정렬된 격자에서 k 번째로 작은 값",
    summary="""
`n × n` 정수 격자 `grid` 가 주어진다. **모든 행이 오름차순이고 모든 열도 오름차순**이다.
격자 전체의 값 중 `k` 번째로 작은 값을 반환한다 (1 부터 센다). 같은 값은 각각 센다.

예: `[[1, 5, 9], [10, 11, 13], [12, 13, 15]]` 에서 `k = 8` 이면 `13` 이다.
""",
    notes="""
전부 꺼내 정렬해도 답은 나오지만 격자가 정렬돼 있다는 사실을 쓰지 않은 것이다. "값 x 이하가
몇 개인가"는 행마다 오른쪽 위에서 시작해 계단처럼 내려가면 O(n) 에 센다. 그 개수가 k 이상이
되는 가장 작은 x 가 답이고, x 는 이분 탐색으로 찾는다 — 격자에 있는 값이어야 하지만, 그
탐색은 저절로 격자의 값에 멈춘다.
""",
    drill_doc="""
Drill.compare(lo, hi)         // 답의 후보 구간
Drill.visit(mid, count)       // mid 이하의 개수를 셌다
""",
    constraints="""
- `1 <= n <= 1_000`
- `-10^5 <= grid[i][j] <= 10^5`
- `1 <= k <= n²`
""",
    signature=dict(name="kthSmallest", parameters=[("grid", "INT_MATRIX"), ("k", "INT")],
                   returns="INT"),
    groups=perf_groups(time_multiplier=0.5),
    reference=_kth_in_sorted_matrix,
    cases={
        "sample": [
            ("01", [[[1, 5, 9], [10, 11, 13], [12, 13, 15]], 8]),
            ("02", [[[-5]], 1]),
        ],
        "boundary": [
            ("01-first", [[[1, 2], [3, 4]], 1]),
            ("02-last", [[[1, 2], [3, 4]], 4]),
            # 같은 값이 여럿. 각각 센다.
            ("03-duplicates", [[[1, 1, 1], [1, 1, 1], [1, 1, 2]], 8]),
            ("04-all-same", [[[7, 7], [7, 7]], 3]),
            # 음수와 양수가 섞였다. 가운데 값의 계산이 음수를 지난다.
            ("05-negatives", [[[-10, -3], [-2, 5]], 2]),
            # 격자에 없는 값이 답이 되면 안 된다. 2 와 8 사이의 값을 답하면 틀린다.
            ("06-gap", [[[1, 2], [8, 9]], 2]),
            ("07-gap-upper", [[[1, 2], [8, 9]], 3]),
        ],
        "hidden": [
            ("01-random-small", [_sorted_grid(5, salt=4001), 13]),
            ("02-random-medium", [_sorted_grid(40, salt=4002), 777]),
            ("03-random-medium-first", [_sorted_grid(40, salt=4003), 1]),
            ("04-random-medium-last", [_sorted_grid(40, salt=4004), 1600]),
            ("05-many-duplicates", [[[v // 3 for v in range(r * 6, r * 6 + 6)] for r in range(6)], 20]),
        ],
        "performance": [
            ("01-small", [_sorted_grid(200, salt=4005), 20000]),
            ("02-medium", [_sorted_grid(500, salt=4006), 125000]),
            ("03-large", [_sorted_grid(1000, salt=4007), 700000]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 값에 대한 이분 탐색 + 계단 세기.
fun kthSmallest(grid: Array<IntArray>, k: Int): Int {
    val n = grid.size
    var lo = grid[0][0]
    var hi = grid[n - 1][n - 1]
    while (lo < hi) {
        val mid = Math.floorDiv(lo + hi, 2)
        Drill.compare(lo, hi)
        var count = 0
        var col = n - 1
        for (row in grid) {
            while (col >= 0 && row[col] > mid) col -= 1
            count += col + 1
        }
        Drill.visit(mid, count)
        if (count < k) lo = mid + 1 else hi = mid
    }
    return lo
}
""",
    mutants=[
        ("count-from-scratch--per-row", "PERFORMANCE",
         "행마다 처음부터 끝까지 세고, 값의 범위 전체를 하나씩 시험한다. 격자의 정렬을 쓰지 않는다.",
         """
fun kthSmallest(grid: Array<IntArray>, k: Int): Int {
    val n = grid.size
    var candidate = grid[0][0]
    while (true) {
        var count = 0
        for (row in grid) for (v in row) { Drill.compare(v, candidate); if (v <= candidate) count += 1 }
        if (count >= k) return candidate
        candidate += 1
    }
}
"""),
        ("upper-bound--off-by-one", "OFF_BY_ONE",
         "개수가 k 를 넘는 가장 작은 값을 찾는다. k 이상이어야 한다.",
         """
fun kthSmallest(grid: Array<IntArray>, k: Int): Int {
    val n = grid.size
    var lo = grid[0][0]; var hi = grid[n - 1][n - 1]
    while (lo < hi) {
        val mid = Math.floorDiv(lo + hi, 2)
        var count = 0; var col = n - 1
        for (row in grid) { while (col >= 0 && row[col] > mid) col -= 1; count += col + 1 }
        if (count <= k) lo = mid + 1 else hi = mid
    }
    return lo
}
"""),
        ("row-scan-resets--wrong-count", "WRONG_BRANCH",
         "행마다 열 포인터를 되돌려 왼쪽부터 다시 센다 — 개수는 맞지만, 되돌린 뒤 mid 보다 큰 값을 세는 조건이 뒤집혔다.",
         """
fun kthSmallest(grid: Array<IntArray>, k: Int): Int {
    val n = grid.size
    var lo = grid[0][0]; var hi = grid[n - 1][n - 1]
    while (lo < hi) {
        val mid = Math.floorDiv(lo + hi, 2)
        var count = 0
        for (row in grid) { var col = 0; while (col < n && row[col] < mid) col += 1; count += col }
        if (count < k) lo = mid + 1 else hi = mid
    }
    return lo
}
"""),
        ("first-row-only", "WRONG_ALGORITHM",
         "첫 행만 정렬된 것으로 보고 k 번째 원소를 답한다.",
         """
fun kthSmallest(grid: Array<IntArray>, k: Int): Int {
    val flat = grid[0]
    return flat[minOf(k - 1, flat.size - 1)]
}
"""),
    ],
))


# --- 96. 정렬된 목록 k개 합치기 (힙) --------------------------------------------------------

def _merge_k_sorted(sizes, values):
    import heapq
    lists = []
    at = 0
    for n in sizes:
        lists.append(values[at:at + n])
        at += n
    return list(heapq.merge(*lists))


def _sorted_lists(k, each, salt):
    """길이가 each 근처에서 흔들리는 정렬 목록 k개 → (sizes, values)."""
    sizes = randoms(k, max(0, each - 3), each + 3, salt=salt)
    values = []
    for i, n in enumerate(sizes):
        values += sorted(randoms(n, -100000, 100000, salt=salt + 1 + i))
    return [sizes, values]


def _lists(*lists):
    return [[len(l) for l in lists], flat(list(l) for l in lists)]


PROBLEMS.append(Problem(
    id="merge-k-sorted",
    title="정렬된 목록 k개 합치기",
    summary="""
각각 오름차순인 정수 목록 `k` 개가 평탄하게 주어진다 — `sizes[i]` 는 `i` 번 목록의 길이이고,
`values` 는 목록들을 차례로 이어 붙인 것이다 (길이는 서로 다를 수 있고 `0` 도 있다).
전부 합쳐 오름차순으로 만든 배열을 반환한다. 같은 값은 각각 남긴다.

예: `sizes = [3, 3, 2]`, `values = [1, 4, 5, 1, 3, 4, 2, 6]` → `[1, 1, 2, 3, 4, 4, 5, 6]`.
""",
    notes="""
전부 정렬하면 O(N log N) 이지만 이미 정렬돼 있다는 것을 버린 것이다. 목록마다 앞 원소
하나씩만 힙에 두면 가장 작은 것이 O(log k) 에 나오고, 꺼낸 목록의 다음 원소를 넣는다.
목록을 둘씩 짝지어 합치는 분할 정복도 같은 O(N log k) 다. 하나씩 차례로 합치면 O(N·k).
""",
    drill_doc="""
Drill.push(v)                 // 힙에 넣었다
Drill.pop(v)                  // 힙에서 꺼내 답에 붙였다
""",
    constraints="""
- `0 <= k <= 200_000`, `values.size = sizes 의 합 <= 200_000`
- `-10^5 <= 값 <= 10^5`, 각 목록은 오름차순
""",
    signature=dict(name="mergeSorted", parameters=[("sizes", "INT_ARRAY"), ("values", "INT_ARRAY")],
                   returns="INT_ARRAY"),
    groups=perf_groups(time_multiplier=0.5),
    reference=_merge_k_sorted,
    cases={
        "sample": [
            ("01", _lists([1, 4, 5], [1, 3, 4], [2, 6])),
            ("02", _lists([7])),
        ],
        "boundary": [
            ("01-no-lists", _lists()),
            ("02-empty-lists", _lists([], [])),
            ("03-some-empty", _lists([], [2, 3], [], [1])),
            ("04-duplicates", _lists([1, 1], [1], [1, 1, 1])),
            # 한 목록이 다른 것보다 전부 작다.
            ("05-disjoint-ranges", _lists([10, 11, 12], [1, 2, 3])),
            ("06-negatives", _lists([-5, -1], [-3, 0], [-100000])),
            ("07-single-long", _lists(list(range(-5, 6)))),
        ],
        "hidden": [
            ("01-random-small", _sorted_lists(4, 6, salt=5201)),
            ("02-random-medium", _sorted_lists(20, 50, salt=5210)),
            ("03-ragged", _lists(*[sorted(randoms(n, -1000, 1000, salt=5240 + n)) for n in [0, 5, 1, 30, 0, 12]])),
            ("04-many-tiny", _lists(*[[v] for v in randoms(300, -100, 100, salt=5250)])),
        ],
        "performance": [
            ("01-small", _sorted_lists(2000, 10, salt=5300)),
            ("02-medium", _sorted_lists(50000, 3, salt=8000)),
            # 목록이 원소 하나씩 20만 개. 하나씩 합치면 N²/2 다.
            ("03-large", [[1] * 200000, randoms(200000, -100000, 100000, salt=60000)]),
        ],
    },
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 4000000},
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 목록마다 앞 원소 하나씩만 힙에 둔다. O(N log k).
fun mergeSorted(sizes: IntArray, values: IntArray): IntArray {
    val start = IntArray(sizes.size + 1)
    for (i in sizes.indices) start[i + 1] = start[i] + sizes[i]
    val out = IntArray(values.size)
    // (값, 목록 번호, values 안의 자리)
    val heap = java.util.PriorityQueue<IntArray>(compareBy { it[0] })
    for (i in sizes.indices) if (sizes[i] > 0) { heap.add(intArrayOf(values[start[i]], i, start[i])); Drill.push(values[start[i]]) }
    var n = 0
    while (heap.isNotEmpty()) {
        val (v, i, j) = heap.poll()
        Drill.pop(v)
        out[n++] = v
        if (j + 1 < start[i + 1]) { heap.add(intArrayOf(values[j + 1], i, j + 1)); Drill.push(values[j + 1]) }
    }
    return out
}
""",
    mutants=[
        ("drops-duplicates", "MISSING_EDGE_CASE",
         "같은 값을 하나로 합친다. 각각 남겨야 한다.",
         """
fun mergeSorted(sizes: IntArray, values: IntArray): IntArray = values.toSortedSet().toIntArray()
"""),
        ("forgets-next-of-popped", "WRONG_BRANCH",
         "꺼낸 목록의 다음 원소를 넣지 않는다. 목록마다 첫 원소만 나온다.",
         """
fun mergeSorted(sizes: IntArray, values: IntArray): IntArray {
    val heap = java.util.PriorityQueue<Int>()
    var at = 0
    for (n in sizes) { if (n > 0) heap.add(values[at]); at += n }
    val out = ArrayList<Int>()
    while (heap.isNotEmpty()) out.add(heap.poll())
    return out.toIntArray()
}
"""),
        ("ignores-list-boundary", "OFF_BY_ONE",
         "목록의 끝을 보지 않고 다음 자리를 넣는다. 다음 목록의 첫 원소가 이 목록 것처럼 들어와 순서가 깨진다.",
         """
fun mergeSorted(sizes: IntArray, values: IntArray): IntArray {
    val out = IntArray(values.size)
    val heap = java.util.PriorityQueue<Int>(compareBy { values[it] })
    var at = 0
    for (n in sizes) { if (n > 0) heap.add(at); at += n }
    var n = 0
    while (heap.isNotEmpty()) {
        val j = heap.poll()
        out[n++] = values[j]
        if (j + 1 < values.size) heap.add(j + 1)
    }
    return out
}
"""),
        ("merge-one-by-one", "PERFORMANCE",
         "목록을 하나씩 차례로 합친다. 앞의 결과를 매번 다시 훑어 O(N·k).",
         """
fun mergeSorted(sizes: IntArray, values: IntArray): IntArray {
    var acc = IntArray(0)
    var at = 0
    for (size in sizes) {
        val list = values.copyOfRange(at, at + size); at += size
        val merged = IntArray(acc.size + list.size)
        var i = 0; var j = 0; var n = 0
        while (i < acc.size || j < list.size) {
            Drill.compare(i, j)
            merged[n++] = if (j >= list.size || (i < acc.size && acc[i] <= list[j])) acc[i++] else list[j++]
        }
        acc = merged
    }
    return acc
}
"""),
    ],
))


# --- 107. 두 정렬 배열의 중앙값 (분할 이분 탐색) ------------------------------------------------

def _median_doubled(a, b):
    merged = sorted(a + b)
    n = len(merged)
    if n % 2 == 1:
        return 2 * merged[n // 2]
    return merged[n // 2 - 1] + merged[n // 2]


PROBLEMS.append(Problem(
    id="median-two-sorted",
    title="두 정렬 배열의 중앙값",
    summary="""
오름차순 정수 배열 `a`, `b` 가 주어진다 (합쳐서 원소가 하나 이상). 둘을 합친 전체의
**중앙값의 두 배**를 반환한다 — 원소 수가 홀수면 가운데 값의 두 배, 짝수면 가운데 두 값의 합.

예: `a = [1, 3]`, `b = [2]` → 중앙값 2, 답 `4`. `a = [1, 2]`, `b = [3, 4]` → 중앙값 2.5, 답 `5`.
""",
    notes="""
합쳐 정렬하면 O((m+n) log) 이고 반쯤 합치면 O(m+n) 인데, 둘 다 맞는다 — 이 문제는 성능이
아니라 **경계**의 문제다. 짧은 배열에서 자르는 자리를 이분 탐색하면 O(log min(m, n)) 이다:
왼쪽 절반의 최댓값이 오른쪽 절반의 최솟값보다 크지 않은 자리를 찾는다. 한쪽이 비는 경우,
짝수·홀수, 두 배열 중 하나가 통째로 왼쪽에 들어가는 경우가 갈림길이다.
""",
    drill_doc="""
Drill.compare(i, j)           // 자르는 자리를 견줬다
Drill.pointer("cut", i)       // 짧은 배열의 자르는 자리
""",
    constraints="""
- `0 <= a.size, b.size <= 100_000`, `a.size + b.size >= 1`
- `-10^6 <= 값 <= 10^6`
""",
    signature=dict(name="medianDoubled", parameters=[("a", "INT_ARRAY"), ("b", "INT_ARRAY")], returns="INT"),
    groups=standard_groups(),
    reference=_median_doubled,
    cases={
        "sample": [("01", [[1, 3], [2]]), ("02", [[1, 2], [3, 4]])],
        "boundary": [
            ("01-one-empty", [[], [5]]),
            ("02-other-empty", [[2, 4, 6], []]),
            ("03-single-each", [[1], [2]]),
            # 한 배열이 통째로 왼쪽에 들어간다.
            ("04-disjoint", [[1, 2, 3], [10, 20, 30, 40]]),
            ("05-disjoint-reversed", [[10, 20, 30, 40], [1, 2, 3]]),
            # 같은 값이 양쪽에.
            ("06-duplicates", [[1, 1, 1], [1, 1]]),
            ("07-negatives", [[-5, -3], [-4, -2, -1]]),
            ("08-interleaved-even", [[1, 3, 5, 7], [2, 4, 6, 8]]),
        ],
        "hidden": [
            ("01-random", [sorted(randoms(30, -100, 100, salt=7401)), sorted(randoms(45, -100, 100, salt=7402))]),
            ("02-random-uneven", [sorted(randoms(3, -100, 100, salt=7403)), sorted(randoms(400, -100, 100, salt=7404))]),
            ("03-large", [sorted(randoms(100000, -1000000, 1000000, salt=7405)), sorted(randoms(99999, -1000000, 1000000, salt=7406))]),
            ("04-all-same", [[7] * 100, [7] * 101]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 짧은 배열에서 자르는 자리를 이분 탐색한다.
fun medianDoubled(a: IntArray, b: IntArray): Int {
    val (x, y) = if (a.size <= b.size) a to b else b to a
    val m = x.size; val n = y.size
    var lo = 0; var hi = m
    val half = (m + n + 1) / 2
    while (lo <= hi) {
        val i = (lo + hi) / 2
        val j = half - i
        Drill.pointer("cut", i)
        val leftX = if (i == 0) Int.MIN_VALUE else x[i - 1]
        val rightX = if (i == m) Int.MAX_VALUE else x[i]
        val leftY = if (j == 0) Int.MIN_VALUE else y[j - 1]
        val rightY = if (j == n) Int.MAX_VALUE else y[j]
        Drill.compare(i, j)
        if (leftX <= rightY && leftY <= rightX) {
            val leftMax = maxOf(leftX, leftY)
            if ((m + n) % 2 == 1) return 2 * leftMax
            return leftMax + minOf(rightX, rightY)
        } else if (leftX > rightY) hi = i - 1 else lo = i + 1
    }
    error("정렬된 입력이면 여기 오지 않는다")
}
""",
    mutants=[
        ("even-uses-left-max-twice", "WRONG_BRANCH", "짝수 개일 때 왼쪽 최댓값을 두 배 한다. 오른쪽 최솟값을 잊었다.", """
fun medianDoubled(a: IntArray, b: IntArray): Int {
    val (x, y) = if (a.size <= b.size) a to b else b to a
    val m = x.size; val n = y.size
    var lo = 0; var hi = m
    val half = (m + n + 1) / 2
    while (lo <= hi) {
        val i = (lo + hi) / 2; val j = half - i
        val leftX = if (i == 0) Int.MIN_VALUE else x[i - 1]; val rightX = if (i == m) Int.MAX_VALUE else x[i]
        val leftY = if (j == 0) Int.MIN_VALUE else y[j - 1]; val rightY = if (j == n) Int.MAX_VALUE else y[j]
        if (leftX <= rightY && leftY <= rightX) return 2 * maxOf(leftX, leftY)
        else if (leftX > rightY) hi = i - 1 else lo = i + 1
    }
    error("")
}
"""),
        ("no-swap--long-first", "MISSING_EDGE_CASE", "짧은 배열을 고르지 않는다. j 가 음수가 되어 범위 밖을 읽거나 틀린다.", """
fun medianDoubled(a: IntArray, b: IntArray): Int {
    val x = a; val y = b
    val m = x.size; val n = y.size
    var lo = 0; var hi = m
    val half = (m + n + 1) / 2
    while (lo <= hi) {
        val i = (lo + hi) / 2; val j = half - i
        val leftX = if (i == 0) Int.MIN_VALUE else x[i - 1]; val rightX = if (i == m) Int.MAX_VALUE else x[i]
        val leftY = if (j <= 0) Int.MIN_VALUE else y[j - 1]; val rightY = if (j >= n) Int.MAX_VALUE else y[j]
        if (leftX <= rightY && leftY <= rightX) {
            val leftMax = maxOf(leftX, leftY)
            if ((m + n) % 2 == 1) return 2 * leftMax
            return leftMax + minOf(rightX, rightY)
        } else if (leftX > rightY) hi = i - 1 else lo = i + 1
    }
    error("")
}
"""),
        ("floor-half", "OFF_BY_ONE", "왼쪽 절반의 크기를 (m+n)/2 로 잡는다. 홀수 개일 때 가운데가 오른쪽으로 간다.", """
fun medianDoubled(a: IntArray, b: IntArray): Int {
    val (x, y) = if (a.size <= b.size) a to b else b to a
    val m = x.size; val n = y.size
    var lo = 0; var hi = m
    val half = (m + n) / 2
    while (lo <= hi) {
        val i = (lo + hi) / 2; val j = half - i
        if (j < 0) { hi = i - 1; continue }
        if (j > n) { lo = i + 1; continue }
        val leftX = if (i == 0) Int.MIN_VALUE else x[i - 1]; val rightX = if (i == m) Int.MAX_VALUE else x[i]
        val leftY = if (j == 0) Int.MIN_VALUE else y[j - 1]; val rightY = if (j == n) Int.MAX_VALUE else y[j]
        if (leftX <= rightY && leftY <= rightX) {
            val leftMax = maxOf(leftX, leftY)
            if ((m + n) % 2 == 1) return 2 * leftMax
            return leftMax + minOf(rightX, rightY)
        } else if (leftX > rightY) hi = i - 1 else lo = i + 1
    }
    error("")
}
"""),
    ],
))


# --- 125. 합이 범위 안인 구간의 수 (누적합 위의 분할 정복, EXPERT) --------------------------------------

def _count_range_sums(nums, lower, upper):
    prefix = [0]
    for x in nums:
        prefix.append(prefix[-1] + x)

    def count(lo, hi):
        if hi - lo <= 1:
            return 0
        mid = (lo + hi) // 2
        total = count(lo, mid) + count(mid, hi)
        j = k = mid
        for left in prefix[lo:mid]:
            while j < hi and prefix[j] - left < lower:
                j += 1
            while k < hi and prefix[k] - left <= upper:
                k += 1
            total += k - j
        prefix[lo:hi] = sorted(prefix[lo:hi])
        return total

    return count(0, len(prefix))


PROBLEMS.append(Problem(
    id="count-range-sums",
    title="합이 범위 안인 구간의 수",
    summary="""
정수 배열 `nums` 와 두 정수 `lower <= upper` 가 주어진다. 비어 있지 않은 연속 부분 배열
중 원소의 합이 `lower` 이상 `upper` 이하인 것의 **개수**를 반환한다.

예: `[-2, 5, -1]`, `lower = -2`, `upper = 2` 이면 `[-2]`, `[-2, 5, -1]`, `[5, -1]` 셋이다.
""",
    notes="""
구간의 합은 누적합의 차 `P[j] - P[i]` 다. 그래서 문제는 "`i < j` 이고 `P[j] - P[i]` 가 범위 안인
쌍의 수"가 된다. 모든 쌍을 보면 n² 이다. 누적합을 절반으로 나눠 왼쪽 절반의 `i` 와 오른쪽
절반의 `j` 사이의 쌍만 세고 양쪽을 정렬해 두면, 각 `i` 에 대해 범위에 드는 `j` 가 연속한
구간이라 두 포인터로 센다 — 병합 정렬로 역전 쌍을 세는 것과 같은 뼈대다. 누적합은 `Int` 를
넘을 수 있다.
""",
    drill_doc="""
Drill.compare(i, j)           // 왼쪽 누적합과 오른쪽 누적합을 비교했다
Drill.write(index, value)     // 병합해 정렬된 누적합을 적었다
""",
    constraints="""
- `1 <= nums.length <= 100_000`
- `-10^9 <= nums[i] <= 10^9`
- `-10^9 <= lower <= upper <= 10^9`
- 답은 `Int` 범위 안이다
""",
    signature=dict(
        name="countRangeSums",
        parameters=[("nums", "INT_ARRAY"), ("lower", "INT"), ("upper", "INT")],
        returns="INT",
    ),
    # 모든 쌍을 보는 오답이 10 만에서 한도의 2.3 배로 겨우 넘겼다. 입력은 자료형 상한이라 한도를 조인다.
    groups=perf_groups(time_multiplier=0.5),
    reference=_count_range_sums,
    cases={
        "sample": [
            ("01", [[-2, 5, -1], -2, 2]),
            ("02", [[0], 0, 0]),
        ],
        "boundary": [
            ("01-single-in", [[3], 1, 5]),
            ("02-single-out", [[3], 4, 5]),
            # 경계값이 포함이다 — 이상·이하.
            ("03-inclusive-bounds", [[1, 2], 1, 3]),
            # 길이 1 짜리 구간도 센다.
            ("04-length-one-counts", [[1, 1, 1], 1, 1]),
            # 누적합이 Int 를 넘는다.
            ("05-int-overflow", [[1000000000, 1000000000, 1000000000, -1000000000], 0, 2000000000]),
            ("06-all-negative", [[-1, -2, -3], -3, -1]),
            ("07-zeros", [[0, 0, 0], 0, 0]),
            ("08-range-below-all", [[5, 6, 7], -10, -1]),
        ],
        "hidden": [
            ("01-random-small", [randoms(12, -10, 10, salt=8341), -3, 4]),
            ("02-random-medium", [randoms(200, -100, 100, salt=8342), -50, 50]),
            ("03-random-wide", [randoms(500, -1000000000, 1000000000, salt=8343), -1000000000, 1000000000]),
            ("04-mixed-large", [randoms(300, -1000000000, 1000000000, salt=8344), 0, 500000000]),
        ],
        "performance": [
            ("01-small", [randoms(5000, -1000, 1000, salt=8351), -100, 100]),
            ("02-medium", [randoms(30000, -1000, 1000, salt=8352), -100, 100]),
            ("03-large", [randoms(100000, -1000, 1000, salt=8353), -50, 50]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 누적합 위에서 병합 정렬하며 범위에 드는 쌍을 두 포인터로 센다.
fun countRangeSums(nums: IntArray, lower: Int, upper: Int): Int {
    val n = nums.size
    val prefix = LongArray(n + 1)
    for (i in 0 until n) prefix[i + 1] = prefix[i] + nums[i]
    val buffer = LongArray(n + 1)
    fun count(lo: Int, hi: Int): Int {
        if (hi - lo <= 1) return 0
        val mid = (lo + hi) / 2
        var total = count(lo, mid) + count(mid, hi)
        var j = mid
        var k = mid
        for (i in lo until mid) {
            while (j < hi && prefix[j] - prefix[i] < lower) j += 1
            while (k < hi && prefix[k] - prefix[i] <= upper) k += 1
            Drill.compare(i, k)
            total += k - j
        }
        var a = lo; var b = mid; var out = lo
        while (a < mid || b < hi) {
            if (b >= hi || (a < mid && prefix[a] <= prefix[b])) { buffer[out] = prefix[a]; a += 1 } else { buffer[out] = prefix[b]; b += 1 }
            out += 1
        }
        for (t in lo until hi) { prefix[t] = buffer[t]; Drill.write(t, prefix[t].toInt()) }
        return total
    }
    return count(0, n + 1)
}
""",
    mutants=[
        ("int-prefix--overflows", "MISSING_EDGE_CASE",
         "누적합을 Int 로 둔다. 원소가 10억이면 셋만 더해도 넘친다.",
         """
fun countRangeSums(nums: IntArray, lower: Int, upper: Int): Int {
    val n = nums.size
    val prefix = IntArray(n + 1)
    for (i in 0 until n) prefix[i + 1] = prefix[i] + nums[i]
    val buffer = IntArray(n + 1)
    fun count(lo: Int, hi: Int): Int {
        if (hi - lo <= 1) return 0
        val mid = (lo + hi) / 2
        var total = count(lo, mid) + count(mid, hi)
        var j = mid; var k = mid
        for (i in lo until mid) {
            while (j < hi && prefix[j] - prefix[i] < lower) j += 1
            while (k < hi && prefix[k] - prefix[i] <= upper) k += 1
            total += k - j
        }
        var a = lo; var b = mid; var out = lo
        while (a < mid || b < hi) {
            if (b >= hi || (a < mid && prefix[a] <= prefix[b])) { buffer[out] = prefix[a]; a += 1 } else { buffer[out] = prefix[b]; b += 1 }
            out += 1
        }
        for (t in lo until hi) prefix[t] = buffer[t]
        return total
    }
    return count(0, n + 1)
}
"""),
        ("strict-upper-bound", "OFF_BY_ONE",
         "합이 upper 와 같은 구간을 세지 않는다. 이하인데 미만으로 본다.",
         """
fun countRangeSums(nums: IntArray, lower: Int, upper: Int): Int {
    val n = nums.size
    val prefix = LongArray(n + 1)
    for (i in 0 until n) prefix[i + 1] = prefix[i] + nums[i]
    val buffer = LongArray(n + 1)
    fun count(lo: Int, hi: Int): Int {
        if (hi - lo <= 1) return 0
        val mid = (lo + hi) / 2
        var total = count(lo, mid) + count(mid, hi)
        var j = mid; var k = mid
        for (i in lo until mid) {
            while (j < hi && prefix[j] - prefix[i] < lower) j += 1
            while (k < hi && prefix[k] - prefix[i] < upper) k += 1
            total += k - j
        }
        var a = lo; var b = mid; var out = lo
        while (a < mid || b < hi) {
            if (b >= hi || (a < mid && prefix[a] <= prefix[b])) { buffer[out] = prefix[a]; a += 1 } else { buffer[out] = prefix[b]; b += 1 }
            out += 1
        }
        for (t in lo until hi) prefix[t] = buffer[t]
        return total
    }
    return count(0, n + 1)
}
"""),
        ("skips-length-one", "OFF_BY_ONE",
         "누적합 배열의 0 번을 빼고 세어 원소 하나짜리 구간을 놓친다.",
         """
fun countRangeSums(nums: IntArray, lower: Int, upper: Int): Int {
    val n = nums.size
    val prefix = LongArray(n + 1)
    for (i in 0 until n) prefix[i + 1] = prefix[i] + nums[i]
    val buffer = LongArray(n + 1)
    fun count(lo: Int, hi: Int): Int {
        if (hi - lo <= 1) return 0
        val mid = (lo + hi) / 2
        var total = count(lo, mid) + count(mid, hi)
        var j = mid; var k = mid
        for (i in lo until mid) {
            while (j < hi && prefix[j] - prefix[i] < lower) j += 1
            while (k < hi && prefix[k] - prefix[i] <= upper) k += 1
            total += k - j
        }
        var a = lo; var b = mid; var out = lo
        while (a < mid || b < hi) {
            if (b >= hi || (a < mid && prefix[a] <= prefix[b])) { buffer[out] = prefix[a]; a += 1 } else { buffer[out] = prefix[b]; b += 1 }
            out += 1
        }
        for (t in lo until hi) prefix[t] = buffer[t]
        return total
    }
    return count(1, n + 1)
}
"""),
        ("all-pairs--quadratic", "PERFORMANCE",
         "누적합의 모든 쌍을 본다. O(n²).",
         """
fun countRangeSums(nums: IntArray, lower: Int, upper: Int): Int {
    val n = nums.size
    val prefix = LongArray(n + 1)
    for (i in 0 until n) prefix[i + 1] = prefix[i] + nums[i]
    var total = 0
    for (i in 0..n) {
        for (j in i + 1..n) {
            Drill.compare(i, j)
            val sum = prefix[j] - prefix[i]
            if (sum >= lower && sum <= upper) total += 1
        }
    }
    return total
}
"""),
    ],
))


# --- 129. k 번째로 작은 쌍의 거리 (이분 탐색 + 두 포인터) --------------------------------------------

def _kth_pair_distance(nums, k):
    values = sorted(nums)
    n = len(values)

    def pairs_within(limit):
        count = 0
        left = 0
        for right in range(n):
            while values[right] - values[left] > limit:
                left += 1
            count += right - left
        return count

    lo, hi = 0, values[-1] - values[0]
    while lo < hi:
        mid = (lo + hi) // 2
        if pairs_within(mid) >= k:
            hi = mid
        else:
            lo = mid + 1
    return lo


PROBLEMS.append(Problem(
    id="kth-smallest-pair-distance",
    title="k 번째로 작은 쌍의 거리",
    summary="""
정수 배열 `nums` 와 `k` 가 주어진다. 서로 다른 두 자리 `i < j` 의 **거리**를 `|nums[i] - nums[j]|`
라 한다. 모든 쌍의 거리를 작은 것부터 늘어놓았을 때 `k` 번째(1 부터)를 반환한다.
""",
    notes="""
쌍은 최대 5천만 개라 다 만들 수 없고 정렬은 더 못 한다 — 제약이 접근을 정한다. 답을 `d` 라
두면 "거리가 `d` 이하인 쌍의 수"는 `d` 가 커질수록 늘어난다. 그러니 `d` 를 이분 탐색하고,
정해진 `d` 에 대해 쌍의 수를 정렬된 배열 위에서 두 포인터로 O(n) 에 센다.
""",
    drill_doc="""
Drill.compare(lo, hi)         // 거리 후보의 범위를 좁혔다
Drill.pointer("left", i)      // 두 포인터의 왼쪽을 옮겼다
""",
    constraints="""
- `2 <= nums.length <= 20_000`
- `0 <= nums[i] <= 1_000_000`
- `1 <= k <= n·(n-1)/2`
""",
    signature=dict(name="kthPairDistance", parameters=[("nums", "INT_ARRAY"), ("k", "INT")], returns="INT"),
    # 이중 반복 오답이 동기 JIT(-Xbatch) 아래서 2.8배에 그쳤다 — 한도를 조여 자릿수로.
    groups=perf_groups(time_multiplier=0.25),
    # 공개 뒤 한도를 조였다 — 판정이 바뀌므로 새 버전이다 (§6.1 공개 후 불변).
    version=2,
    reference=_kth_pair_distance,
    cases={
        "sample": [("01", [[1, 3, 1], 1]), ("02", [[1, 6, 1], 3])],
        "boundary": [
            ("01-two", [[5, 9], 1]),
            ("02-all-equal", [[7, 7, 7, 7], 6]),
            # 거리 0 인 쌍이 있어도 k 가 크면 답은 0 이 아니다.
            ("03-past-zeros", [[1, 1, 1, 4], 4]),
            ("04-last-pair", [[1, 5, 9, 14], 6]),
            # 같은 거리의 쌍이 여럿일 때 k 가 그 사이에 있다.
            ("05-ties", [[1, 2, 3, 4], 3]),
            ("06-unsorted", [[9, 1, 5, 3], 2]),
        ],
        "hidden": [
            ("01-random-small", [randoms(10, 0, 20, salt=8441), 17]),
            ("02-random-medium", [randoms(80, 0, 1000, salt=8442), 1500]),
            ("03-clustered", [randoms(60, 100, 103, salt=8443), 1000]),
            ("04-wide", [randoms(50, 0, 1000000, salt=8444), 600]),
        ],
        "performance": [
            ("01-small", [randoms(1000, 0, 1000000, salt=8451), 200000]),
            ("02-medium", [randoms(4000, 0, 1000000, salt=8452), 5000000]),
            ("03-large", [randoms(20000, 0, 1000000, salt=8453), 100000000]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 거리를 이분 탐색하고, 거리마다 두 포인터로 쌍을 센다.
fun kthPairDistance(nums: IntArray, k: Int): Int {
    val values = nums.sortedArray()
    val n = values.size
    fun pairsWithin(limit: Int): Long {
        var count = 0L
        var left = 0
        for (right in 0 until n) {
            while (values[right] - values[left] > limit) { left += 1; Drill.pointer("left", left) }
            count += right - left
        }
        return count
    }
    var lo = 0
    var hi = values[n - 1] - values[0]
    while (lo < hi) {
        val mid = (lo + hi) / 2
        Drill.compare(lo, hi)
        if (pairsWithin(mid) >= k) hi = mid else lo = mid + 1
    }
    return lo
}
""",
    mutants=[
        ("counts-strictly-less", "OFF_BY_ONE",
         "거리가 한도 '미만'인 쌍을 센다. 이하여야 한다 — 답이 정확히 한도인 쌍을 놓친다.",
         """
fun kthPairDistance(nums: IntArray, k: Int): Int {
    val values = nums.sortedArray()
    val n = values.size
    fun pairsWithin(limit: Int): Long {
        var count = 0L; var left = 0
        for (right in 0 until n) {
            while (values[right] - values[left] >= limit) left += 1
            count += right - left
        }
        return count
    }
    var lo = 0; var hi = values[n - 1] - values[0]
    while (lo < hi) { val mid = (lo + hi) / 2; if (pairsWithin(mid) >= k) hi = mid else lo = mid + 1 }
    return lo
}
"""),
        ("forgets-to-sort", "MISSING_EDGE_CASE",
         "정렬하지 않고 두 포인터를 돌린다. 이미 정렬된 입력에서만 맞는다.",
         """
fun kthPairDistance(nums: IntArray, k: Int): Int {
    val values = nums
    val n = values.size
    fun pairsWithin(limit: Int): Long {
        var count = 0L; var left = 0
        for (right in 0 until n) {
            while (left < right && kotlin.math.abs(values[right] - values[left]) > limit) left += 1
            count += right - left
        }
        return count
    }
    var lo = 0; var hi = values.max() - values.min()
    while (lo < hi) { val mid = (lo + hi) / 2; if (pairsWithin(mid) >= k) hi = mid else lo = mid + 1 }
    return lo
}
"""),
        ("upper-bound-too-small", "WRONG_BRANCH",
         "이분 탐색의 위 경계를 최댓값의 절반으로 둔다. 답이 그 위에 있으면 경계에서 멈춘다.",
         """
fun kthPairDistance(nums: IntArray, k: Int): Int {
    val values = nums.sortedArray()
    val n = values.size
    fun pairsWithin(limit: Int): Long {
        var count = 0L; var left = 0
        for (right in 0 until n) {
            while (values[right] - values[left] > limit) left += 1
            count += right - left
        }
        return count
    }
    var lo = 0; var hi = values[n - 1] / 2
    while (lo < hi) { val mid = (lo + hi) / 2; if (pairsWithin(mid) >= k) hi = mid else lo = mid + 1 }
    return lo
}
"""),
        ("count-pairs-by-double-loop", "PERFORMANCE",
         "거리를 이분 탐색하되 쌍을 셀 때 정렬도 두 포인터도 없이 모든 쌍을 본다. O(n² log W).",
         """
fun kthPairDistance(nums: IntArray, k: Int): Int {
    val n = nums.size
    fun pairsWithin(limit: Int): Long {
        var count = 0L
        for (i in 0 until n) for (j in i + 1 until n) { Drill.compare(i, j); if (kotlin.math.abs(nums[i] - nums[j]) <= limit) count += 1 }
        return count
    }
    var lo = 0; var hi = nums.max() - nums.min()
    while (lo < hi) { val mid = (lo + hi) / 2; if (pairsWithin(mid) >= k) hi = mid else lo = mid + 1 }
    return lo
}
"""),
    ],
))


# --- 134. 뒤에 있는 더 작은 수의 개수 (병합 정렬로 세기) -------------------------------------------------

def _count_smaller_after(nums):
    n = len(nums)
    counts = [0] * n
    order = list(range(n))

    def sort(lo, hi):
        if hi - lo <= 1:
            return
        mid = (lo + hi) // 2
        sort(lo, mid)
        sort(mid, hi)
        merged = []
        i, j = lo, mid
        while i < mid or j < hi:
            if j >= hi or (i < mid and nums[order[i]] <= nums[order[j]]):
                counts[order[i]] += j - mid
                merged.append(order[i])
                i += 1
            else:
                merged.append(order[j])
                j += 1
        order[lo:hi] = merged

    sort(0, n)
    return counts


PROBLEMS.append(Problem(
    id="count-smaller-after-self",
    title="뒤에 있는 더 작은 수의 개수",
    summary="""
정수 배열 `nums` 가 주어진다. 각 자리 `i` 에 대해 `i` 보다 **뒤에** 있으면서 `nums[i]` 보다
**작은** 원소의 개수를 담은 배열을 반환한다.
""",
    notes="""
자리마다 뒤를 훑으면 n² 이다. 역전 쌍 세기의 뼈대다 — 병합 정렬을 하되 값이 아니라 **자리**를
정렬하고, 병합할 때 왼쪽 원소가 나가는 순간 오른쪽에서 이미 나간 원소의 수가 곧 "뒤에 있는
더 작은 수"다. 같은 값은 작지 않으니 왼쪽을 먼저 내보낸다.
""",
    drill_doc="""
Drill.compare(i, j)           // 왼쪽과 오른쪽 원소를 비교했다
Drill.write(index, count)     // 자리의 답을 늘렸다
""",
    constraints="""
- `1 <= nums.length <= 100_000`
- `-10^9 <= nums[i] <= 10^9`
""",
    signature=dict(name="countSmallerAfter", parameters=[("nums", "INT_ARRAY")], returns="INT_ARRAY"),
    # n² 훑기가 한도의 1.1배에 그쳤다 — 비교 50억 번이 JIT 아래서 1초다. 자릿수로 지게 한다.
    groups=perf_groups(time_multiplier=0.15),
    reference=_count_smaller_after,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 2000000},
    cases={
        "sample": [("01", [[5, 2, 6, 1]]), ("02", [[3, 3, 1]])],
        "boundary": [
            ("01-single", [[7]]),
            # 같은 값은 작지 않다.
            ("02-equal-values", [[2, 2, 2]]),
            ("03-descending", [[4, 3, 2, 1]]),
            ("04-ascending", [[1, 2, 3, 4]]),
            ("05-negative", [[-1, -3, -2]]),
            # 병합 경계에서 안정성이 필요하다 — 같은 값이 양쪽에 있다.
            ("06-equal-across-halves", [[1, 3, 1, 3]]),
        ],
        "hidden": [
            ("01-random-small", [randoms(12, -5, 5, salt=8601)]),
            ("02-random-medium", [randoms(300, -100, 100, salt=8602)]),
            ("03-random-wide", [randoms(500, -1000000000, 1000000000, salt=8603)]),
            ("04-many-duplicates", [randoms(400, 0, 3, salt=8604)]),
        ],
        "performance": [
            ("01-small", [randoms(5000, -100000, 100000, salt=8611)]),
            ("02-medium", [randoms(30000, -100000, 100000, salt=8612)]),
            ("03-large", [list(range(100000, 0, -1))]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 자리를 병합 정렬하며 왼쪽이 나갈 때 오른쪽에서 먼저 나간 수를 더한다.
fun countSmallerAfter(nums: IntArray): IntArray {
    val n = nums.size
    val counts = IntArray(n)
    var order = IntArray(n) { it }
    var buffer = IntArray(n)
    fun sort(lo: Int, hi: Int) {
        if (hi - lo <= 1) return
        val mid = (lo + hi) / 2
        sort(lo, mid); sort(mid, hi)
        var i = lo; var j = mid; var out = lo
        while (i < mid || j < hi) {
            if (j >= hi || (i < mid && nums[order[i]] <= nums[order[j]])) {
                Drill.compare(order[i], if (j < hi) order[j] else order[i])
                counts[order[i]] += j - mid
                Drill.write(order[i], counts[order[i]])
                buffer[out++] = order[i++]
            } else {
                buffer[out++] = order[j++]
            }
        }
        for (t in lo until hi) order[t] = buffer[t]
    }
    sort(0, n)
    return counts
}
""",
    mutants=[
        ("strict-merge--counts-equal-as-smaller", "OFF_BY_ONE",
         "병합에서 같은 값일 때 오른쪽을 먼저 내보낸다. 같은 값이 '더 작은 수'로 세어진다.",
         """
fun countSmallerAfter(nums: IntArray): IntArray {
    val n = nums.size
    val counts = IntArray(n)
    val order = IntArray(n) { it }
    val buffer = IntArray(n)
    fun sort(lo: Int, hi: Int) {
        if (hi - lo <= 1) return
        val mid = (lo + hi) / 2
        sort(lo, mid); sort(mid, hi)
        var i = lo; var j = mid; var out = lo
        while (i < mid || j < hi) {
            if (j >= hi || (i < mid && nums[order[i]] < nums[order[j]])) { counts[order[i]] += j - mid; buffer[out++] = order[i++] } else buffer[out++] = order[j++]
        }
        for (t in lo until hi) order[t] = buffer[t]
    }
    sort(0, n)
    return counts
}
"""),
        ("counts-before-instead-of-after", "WRONG_ALGORITHM",
         "앞에 있는 더 작은 수를 센다.",
         """
fun countSmallerAfter(nums: IntArray): IntArray {
    val n = nums.size
    val counts = IntArray(n)
    val order = IntArray(n) { it }
    val buffer = IntArray(n)
    fun sort(lo: Int, hi: Int) {
        if (hi - lo <= 1) return
        val mid = (lo + hi) / 2
        sort(lo, mid); sort(mid, hi)
        var i = lo; var j = mid; var out = lo
        while (i < mid || j < hi) {
            if (i >= mid || (j < hi && nums[order[j]] < nums[order[i]])) { counts[order[j]] += i - lo; buffer[out++] = order[j++] } else buffer[out++] = order[i++]
        }
        for (t in lo until hi) order[t] = buffer[t]
    }
    sort(0, n)
    return counts
}
"""),
        ("sorts-values--loses-positions", "WRONG_BRANCH",
         "값을 정렬하고 자리를 잃는다. 답이 원래 자리가 아니라 정렬된 자리에 적힌다.",
         """
fun countSmallerAfter(nums: IntArray): IntArray {
    val n = nums.size
    val values = nums.copyOf()
    val counts = IntArray(n)
    val buffer = IntArray(n)
    val cbuf = IntArray(n)
    fun sort(lo: Int, hi: Int) {
        if (hi - lo <= 1) return
        val mid = (lo + hi) / 2
        sort(lo, mid); sort(mid, hi)
        var i = lo; var j = mid; var out = lo
        while (i < mid || j < hi) {
            if (j >= hi || (i < mid && values[i] <= values[j])) { cbuf[out] = counts[i] + (j - mid); buffer[out++] = values[i++] } else { cbuf[out] = counts[j]; buffer[out++] = values[j++] }
        }
        for (t in lo until hi) { values[t] = buffer[t]; counts[t] = cbuf[t] }
    }
    sort(0, n)
    return counts
}
"""),
        ("scan-right--quadratic", "PERFORMANCE",
         "자리마다 뒤를 훑는다. O(n²).",
         """
fun countSmallerAfter(nums: IntArray): IntArray {
    val n = nums.size
    val counts = IntArray(n)
    for (i in 0 until n) {
        var c = 0
        for (j in i + 1 until n) { Drill.compare(i, j); if (nums[j] < nums[i]) c += 1 }
        counts[i] = c
    }
    return counts
}
"""),
    ],
))


# --- 135. 가장 가까운 두 점의 거리 제곱 (분할 정복) ----------------------------------------------------

def _closest_pair(points):
    pts = sorted((points[i], points[i + 1]) for i in range(0, len(points), 2))

    def solve(lo, hi):
        if hi - lo <= 3:
            best = None
            for i in range(lo, hi):
                for j in range(i + 1, hi):
                    d = (pts[i][0] - pts[j][0]) ** 2 + (pts[i][1] - pts[j][1]) ** 2
                    if best is None or d < best:
                        best = d
            return best if best is not None else float("inf")
        mid = (lo + hi) // 2
        mid_x = pts[mid][0]
        best = min(solve(lo, mid), solve(mid, hi))
        strip = [p for p in pts[lo:hi] if (p[0] - mid_x) ** 2 < best]
        strip.sort(key=lambda p: p[1])
        for i in range(len(strip)):
            j = i + 1
            while j < len(strip) and (strip[j][1] - strip[i][1]) ** 2 < best:
                d = (strip[i][0] - strip[j][0]) ** 2 + (strip[i][1] - strip[j][1]) ** 2
                if d < best:
                    best = d
                j += 1
        return best

    return int(solve(0, len(pts)))


def _points(n, span, salt):
    xs = randoms(n, -span, span, salt=salt)
    ys = randoms(n, -span, span, salt=salt + 1)
    return flat([xs[i], ys[i]] for i in range(n))


PROBLEMS.append(Problem(
    id="closest-pair-squared",
    title="가장 가까운 두 점",
    summary="""
평면의 점들이 `[x1, y1, x2, y2, ...]` 로 주어진다. 서로 다른 두 점 사이의 **거리의 제곱**
`(x1-x2)² + (y1-y2)²` 중 가장 작은 값을 반환한다. 같은 자리에 점이 둘 있으면 `0` 이다.
""",
    notes="""
모든 쌍은 n² 이다. x 로 정렬해 절반으로 나누고, 양쪽의 답 `d` 를 구한 뒤 경계선에서 `d` 안에
있는 점들(띠)만 다시 본다. 띠를 y 로 정렬하면 각 점은 y 차가 `d` 를 넘기 전까지 — 많아야
상수 개 — 만 비교하면 된다. 그것이 O(n log² n) 이고, 이 제약에서 충분하다.
""",
    drill_doc="""
Drill.compare(i, j)           // 두 점의 거리를 봤다
Drill.write(0, best)          // 답을 줄였다
""",
    constraints="""
- `2 <= 점의 수 <= 100_000`, `points.size` 는 짝수
- `-30_000 <= x, y <= 30_000` (거리 제곱은 `Int` 안이다)
""",
    signature=dict(name="closestPairSquared", parameters=[("points", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(time_multiplier=0.5),
    reference=_closest_pair,
    cases={
        "sample": [("01", [[0, 0, 3, 4, 1, 1]]), ("02", [[0, 0, 5, 5]])],
        "boundary": [
            ("01-two-points", [[-3, 2, 4, -2]]),
            ("02-duplicate-point", [[1, 1, 5, 5, 1, 1]]),
            # 가장 가까운 두 점이 경계선 양쪽에 있다.
            ("03-across-split", [[-10, 0, -1, 0, 1, 0, 10, 0]]),
            # x 는 같고 y 만 다르다 — 정렬이 x 만 보면 띠가 전부다.
            ("04-vertical-line", [[0, 0, 0, 7, 0, 3, 0, 10]]),
            ("05-three-collinear", [[0, 0, 100, 0, 51, 0]]),
            # 띠에서 y 차가 큰 점은 볼 필요가 없지만, 보는 범위를 너무 좁히면 놓친다.
            ("06-strip-diagonal", [[-2, 0, 2, 3, 0, 100, 0, -100]]),
            # 띠를 y 로 늘어놓으면 답인 두 점 사이에 두 점이 끼어 있다 — "다음 둘만" 보면 놓친다.
            ("07-strip-partner-third", [[-6, -4, -1, -6, -1, -4, 4, -5]]),
        ],
        "hidden": [
            ("01-random-small", [_points(10, 20, salt=8621)]),
            ("02-random-medium", [_points(200, 1000, salt=8623)]),
            ("03-random-large", [_points(2000, 30000, salt=8625)]),
            ("04-grid-points", [flat([x * 7, y * 5] for x in range(20) for y in range(20))]),
        ],
        "performance": [
            ("01-small", [_points(5000, 30000, salt=8631)]),
            ("02-medium", [_points(30000, 30000, salt=8633)]),
            ("03-large", [_points(100000, 30000, salt=8635)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). x 정렬 → 분할 → 띠에서 y 순으로 이웃만.
fun closestPairSquared(points: IntArray): Int {
    val n = points.size / 2
    val order = (0 until n).sortedWith(compareBy({ points[it * 2] }, { points[it * 2 + 1] })).toIntArray()
    val xs = IntArray(n) { points[order[it] * 2] }
    val ys = IntArray(n) { points[order[it] * 2 + 1] }
    fun dist(i: Int, j: Int): Long {
        val dx = (xs[i] - xs[j]).toLong(); val dy = (ys[i] - ys[j]).toLong()
        return dx * dx + dy * dy
    }
    val strip = IntArray(n)
    fun solve(lo: Int, hi: Int): Long {
        if (hi - lo <= 3) {
            var best = Long.MAX_VALUE
            for (i in lo until hi) for (j in i + 1 until hi) { Drill.compare(i, j); best = minOf(best, dist(i, j)) }
            return best
        }
        val mid = (lo + hi) / 2
        val midX = xs[mid]
        var best = minOf(solve(lo, mid), solve(mid, hi))
        var m = 0
        for (i in lo until hi) {
            val dx = (xs[i] - midX).toLong()
            if (dx * dx < best) strip[m++] = i
        }
        val band = strip.copyOfRange(0, m).sortedBy { ys[it] }
        for (a in band.indices) {
            var b = a + 1
            while (b < band.size) {
                val dy = (ys[band[b]] - ys[band[a]]).toLong()
                if (dy * dy >= best) break
                Drill.compare(band[a], band[b])
                val d = dist(band[a], band[b])
                if (d < best) { best = d; Drill.write(0, best.toInt()) }
                b += 1
            }
        }
        return best
    }
    return solve(0, n).toInt()
}
""",
    mutants=[
        # "다음 셋"은 열린 띠와 같은 편 간격 때문에 실제로 놓치는 입력이 없었다 — 동치 오답이라 둘로 줄였다.
        ("strip-checks-only-next-two", "MISSING_EDGE_CASE",
         "띠에서 이웃을 y 차와 무관하게 다음 둘만 본다. 답인 두 점 사이에 둘이 끼면 놓친다.",
         """
fun closestPairSquared(points: IntArray): Int {
    val n = points.size / 2
    val order = (0 until n).sortedWith(compareBy({ points[it * 2] }, { points[it * 2 + 1] })).toIntArray()
    val xs = IntArray(n) { points[order[it] * 2] }
    val ys = IntArray(n) { points[order[it] * 2 + 1] }
    fun dist(i: Int, j: Int): Long { val dx = (xs[i] - xs[j]).toLong(); val dy = (ys[i] - ys[j]).toLong(); return dx * dx + dy * dy }
    fun solve(lo: Int, hi: Int): Long {
        if (hi - lo <= 3) { var best = Long.MAX_VALUE; for (i in lo until hi) for (j in i + 1 until hi) best = minOf(best, dist(i, j)); return best }
        val mid = (lo + hi) / 2; val midX = xs[mid]
        var best = minOf(solve(lo, mid), solve(mid, hi))
        val band = (lo until hi).filter { val dx = (xs[it] - midX).toLong(); dx * dx < best }.sortedBy { ys[it] }
        for (a in band.indices) for (b in a + 1 until minOf(band.size, a + 3)) best = minOf(best, dist(band[a], band[b]))
        return best
    }
    return solve(0, n).toInt()
}
"""),
        ("ignores-cross-pairs", "WRONG_ALGORITHM",
         "양쪽의 답만 보고 경계를 가로지르는 쌍을 보지 않는다.",
         """
fun closestPairSquared(points: IntArray): Int {
    val n = points.size / 2
    val order = (0 until n).sortedBy { points[it * 2] }.toIntArray()
    val xs = IntArray(n) { points[order[it] * 2] }
    val ys = IntArray(n) { points[order[it] * 2 + 1] }
    fun dist(i: Int, j: Int): Long { val dx = (xs[i] - xs[j]).toLong(); val dy = (ys[i] - ys[j]).toLong(); return dx * dx + dy * dy }
    fun solve(lo: Int, hi: Int): Long {
        if (hi - lo <= 3) { var best = Long.MAX_VALUE; for (i in lo until hi) for (j in i + 1 until hi) best = minOf(best, dist(i, j)); return best }
        val mid = (lo + hi) / 2
        return minOf(solve(lo, mid), solve(mid, hi))
    }
    return solve(0, n).toInt()
}
"""),
        ("int-distance--overflow-free-but-manhattan", "WRONG_BRANCH",
         "거리를 |dx| + |dy| 로 잰다. 축에 나란한 쌍에서만 맞는다.",
         """
fun closestPairSquared(points: IntArray): Int {
    val n = points.size / 2
    var best = Long.MAX_VALUE
    var bestSq = Long.MAX_VALUE
    for (i in 0 until n) for (j in i + 1 until n) {
        val dx = (points[i * 2] - points[j * 2]).toLong(); val dy = (points[i * 2 + 1] - points[j * 2 + 1]).toLong()
        val manhattan = kotlin.math.abs(dx) + kotlin.math.abs(dy)
        if (manhattan < best) { best = manhattan; bestSq = dx * dx + dy * dy }
    }
    return bestSq.toInt()
}
"""),
        ("all-pairs--quadratic", "PERFORMANCE",
         "모든 쌍을 본다. O(n²).",
         """
fun closestPairSquared(points: IntArray): Int {
    val n = points.size / 2
    var best = Long.MAX_VALUE
    for (i in 0 until n) for (j in i + 1 until n) {
        Drill.compare(i, j)
        val dx = (points[i * 2] - points[j * 2]).toLong(); val dy = (points[i * 2 + 1] - points[j * 2 + 1]).toLong()
        val d = dx * dx + dy * dy
        if (d < best) best = d
    }
    return best.toInt()
}
"""),
    ],
))


# --- 136. 구간에서 값의 빈도 (자리 목록 위 이분 탐색) --------------------------------------------------

def _range_frequency(nums, queries):
    import bisect
    positions = {}
    for i, x in enumerate(nums):
        positions.setdefault(x, []).append(i)
    out = []
    for q in range(0, len(queries), 3):
        left, right, value = queries[q], queries[q + 1], queries[q + 2]
        pos = positions.get(value)
        if not pos:
            out.append(0)
            continue
        out.append(bisect.bisect_right(pos, right) - bisect.bisect_left(pos, left))
    return out


def _queries(n, count, salt):
    a = randoms(count, 0, n - 1, salt=salt)
    b = randoms(count, 0, n - 1, salt=salt + 1)
    v = randoms(count, 0, 20, salt=salt + 2)
    return flat([min(a[i], b[i]), max(a[i], b[i]), v[i]] for i in range(count))


PROBLEMS.append(Problem(
    id="range-frequency-queries",
    title="구간에서 값의 빈도",
    summary="""
정수 배열 `nums` 와 질의들이 주어진다. `queries` 는 `[l1, r1, v1, l2, r2, v2, ...]` 이며 질의
하나는 "`nums[l..r]` (양 끝 포함) 에 `v` 가 몇 번 나오는가"다. 질의마다의 답을 순서대로 담은
배열을 반환한다.
""",
    notes="""
질의마다 구간을 훑으면 질의 수 × 구간 길이다. 값마다 **그 값이 나오는 자리들의 정렬된 목록**을
만들어 두면, 질의는 그 목록에서 `l` 이상인 첫 자리와 `r` 초과인 첫 자리를 이분 탐색으로 찾아
빼는 것이다. 없는 값은 0 이다.
""",
    drill_doc="""
Drill.compare(lo, hi)         // 이분 탐색의 범위를 좁혔다
Drill.write(q, answer)        // 질의의 답을 적었다
""",
    constraints="""
- `1 <= nums.length <= 100_000`, `0 <= nums[i] <= 10^9`
- `queries.size` 는 3 의 배수이며 질의는 최대 `100_000` 개, `0 <= l <= r < n`
""",
    signature=dict(name="rangeFrequency", parameters=[("nums", "INT_ARRAY"), ("queries", "INT_ARRAY")], returns="INT_ARRAY"),
    # 공개 뒤 한도(케이스)를 고쳤다 — 판정이 바뀌므로 새 버전이다 (§6.1 공개 후 불변).
    version=2,
    # 질의마다 훑기가 전체 검증에서 2.7배에 그쳤다 — 값이 21 가지뿐이라 구간이 짧은 질의가 많다.
    groups=perf_groups(time_multiplier=0.25),
    reference=_range_frequency,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 2000000},
    cases={
        "sample": [("01", [[1, 2, 1, 1, 3, 1], [0, 5, 1, 1, 3, 1, 2, 2, 5]]), ("02", [[7], [0, 0, 7, 0, 0, 8]])],
        "boundary": [
            ("01-single-query-whole", [[3, 3, 3], [0, 2, 3]]),
            # 양 끝이 포함이다.
            ("02-inclusive-ends", [[5, 1, 5], [0, 0, 5, 2, 2, 5, 0, 2, 5]]),
            ("03-missing-value", [[1, 2, 3], [0, 2, 9]]),
            # 값은 있는데 구간 밖이다.
            ("04-value-outside-range", [[4, 0, 0, 4], [1, 2, 4]]),
            ("05-same-position", [[2, 2], [1, 1, 2]]),
            ("06-no-queries", [[1, 2], []]),
        ],
        "hidden": [
            ("01-random-small", [randoms(12, 0, 3, salt=8641), _queries(12, 8, salt=8642)]),
            ("02-random-medium", [randoms(300, 0, 20, salt=8645), _queries(300, 100, salt=8646)]),
            ("03-large-values", [randoms(100, 0, 1000000000, salt=8649), [0, 99, 1000000000, 0, 99, 0]]),
            ("04-all-same", [[9] * 200, [0, 199, 9, 50, 60, 9, 0, 0, 9]]),
        ],
        "performance": [
            ("01-small", [randoms(5000, 0, 20, salt=8651), _queries(5000, 5000, salt=8652)]),
            ("02-medium", [randoms(30000, 0, 20, salt=8655), _queries(30000, 30000, salt=8656)]),
            ("03-large", [randoms(100000, 0, 20, salt=8659), _queries(100000, 100000, salt=8660)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 값마다 자리 목록, 질의마다 이분 탐색 둘.
fun rangeFrequency(nums: IntArray, queries: IntArray): IntArray {
    val positions = HashMap<Int, MutableList<Int>>()
    for (i in nums.indices) positions.getOrPut(nums[i]) { ArrayList() }.add(i)
    fun lowerBound(list: List<Int>, target: Int): Int {
        var lo = 0; var hi = list.size
        while (lo < hi) { val mid = (lo + hi) / 2; Drill.compare(lo, hi); if (list[mid] < target) lo = mid + 1 else hi = mid }
        return lo
    }
    val out = IntArray(queries.size / 3)
    for (q in out.indices) {
        val left = queries[q * 3]; val right = queries[q * 3 + 1]; val value = queries[q * 3 + 2]
        val list = positions[value]
        out[q] = if (list == null) 0 else lowerBound(list, right + 1) - lowerBound(list, left)
        Drill.write(q, out[q])
    }
    return out
}
""",
    mutants=[
        ("exclusive-right-end", "OFF_BY_ONE",
         "오른쪽 끝을 빼고 센다. 양 끝 포함이다.",
         """
fun rangeFrequency(nums: IntArray, queries: IntArray): IntArray {
    val positions = HashMap<Int, MutableList<Int>>()
    for (i in nums.indices) positions.getOrPut(nums[i]) { ArrayList() }.add(i)
    fun lowerBound(list: List<Int>, target: Int): Int { var lo = 0; var hi = list.size; while (lo < hi) { val mid = (lo + hi) / 2; if (list[mid] < target) lo = mid + 1 else hi = mid }; return lo }
    val out = IntArray(queries.size / 3)
    for (q in out.indices) {
        val list = positions[queries[q * 3 + 2]]
        out[q] = if (list == null) 0 else lowerBound(list, queries[q * 3 + 1]) - lowerBound(list, queries[q * 3])
    }
    return out
}
"""),
        ("upper-bound-as-lower", "WRONG_BRANCH",
         "왼쪽 끝을 초과로 찾는다. 왼쪽 끝 자리의 값을 놓친다.",
         """
fun rangeFrequency(nums: IntArray, queries: IntArray): IntArray {
    val positions = HashMap<Int, MutableList<Int>>()
    for (i in nums.indices) positions.getOrPut(nums[i]) { ArrayList() }.add(i)
    fun upperBound(list: List<Int>, target: Int): Int { var lo = 0; var hi = list.size; while (lo < hi) { val mid = (lo + hi) / 2; if (list[mid] <= target) lo = mid + 1 else hi = mid }; return lo }
    val out = IntArray(queries.size / 3)
    for (q in out.indices) {
        val list = positions[queries[q * 3 + 2]]
        out[q] = if (list == null) 0 else upperBound(list, queries[q * 3 + 1]) - upperBound(list, queries[q * 3])
    }
    return out
}
"""),
        ("scan-per-query", "PERFORMANCE",
         "질의마다 구간을 훑는다. O(질의 × 구간 길이).",
         """
fun rangeFrequency(nums: IntArray, queries: IntArray): IntArray {
    val out = IntArray(queries.size / 3)
    for (q in out.indices) {
        var c = 0
        for (i in queries[q * 3]..queries[q * 3 + 1]) { Drill.compare(i, q); if (nums[i] == queries[q * 3 + 2]) c += 1 }
        out[q] = c
    }
    return out
}
"""),
    ],
))


# --- 190. 건물의 윤곽선 (분할 정복) -------------------------------------------

def _skyline(buildings):
    count = len(buildings) // 3

    def merge(left, right):
        out = []
        i = j = 0
        hl = hr = 0
        while i < len(left) and j < len(right):
            if left[i][0] < right[j][0]:
                x, hl = left[i]
                i += 1
            elif left[i][0] > right[j][0]:
                x, hr = right[j]
                j += 1
            else:
                x = left[i][0]
                hl = left[i][1]
                hr = right[j][1]
                i += 1
                j += 1
            height = max(hl, hr)
            if not out or out[-1][1] != height:
                out.append((x, height))
        for rest in (left[i:], right[j:]):
            for x, height in rest:
                if not out or out[-1][1] != height:
                    out.append((x, height))
        return out

    def solve(lo, hi):
        if lo == hi:
            return [(buildings[3 * lo], buildings[3 * lo + 2]), (buildings[3 * lo + 1], 0)]
        mid = (lo + hi) // 2
        return merge(solve(lo, mid), solve(mid + 1, hi))

    if count == 0:
        return []
    return flat([x, h] for x, h in solve(0, count - 1))


def _random_buildings(count, span, height, salt):
    left = randoms(count, 1, span, salt=salt)
    width = randoms(count, 1, max(2, span // 50), salt=salt + 1)
    tall = randoms(count, 1, height, salt=salt + 2)
    return flat([left[i], left[i] + width[i], tall[i]] for i in range(count))


PROBLEMS.append(Problem(
    id="skyline-outline",
    # v2: 성능 그룹의 시계를 조였다. 좌표마다 훑는 오답이 CI 머신에서 한도의 2.2배에 그쳤다 (§12.1 재현성).
    version=2,
    title="건물의 윤곽선",
    summary="""
건물들이 `buildings = [l1, r1, h1, l2, r2, h2, ...]` 로 주어진다. 건물 하나는 `x` 가 `l` 이상 `r` **미만**인
구간에 높이 `h` 로 서 있다. 겹쳐 선 건물들을 멀리서 봤을 때의 **윤곽선**을 `[x1, h1, x2, h2, ...]` 로
반환한다 — 높이가 바뀌는 자리의 `x` 와 그 자리부터의 높이이고, `x` 가 커지는 순서다. 마지막 높이는 `0` 이다.

같은 높이가 이어지는 점은 넣지 않는다. 건물이 없으면 빈 배열이다.
""",
    notes="""
윤곽선 둘을 합치는 일은 **정렬된 두 목록을 훑는 것**이다 — `x` 가 작은 쪽을 꺼내 그쪽 높이를 갱신하고,
두 높이 중 큰 값이 그 자리의 높이다. 직전 높이와 같으면 점을 넣지 않는다.

합칠 수 있으면 나눌 수 있다. 건물 하나의 윤곽선은 `[(l, h), (r, 0)]` 이고, 절반씩 나눠 푼 뒤 합치면
`n log n` 이다. 좌표마다 모든 건물을 훑으면 `n²` 이다.
""",
    drill_doc="""
Drill.write(x, height)        // 윤곽선에 점을 찍었다
Drill.compare(left, right)    // 두 윤곽선의 다음 점을 견줬다
""",
    constraints="""
- 건물 `0..50_000` 개, `1 <= l < r <= 1_000_000_000`, `1 <= h <= 1_000_000_000`
""",
    signature=dict(name="skyline", parameters=[("buildings", "INT_ARRAY")], returns="INT_ARRAY"),
    # 좌표마다 건물을 훑는 오답의 안쪽은 정수 비교뿐이라 JIT 가 벡터화한다 — CI 머신에서 한도의 2.2배로
    # 떨어졌고 같은 케이스가 다른 실행에서는 3배를 넘겼다. 정답이 한도의 4% 를 쓰므로 시계를 조인다.
    groups=perf_groups(time_multiplier=0.25),
    reference=_skyline,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 16000000},
    cases={
        "sample": [
            ("01", [[1, 3, 3, 2, 4, 4]]),
            ("02", [[1, 5, 2]]),
        ],
        "boundary": [
            ("01-empty", [[]]),
            ("02-single", [[2, 9, 10]]),
            # 나란히 붙은 같은 높이 — 점이 둘이 아니라 하나다.
            ("03-touching-same-height", [[1, 3, 5, 3, 6, 5]]),
            ("04-nested", [[1, 10, 3, 3, 5, 9]]),
            ("05-identical", [[1, 4, 7, 1, 4, 7]]),
            # 한 건물이 다른 건물을 완전히 덮는다.
            ("06-covering", [[1, 10, 9, 2, 3, 4]]),
            ("07-adjacent-gap", [[1, 2, 5, 4, 6, 5]]),
            ("08-same-left", [[1, 5, 3, 1, 9, 2]]),
        ],
        "hidden": [
            ("01-random-small", [_random_buildings(8, 30, 20, salt=9861)]),
            ("02-random-medium", [_random_buildings(200, 1000, 1000, salt=9863)]),
            ("03-random-wide", [_random_buildings(500, 1000000000, 1000000000, salt=9865)]),
            ("04-staircase", [flat([i, i + 2, i] for i in range(1, 300))]),
            ("05-same-height", [flat([i, i + 3, 50] for i in range(1, 200))]),
        ],
        "performance": [
            ("01-medium", [_random_buildings(10000, 1000000, 1000000, salt=9871)]),
            ("02-large", [_random_buildings(50000, 1000000000, 1000000000, salt=9873)]),
            ("03-staircase-large", [flat([i, i + 2, 50000 - i] for i in range(1, 50000))]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 절반씩 풀고 두 윤곽선을 훑어 합친다.
fun skyline(buildings: IntArray): IntArray {
    val count = buildings.size / 3
    if (count == 0) return IntArray(0)

    fun merge(left: ArrayList<IntArray>, right: ArrayList<IntArray>): ArrayList<IntArray> {
        val out = ArrayList<IntArray>()
        var i = 0; var j = 0; var hl = 0; var hr = 0
        fun push(x: Int, h: Int) { if (out.isEmpty() || out[out.size - 1][1] != h) { out.add(intArrayOf(x, h)); Drill.write(x, h) } }
        while (i < left.size && j < right.size) {
            Drill.compare(left[i][0], right[j][0])
            val x: Int
            if (left[i][0] < right[j][0]) { x = left[i][0]; hl = left[i][1]; i += 1 }
            else if (left[i][0] > right[j][0]) { x = right[j][0]; hr = right[j][1]; j += 1 }
            else { x = left[i][0]; hl = left[i][1]; hr = right[j][1]; i += 1; j += 1 }
            push(x, if (hl > hr) hl else hr)
        }
        while (i < left.size) { push(left[i][0], left[i][1]); i += 1 }
        while (j < right.size) { push(right[j][0], right[j][1]); j += 1 }
        return out
    }

    fun solve(lo: Int, hi: Int): ArrayList<IntArray> {
        if (lo == hi) {
            val one = ArrayList<IntArray>(2)
            one.add(intArrayOf(buildings[3 * lo], buildings[3 * lo + 2]))
            one.add(intArrayOf(buildings[3 * lo + 1], 0))
            return one
        }
        val mid = (lo + hi) / 2
        return merge(solve(lo, mid), solve(mid + 1, hi))
    }

    val points = solve(0, count - 1)
    val out = IntArray(points.size * 2)
    for (k in points.indices) { out[2 * k] = points[k][0]; out[2 * k + 1] = points[k][1] }
    return out
}
""",
    mutants=[
        ("keeps-equal-heights", "MISSING_EDGE_CASE",
         "직전 높이와 같아도 점을 찍는다. 윤곽선은 높이가 바뀌는 자리만 담는다.",
         """
fun skyline(buildings: IntArray): IntArray {
    val count = buildings.size / 3
    if (count == 0) return IntArray(0)
    fun merge(left: ArrayList<IntArray>, right: ArrayList<IntArray>): ArrayList<IntArray> {
        val out = ArrayList<IntArray>()
        var i = 0; var j = 0; var hl = 0; var hr = 0
        while (i < left.size && j < right.size) {
            val x: Int
            if (left[i][0] < right[j][0]) { x = left[i][0]; hl = left[i][1]; i += 1 }
            else if (left[i][0] > right[j][0]) { x = right[j][0]; hr = right[j][1]; j += 1 }
            else { x = left[i][0]; hl = left[i][1]; hr = right[j][1]; i += 1; j += 1 }
            out.add(intArrayOf(x, if (hl > hr) hl else hr))
        }
        while (i < left.size) { out.add(left[i]); i += 1 }
        while (j < right.size) { out.add(right[j]); j += 1 }
        return out
    }
    fun solve(lo: Int, hi: Int): ArrayList<IntArray> {
        if (lo == hi) {
            val one = ArrayList<IntArray>(2)
            one.add(intArrayOf(buildings[3 * lo], buildings[3 * lo + 2]))
            one.add(intArrayOf(buildings[3 * lo + 1], 0))
            return one
        }
        val mid = (lo + hi) / 2
        return merge(solve(lo, mid), solve(mid + 1, hi))
    }
    val points = solve(0, count - 1)
    val out = IntArray(points.size * 2)
    for (k in points.indices) { out[2 * k] = points[k][0]; out[2 * k + 1] = points[k][1] }
    return out
}
"""),
        ("equal-x-takes-one-side", "WRONG_BRANCH",
         "두 윤곽선의 x 가 같을 때 한쪽만 꺼낸다. 다른 쪽의 높이 변화가 한 점 늦게 반영된다.",
         """
fun skyline(buildings: IntArray): IntArray {
    val count = buildings.size / 3
    if (count == 0) return IntArray(0)
    fun merge(left: ArrayList<IntArray>, right: ArrayList<IntArray>): ArrayList<IntArray> {
        val out = ArrayList<IntArray>()
        var i = 0; var j = 0; var hl = 0; var hr = 0
        fun push(x: Int, h: Int) { if (out.isEmpty() || out[out.size - 1][1] != h) out.add(intArrayOf(x, h)) }
        while (i < left.size && j < right.size) {
            val x: Int
            if (left[i][0] <= right[j][0]) { x = left[i][0]; hl = left[i][1]; i += 1 }
            else { x = right[j][0]; hr = right[j][1]; j += 1 }
            push(x, if (hl > hr) hl else hr)
        }
        while (i < left.size) { push(left[i][0], left[i][1]); i += 1 }
        while (j < right.size) { push(right[j][0], right[j][1]); j += 1 }
        return out
    }
    fun solve(lo: Int, hi: Int): ArrayList<IntArray> {
        if (lo == hi) {
            val one = ArrayList<IntArray>(2)
            one.add(intArrayOf(buildings[3 * lo], buildings[3 * lo + 2]))
            one.add(intArrayOf(buildings[3 * lo + 1], 0))
            return one
        }
        val mid = (lo + hi) / 2
        return merge(solve(lo, mid), solve(mid + 1, hi))
    }
    val points = solve(0, count - 1)
    val out = IntArray(points.size * 2)
    for (k in points.indices) { out[2 * k] = points[k][0]; out[2 * k + 1] = points[k][1] }
    return out
}
"""),
        ("right-edge-inclusive", "OFF_BY_ONE",
         "건물이 오른쪽 끝 좌표까지 서 있다고 본다. 구간은 오른쪽이 열려 있다.",
         """
fun skyline(buildings: IntArray): IntArray {
    val count = buildings.size / 3
    if (count == 0) return IntArray(0)
    fun merge(left: ArrayList<IntArray>, right: ArrayList<IntArray>): ArrayList<IntArray> {
        val out = ArrayList<IntArray>()
        var i = 0; var j = 0; var hl = 0; var hr = 0
        fun push(x: Int, h: Int) { if (out.isEmpty() || out[out.size - 1][1] != h) out.add(intArrayOf(x, h)) }
        while (i < left.size && j < right.size) {
            val x: Int
            if (left[i][0] < right[j][0]) { x = left[i][0]; hl = left[i][1]; i += 1 }
            else if (left[i][0] > right[j][0]) { x = right[j][0]; hr = right[j][1]; j += 1 }
            else { x = left[i][0]; hl = left[i][1]; hr = right[j][1]; i += 1; j += 1 }
            push(x, if (hl > hr) hl else hr)
        }
        while (i < left.size) { push(left[i][0], left[i][1]); i += 1 }
        while (j < right.size) { push(right[j][0], right[j][1]); j += 1 }
        return out
    }
    fun solve(lo: Int, hi: Int): ArrayList<IntArray> {
        if (lo == hi) {
            val one = ArrayList<IntArray>(2)
            one.add(intArrayOf(buildings[3 * lo], buildings[3 * lo + 2]))
            one.add(intArrayOf(buildings[3 * lo + 1] + 1, 0))
            return one
        }
        val mid = (lo + hi) / 2
        return merge(solve(lo, mid), solve(mid + 1, hi))
    }
    val points = solve(0, count - 1)
    val out = IntArray(points.size * 2)
    for (k in points.indices) { out[2 * k] = points[k][0]; out[2 * k + 1] = points[k][1] }
    return out
}
"""),
        ("scan-every-coordinate", "PERFORMANCE",
         "좌표마다 모든 건물을 훑어 그 자리의 높이를 구한다. O(n^2).",
         """
fun skyline(buildings: IntArray): IntArray {
    val count = buildings.size / 3
    if (count == 0) return IntArray(0)
    val xs = java.util.TreeSet<Int>()
    for (i in 0 until count) { xs.add(buildings[3 * i]); xs.add(buildings[3 * i + 1]) }
    val out = ArrayList<Int>()
    var last = -1
    for (x in xs) {
        var best = 0
        for (i in 0 until count) {
            Drill.compare(x, i)
            if (buildings[3 * i] <= x && x < buildings[3 * i + 1] && buildings[3 * i + 2] > best) best = buildings[3 * i + 2]
        }
        if (best != last) { out.add(x); out.add(best); last = best }
    }
    return IntArray(out.size) { out[it] }
}
"""),
    ],
))
