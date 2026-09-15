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
