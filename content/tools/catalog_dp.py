"""동적 계획법 (역량: 겹치는 부분 문제를 한 번만 풀기)."""

from author import Problem, standard_groups, perf_groups, randoms

PROBLEMS = []
MOD = 1_000_000_007


# --- 13. 계단 오르기 ---------------------------------------------------------

def _climb(n):
    a, b = 1, 1
    for _ in range(n - 1):
        a, b = b, (a + b) % MOD
    return b % MOD


PROBLEMS.append(Problem(
    id="climb-stairs",
    title="계단 오르기",
    summary="""
`n` 개의 계단을 오른다. 한 번에 1칸 또는 2칸을 오를 수 있다. 꼭대기에 이르는 서로 다른
방법의 수를 `1_000_000_007` 로 나눈 나머지로 반환한다.
""",
    notes="""
`n` 이 크므로 재귀만으로는 같은 부분 문제를 지수적으로 다시 푼다. 그리고 방법의 수는
금방 `Int` 를 넘으므로, **더할 때마다** 나머지를 취해야 한다.
""",
    drill_doc="""
Drill.write(step, ways)  // step 번째 계단까지의 방법 수
""",
    constraints="""
- `1 <= n <= 500_000`
""",
    signature=dict(name="climbStairs", parameters=[("n", "INT")], returns="INT"),
    groups=perf_groups(),
    reference=_climb,
    cases={
        "sample": [("01", [2]), ("02", [5])],
        "boundary": [
            ("01-one", [1]),
            ("02-two", [2]),
            ("03-three", [3]),
            # 여기서부터 Int 를 넘는다. 마지막에만 나머지를 취하면 틀린다.
            ("04-overflow-point", [50]),
            ("05-just-past-int", [46]),
        ],
        "hidden": [
            ("01-mid", [30]),
            ("02-large-mod", [1000]),
            ("03-larger-mod", [100000]),
        ],
        # 재귀만 쓴 풀이는 작은 n 도 통과하지 못한다. 반복 풀이와 메모이제이션을 가른다.
        "performance": [
            ("01-small", [40]),
            ("02-medium", [200000]),
            ("03-large", [500000]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 두 칸만 기억하는 반복.
//
// 나머지를 **더할 때마다** 취한다. 마지막에 한 번만 취하면 그 전에 이미 Int 를 넘어
// 값이 망가진다 — n 이 46 만 되어도 그렇다.
fun climbStairs(n: Int): Int {
    val mod = 1_000_000_007L
    var previous = 1L
    var current = 1L

    for (step in 1 until n) {
        val next = (previous + current) % mod
        previous = current
        current = next
        Drill.write(step, current.toInt())
    }
    return current.toInt()
}
""",
    mutants=[
        ("late-mod--overflows", "MISSING_EDGE_CASE",
         "마지막에만 나머지를 취해 그 전에 Int 가 넘친다.",
         """
fun climbStairs(n: Int): Int {
    var previous = 1
    var current = 1
    for (step in 1 until n) {
        val next = previous + current
        previous = current
        current = next
    }
    return current % 1_000_000_007
}
"""),
        ("off-by-one--shifts-sequence", "OFF_BY_ONE",
         "한 칸 덜 올라 n-1 의 답을 돌려준다.",
         """
fun climbStairs(n: Int): Int {
    val mod = 1_000_000_007L
    var previous = 1L
    var current = 1L
    for (step in 1 until n - 1) {
        val next = (previous + current) % mod
        previous = current
        current = next
    }
    return current.toInt()
}
"""),
        ("naive-recursion--exponential", "PERFORMANCE",
         "메모이제이션 없는 재귀라 같은 부분 문제를 지수적으로 다시 푼다.",
         """
fun climbStairs(n: Int): Int {
    fun ways(step: Int): Long = when {
        step <= 1 -> 1L
        else -> (ways(step - 1) + ways(step - 2)) % 1_000_000_007L
    }
    return ways(n).toInt()
}
"""),
    ],
))


# --- 14. 최소 동전 개수 ------------------------------------------------------

def _coin_change(coins, amount):
    best = [0] + [amount + 1] * amount
    for value in range(1, amount + 1):
        for coin in coins:
            if coin <= value and best[value - coin] + 1 < best[value]:
                best[value] = best[value - coin] + 1
    return -1 if best[amount] > amount else best[amount]


PROBLEMS.append(Problem(
    id="coin-change-min",
    title="최소 동전 개수",
    summary="""
동전 금액 배열 `coins` 와 목표 금액 `amount` 가 주어진다. 목표를 만드는 데 필요한
**최소 동전 개수**를 반환한다. 만들 수 없으면 `-1` 이다. 각 동전은 몇 번이든 쓸 수 있다.
""",
    notes="""
큰 동전부터 욕심껏 집으면 틀린다. `[1, 3, 4]` 로 6 을 만들 때 `4 + 1 + 1` 이 아니라
`3 + 3` 이 답이다.
""",
    drill_doc="""
Drill.write(value, count)  // value 를 만드는 최소 개수
Drill.compare(value, coin) // 이 동전을 써 봤다
""",
    constraints="""
- `1 <= coins.size <= 20`
- `1 <= coins[i] <= 10_000`
- `0 <= amount <= 100_000`
""",
    signature=dict(name="coinChange", parameters=[("coins", "INT_ARRAY"), ("amount", "INT")],
                   returns="INT"),
    groups=standard_groups(),
    reference=_coin_change,
    cases={
        "sample": [
            ("01", [[1, 2, 5], 11]),
            ("02", [[2], 3]),
        ],
        "boundary": [
            ("01-zero-amount", [[1, 5], 0]),
            ("02-impossible", [[5, 7], 3]),
            ("03-exact-single-coin", [[7], 7]),
            # 욕심쟁이가 틀리는 고전 사례.
            ("04-greedy-fails", [[1, 3, 4], 6]),
            ("05-greedy-fails-2", [[1, 5, 8], 15]),
            ("06-big-coin-unusable", [[10000], 9999]),
        ],
        "hidden": [
            ("01-many-coins", [[1, 2, 5, 10, 20, 50], 137]),
            ("02-prime-coins", [[3, 5, 7], 100]),
            ("03-impossible-large", [[4, 6], 999]),
            ("04-single-one", [[1], 500]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 금액을 1 씩 올려 가는 DP.
//
// 욕심껏 큰 동전을 집으면 안 된다. 어떤 금액의 최소 개수는 "그보다 동전 하나만큼
// 작은 금액들" 중 가장 좋은 것에 1 을 더한 값이며, 그 후보를 전부 봐야 한다.
fun coinChange(coins: IntArray, amount: Int): Int {
    val unreachable = amount + 1
    val best = IntArray(amount + 1) { unreachable }
    best[0] = 0

    for (value in 1..amount) {
        for (coin in coins) {
            if (coin > value) continue
            Drill.compare(value, coin)
            val candidate = best[value - coin] + 1
            if (candidate < best[value]) {
                best[value] = candidate
                Drill.write(value, candidate)
            }
        }
    }
    return if (best[amount] > amount) -1 else best[amount]
}
""",
    mutants=[
        ("greedy--takes-largest-first", "WRONG_BRANCH",
         "큰 동전부터 욕심껏 집는다. [1,3,4] 로 6 을 만들 때 3개를 쓴다.",
         """
fun coinChange(coins: IntArray, amount: Int): Int {
    var left = amount
    var used = 0
    for (coin in coins.sortedDescending()) {
        used += left / coin
        left %= coin
    }
    return if (left == 0) used else -1
}
"""),
        ("zero-not-handled--returns-one", "MISSING_EDGE_CASE",
         "금액이 0 일 때 0 이 아닌 값을 돌려준다.",
         """
fun coinChange(coins: IntArray, amount: Int): Int {
    val unreachable = amount + 1
    val best = IntArray(amount + 1) { unreachable }
    for (value in 1..amount) {
        for (coin in coins) {
            if (coin > value) continue
            if (coin == value) { best[value] = 1; continue }
            val candidate = best[value - coin] + 1
            if (candidate < best[value]) best[value] = candidate
        }
    }
    return if (amount == 0) 1 else if (best[amount] > amount) -1 else best[amount]
}
"""),
        ("no-impossible--returns-large", "MISSING_EDGE_CASE",
         "만들 수 없을 때 -1 대신 큰 수를 돌려준다.",
         """
fun coinChange(coins: IntArray, amount: Int): Int {
    val unreachable = amount + 1
    val best = IntArray(amount + 1) { unreachable }
    best[0] = 0
    for (value in 1..amount) {
        for (coin in coins) {
            if (coin > value) continue
            val candidate = best[value - coin] + 1
            if (candidate < best[value]) best[value] = candidate
        }
    }
    return best[amount]
}
"""),
    ],
))


# --- 15. 최장 증가 부분 수열 -------------------------------------------------

def _lis(nums):
    import bisect
    tails = []
    for value in nums:
        i = bisect.bisect_left(tails, value)
        if i == len(tails):
            tails.append(value)
        else:
            tails[i] = value
    return len(tails)


PROBLEMS.append(Problem(
    id="longest-increasing-run",
    # v2: 성능 케이스를 키웠다. 두 겹 풀이가 한도를 배로만 넘겨, 한가한 머신에서는
    # 통과하고 바쁜 머신에서만 잡혔다 (§12.1 재현성).
    version=2,
    title="최장 증가 부분 수열의 길이",
    summary="""
정수 배열 `nums` 에서 **엄격히 증가하는** 부분 수열 중 가장 긴 것의 길이를 반환한다.
부분 수열은 연속하지 않아도 되지만 순서는 유지해야 한다.
""",
    notes="""
`O(n^2)` DP 로도 풀리지만 큰 입력에서는 무너진다. 길이별로 "가능한 가장 작은 끝값"을
들고 이분 탐색으로 갱신하면 `O(n log n)` 이다.
""",
    drill_doc="""
Drill.visit(index, value)  // 원소를 봤다
Drill.write(pos, value)    // 길이 pos+1 의 가장 작은 끝값을 갱신했다
""",
    constraints="""
- `1 <= nums.size <= 200_000`
- `-10^9 <= nums[i] <= 10^9`
""",
    signature=dict(name="longestIncreasing", parameters=[("nums", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(),
    reference=_lis,
    cases={
        "sample": [
            ("01", [[10, 9, 2, 5, 3, 7, 101, 18]]),
            ("02", [[0, 1, 0, 3, 2, 3]]),
        ],
        "boundary": [
            ("01-single", [[5]]),
            ("02-all-decreasing", [[5, 4, 3, 2, 1]]),
            # 같은 값은 증가가 아니다. `<=` 로 쓴 풀이가 걸린다.
            ("03-all-equal", [[7, 7, 7, 7]]),
            ("04-all-increasing", [[1, 2, 3, 4, 5]]),
            ("05-two-equal-then-rise", [[2, 2, 3]]),
        ],
        "hidden": [
            ("01-zigzag", [[1, 5, 2, 6, 3, 7, 4, 8]]),
            ("02-random", [randoms(300, -1000, 1000, salt=71)]),
            ("03-long-tail", [list(range(50, 0, -1)) + list(range(1, 51))]),
        ],
        "performance": [
            ("01-small", [randoms(3000, -1000000, 1000000, salt=72)]),
            ("02-medium", [randoms(40000, -1000000, 1000000, salt=73)]),
            # 오름차순이면 O(n^2) 풀이가 매 위치에서 앞을 전부 훑는다.
            ("03-worst-case", [list(range(1, 400001))]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 길이별 최소 끝값 + 이분 탐색.
//
// tails[i] 는 "길이 i+1 인 증가 수열이 가질 수 있는 가장 작은 끝값"이다. 끝값이
// 작을수록 뒤에 이어 붙일 여지가 크므로, 각 길이에 대해 최소값만 들고 있으면 된다.
//
// 왼쪽 경계를 찾는 이분 탐색이라 같은 값은 기존 자리를 덮어쓴다 — 엄격히 증가해야
// 하므로 같은 값으로 길이를 늘리지 않는다.
fun longestIncreasing(nums: IntArray): Int {
    val tails = IntArray(nums.size)
    var length = 0

    for (index in nums.indices) {
        val value = nums[index]
        Drill.visit(index, value)

        var low = 0
        var high = length
        while (low < high) {
            val mid = low + (high - low) / 2
            if (tails[mid] < value) low = mid + 1 else high = mid
        }
        tails[low] = value
        Drill.write(low, value)
        if (low == length) length += 1
    }
    return length
}
""",
    mutants=[
        ("non-strict--counts-equal", "WRONG_BRANCH",
         "같은 값도 증가로 본다.",
         """
fun longestIncreasing(nums: IntArray): Int {
    val tails = IntArray(nums.size)
    var length = 0
    for (value in nums) {
        var low = 0
        var high = length
        while (low < high) {
            val mid = low + (high - low) / 2
            if (tails[mid] <= value) low = mid + 1 else high = mid
        }
        tails[low] = value
        if (low == length) length += 1
    }
    return length
}
"""),
        ("contiguous-only--needs-adjacency", "MISSING_EDGE_CASE",
         "연속한 구간만 센다. 떨어져 있는 원소를 이어 붙이지 못한다.",
         """
fun longestIncreasing(nums: IntArray): Int {
    var best = 1
    var run = 1
    for (i in 1 until nums.size) {
        run = if (nums[i] > nums[i - 1]) run + 1 else 1
        if (run > best) best = run
    }
    return best
}
"""),
        ("quadratic--scans-all-previous", "PERFORMANCE",
         "각 위치에서 앞을 전부 훑는 O(n^2) DP 다. 오름차순 입력이 최악이다.",
         """
fun longestIncreasing(nums: IntArray): Int {
    val best = IntArray(nums.size) { 1 }
    var answer = 1
    for (i in nums.indices) {
        for (j in 0 until i) {
            if (nums[j] < nums[i] && best[j] + 1 > best[i]) best[i] = best[j] + 1
        }
        if (best[i] > answer) answer = best[i]
    }
    return answer
}
"""),
    ],
))


# --- 16. 도둑질 --------------------------------------------------------------

def _rob(values):
    skip, take = 0, 0
    for value in values:
        skip, take = max(skip, take), skip + value
    return max(skip, take)


PROBLEMS.append(Problem(
    id="house-robber",
    title="이웃하지 않게 고르기",
    summary="""
집마다 든 금액이 배열 `values` 로 주어진다. **이웃한 두 집을 함께 고를 수 없을 때**
얻을 수 있는 최대 금액을 반환한다.
""",
    notes="""
"한 집 건너 전부 고르기"는 답이 아니다. `[2, 1, 1, 2]` 에서 답은 양 끝의 `4` 다.
""",
    drill_doc="""
Drill.visit(index, value)  // 집을 봤다
Drill.write(index, best)   // 이 집까지의 최선
""",
    constraints="""
- `1 <= values.size <= 200_000`
- `0 <= values[i] <= 10_000`
""",
    signature=dict(name="rob", parameters=[("values", "INT_ARRAY")], returns="INT"),
    groups=standard_groups(),
    reference=_rob,
    cases={
        "sample": [
            ("01", [[1, 2, 3, 1]]),
            ("02", [[2, 7, 9, 3, 1]]),
        ],
        "boundary": [
            ("01-single", [[9]]),
            ("02-two", [[5, 8]]),
            ("03-all-zero", [[0, 0, 0]]),
            # 한 칸씩 건너뛰기가 답이 아닌 경우.
            ("04-ends-win", [[2, 1, 1, 2]]),
            ("05-middle-wins", [[1, 9, 1]]),
        ],
        "hidden": [
            ("01-alternating", [[(10 if i % 2 == 0 else 9) for i in range(30)]]),
            ("02-random", [randoms(200, 0, 10000, salt=81)]),
            ("03-increasing", [list(range(1, 41))]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 두 상태만 들고 가는 DP.
//
// 각 집에서 갈리는 것은 "이 집을 고르는가"뿐이다. 고르면 직전을 못 고르므로 그 전까지의
// 최선에 더하고, 안 고르면 직전까지의 최선을 그대로 이어받는다. 배열을 따로 두지 않고
// 두 값만 들고 가면 메모리도 상수다.
fun rob(values: IntArray): Int {
    var skip = 0
    var take = 0

    for (index in values.indices) {
        Drill.visit(index, values[index])
        val nextSkip = maxOf(skip, take)
        val nextTake = skip + values[index]
        skip = nextSkip
        take = nextTake
        Drill.write(index, maxOf(skip, take))
    }
    return maxOf(skip, take)
}
""",
    mutants=[
        ("alternate--takes-every-other", "WRONG_BRANCH",
         "한 칸 건너 전부 고른다. 양 끝을 고르는 것이 나은 경우를 놓친다.",
         """
fun rob(values: IntArray): Int {
    var even = 0
    var odd = 0
    for (index in values.indices) {
        if (index % 2 == 0) even += values[index] else odd += values[index]
    }
    return maxOf(even, odd)
}
"""),
        ("off-by-one--skips-last-house", "OFF_BY_ONE",
         "마지막 집을 고려하지 않는다.",
         """
fun rob(values: IntArray): Int {
    var skip = 0
    var take = 0
    for (index in 0 until values.size - 1) {
        val nextSkip = maxOf(skip, take)
        val nextTake = skip + values[index]
        skip = nextSkip
        take = nextTake
    }
    return maxOf(skip, take)
}
"""),
        ("greedy-max--ignores-adjacency", "MISSING_EDGE_CASE",
         "가장 큰 집부터 고르되 이웃 조건을 제대로 지키지 않는다.",
         """
fun rob(values: IntArray): Int = values.max()
"""),
    ],
))


# --- 24. 0/1 배낭 -----------------------------------------------------------

def _knapsack(weights, values, capacity):
    best = [0] * (capacity + 1)
    for i in range(len(weights)):
        w, v = weights[i], values[i]
        for c in range(capacity, w - 1, -1):
            if best[c - w] + v > best[c]:
                best[c] = best[c - w] + v
    return best[capacity]


PROBLEMS.append(Problem(
    id="knapsack-01",
    title="0/1 배낭",
    summary="""
물건의 무게 배열 `weights` 와 가치 배열 `values`, 배낭 용량 `capacity` 가 주어진다.
각 물건은 **넣거나 넣지 않거나** 둘 중 하나일 때, 담을 수 있는 최대 가치를 반환한다.
""",
    notes="""
같은 물건을 여러 번 담을 수 없다. 1차원 배열로 풀 때 용량을 **큰 쪽부터** 훑어야 하며,
작은 쪽부터 훑으면 같은 물건을 두 번 담게 된다.

가치 대비 무게가 좋은 것부터 욕심껏 담는 방법은 틀린다.
""",
    drill_doc="""
Drill.visit(item, value)     // 물건을 봤다
Drill.write(capacity, best)  // 그 용량에서의 최선
""",
    constraints="""
- `1 <= weights.size == values.size <= 200`
- `1 <= weights[i] <= 1_000`
- `0 <= values[i] <= 10_000`
- `0 <= capacity <= 20_000`
""",
    signature=dict(
        name="knapsack",
        parameters=[("weights", "INT_ARRAY"), ("values", "INT_ARRAY"), ("capacity", "INT")],
        returns="INT",
    ),
    groups=standard_groups(),
    reference=_knapsack,
    cases={
        "sample": [
            ("01", [[1, 3, 4, 5], [1, 4, 5, 7], 7]),
            ("02", [[2, 2, 3], [3, 3, 5], 4]),
        ],
        "boundary": [
            ("01-zero-capacity", [[1, 2], [10, 20], 0]),
            ("02-single-fits", [[3], [9], 5]),
            ("03-single-too-heavy", [[9], [99], 5]),
            # 같은 물건을 두 번 담으면 더 좋아지는 배치. 훑는 방향이 틀리면 걸린다.
            ("04-reuse-would-help", [[2, 5], [10, 11], 4]),
            # 욕심쟁이가 틀리는 배치.
            ("05-greedy-fails", [[3, 4, 5], [30, 50, 60], 8]),
            ("06-all-same-weight", [[2, 2, 2], [1, 5, 3], 4]),
        ],
        "hidden": [
            ("01-many-small", [[1] * 20, list(range(1, 21)), 10]),
            ("02-exact-fit", [[3, 5, 7], [10, 20, 30], 15]),
            ("03-random", [randoms(60, 1, 100, salt=101), randoms(60, 0, 1000, salt=102), 500]),
            ("04-nothing-fits", [[100, 200], [1, 2], 50]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 1차원 배열 DP.
//
// 용량을 **큰 쪽부터** 훑는다. 작은 쪽부터 훑으면 방금 이 물건을 넣어 갱신한 값을
// 다시 읽어, 같은 물건을 여러 번 담은 답이 나온다 — 그건 다른 문제(무한 배낭)의 답이다.
fun knapsack(weights: IntArray, values: IntArray, capacity: Int): Int {
    val best = IntArray(capacity + 1)

    for (item in weights.indices) {
        Drill.visit(item, values[item])
        val weight = weights[item]

        for (room in capacity downTo weight) {
            val candidate = best[room - weight] + values[item]
            if (candidate > best[room]) {
                best[room] = candidate
                Drill.write(room, candidate)
            }
        }
    }
    return best[capacity]
}
""",
    mutants=[
        ("forward-loop--reuses-item", "WRONG_BRANCH",
         "용량을 작은 쪽부터 훑어 같은 물건을 여러 번 담는다.",
         """
fun knapsack(weights: IntArray, values: IntArray, capacity: Int): Int {
    val best = IntArray(capacity + 1)
    for (item in weights.indices) {
        for (room in weights[item]..capacity) {
            val candidate = best[room - weights[item]] + values[item]
            if (candidate > best[room]) best[room] = candidate
        }
    }
    return best[capacity]
}
"""),
        ("greedy-by-density--not-optimal", "WRONG_BRANCH",
         "가치 밀도가 높은 것부터 욕심껏 담는다.",
         """
fun knapsack(weights: IntArray, values: IntArray, capacity: Int): Int {
    val order = weights.indices.sortedByDescending { values[it].toDouble() / weights[it] }
    var room = capacity
    var total = 0
    for (item in order) {
        if (weights[item] <= room) {
            room -= weights[item]
            total += values[item]
        }
    }
    return total
}
"""),
        ("off-by-one--misses-exact-fit", "OFF_BY_ONE",
         "용량과 정확히 맞는 경우를 건너뛴다.",
         """
fun knapsack(weights: IntArray, values: IntArray, capacity: Int): Int {
    val best = IntArray(capacity + 1)
    for (item in weights.indices) {
        for (room in capacity downTo weights[item] + 1) {
            val candidate = best[room - weights[item]] + values[item]
            if (candidate > best[room]) best[room] = candidate
        }
    }
    return best[capacity]
}
"""),
    ],
))


# --- 25. 두 수열의 편집 거리 -------------------------------------------------

def _edit_distance(a, b):
    previous = list(range(len(b) + 1))
    for i in range(1, len(a) + 1):
        current = [i] + [0] * len(b)
        for j in range(1, len(b) + 1):
            cost = 0 if a[i - 1] == b[j - 1] else 1
            current[j] = min(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
        previous = current
    return previous[len(b)]


PROBLEMS.append(Problem(
    id="edit-distance",
    title="두 수열의 편집 거리",
    summary="""
정수 수열 `source` 를 `target` 으로 바꾸는 데 필요한 최소 연산 수를 반환한다. 연산은
세 가지다.

- 원소 하나 **삽입**
- 원소 하나 **삭제**
- 원소 하나 **교체**
""",
    notes="""
교체는 한 번의 연산이다. "삭제하고 삽입"으로 세면 두 번이 되어 답이 커진다.
""",
    drill_doc="""
Drill.compare(i, j)      // 두 원소를 견줬다
Drill.write(j, distance) // 그 자리까지의 최소 연산 수
""",
    constraints="""
- `0 <= source.size, target.size <= 1_000`
- `-10^9 <= 원소 <= 10^9`
""",
    signature=dict(name="editDistance",
                   parameters=[("source", "INT_ARRAY"), ("target", "INT_ARRAY")],
                   returns="INT"),
    groups=standard_groups(),
    reference=_edit_distance,
    cases={
        "sample": [
            ("01", [[1, 2, 3], [1, 4, 3]]),
            ("02", [[1, 2, 3, 4], [1, 3]]),
        ],
        "boundary": [
            ("01-identical", [[5, 6, 7], [5, 6, 7]]),
            ("02-one-empty", [[1, 2, 3], []]),
            ("03-other-empty", [[], [9, 9]]),
            ("04-both-empty", [[], []]),
            # 교체 한 번이면 되는데 삭제+삽입으로 세면 2 가 된다.
            ("05-single-replace", [[1], [2]]),
            ("06-all-different", [[1, 2, 3], [4, 5, 6]]),
        ],
        "hidden": [
            ("01-prefix-shared", [[1, 2, 3, 4, 5], [1, 2, 9, 4, 5]]),
            ("02-shift", [[1, 2, 3, 4], [2, 3, 4, 5]]),
            ("03-long", [list(range(40)), list(range(5, 45))]),
            ("04-reversed", [[1, 2, 3, 4, 5], [5, 4, 3, 2, 1]]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 두 줄만 들고 가는 DP.
//
// 세 연산이 각각 격자의 한 방향에 대응한다. 위에서 오면 삭제, 왼쪽에서 오면 삽입,
// 대각선에서 오면 교체(같으면 공짜)다. 대각선을 빠뜨리면 교체가 두 번으로 세어진다.
fun editDistance(source: IntArray, target: IntArray): Int {
    var previous = IntArray(target.size + 1) { it }
    val current = IntArray(target.size + 1)

    for (i in 1..source.size) {
        current[0] = i
        for (j in 1..target.size) {
            Drill.compare(i - 1, j - 1)
            val cost = if (source[i - 1] == target[j - 1]) 0 else 1
            current[j] = minOf(
                previous[j] + 1,
                current[j - 1] + 1,
                previous[j - 1] + cost,
            )
            Drill.write(j, current[j])
        }
        previous = current.copyOf()
    }
    return previous[target.size]
}
""",
    mutants=[
        ("no-replace--counts-two", "MISSING_EDGE_CASE",
         "교체를 삭제+삽입으로 센다. 한 글자만 다른 경우가 2 가 된다.",
         """
fun editDistance(source: IntArray, target: IntArray): Int {
    var previous = IntArray(target.size + 1) { it }
    val current = IntArray(target.size + 1)
    for (i in 1..source.size) {
        current[0] = i
        for (j in 1..target.size) {
            current[j] = if (source[i - 1] == target[j - 1]) {
                previous[j - 1]
            } else {
                minOf(previous[j] + 1, current[j - 1] + 1)
            }
        }
        for (j in previous.indices) previous[j] = current[j]
    }
    return previous[target.size]
}
"""),
        ("length-difference--too-naive", "WRONG_BRANCH",
         "길이 차이만 답으로 삼는다.",
         """
fun editDistance(source: IntArray, target: IntArray): Int {
    val diff = source.size - target.size
    return if (diff < 0) -diff else diff
}
"""),
        ("bad-init--ignores-empty", "MISSING_EDGE_CASE",
         "빈 수열로 시작하는 줄을 0 으로 두어 삽입·삭제 비용을 세지 않는다.",
         """
fun editDistance(source: IntArray, target: IntArray): Int {
    var previous = IntArray(target.size + 1)
    val current = IntArray(target.size + 1)
    for (i in 1..source.size) {
        current[0] = 0
        for (j in 1..target.size) {
            val cost = if (source[i - 1] == target[j - 1]) 0 else 1
            current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
        }
        for (j in previous.indices) previous[j] = current[j]
    }
    return previous[target.size]
}
"""),
    ],
))
