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
    fun ways(step: Int): Long {
        Drill.call("ways($step)")
        return when {
        step <= 1 -> 1L
        else -> (ways(step - 1) + ways(step - 2)) % 1_000_000_007L
        }
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
            Drill.compare(j, i)
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


# --- 59. 가장 긴 공통 부분 수열 (메모리로 잡는 오답) ---------------------------------------

def _lcs_length(first, second):
    """비트 병렬 LCS (Hyyrö). 15,000 × 15,000 의 표를 파이썬으로 채우면 몇 분이다."""
    n = len(first)
    if n == 0 or not second:
        return 0
    masks = {}
    for i, ch in enumerate(first):
        masks[ch] = masks.get(ch, 0) | (1 << i)
    full = (1 << n) - 1
    row = full
    for ch in second:
        hit = row & masks.get(ch, 0)
        row = ((row + hit) | (row - hit)) & full
    return n - bin(row).count("1")


def _letters(count, alphabet, salt):
    picks = randoms(count, 0, len(alphabet) - 1, salt=salt)
    return "".join(alphabet[p] for p in picks)


PROBLEMS.append(Problem(
    id="longest-common-subsequence",
    title="가장 긴 공통 부분 수열",
    summary="""
두 문자열 `first` 와 `second` 가 주어진다. 둘 모두의 **부분 수열**(순서를 지키되 건너뛸 수
있는 문자열)인 것 중 가장 긴 것의 길이를 반환한다.

예: `"abcde"` 와 `"ace"` 의 답은 `3` (`"ace"`), `"abc"` 와 `"def"` 의 답은 `0` 이다.
""",
    notes="""
`first` 의 앞 i 글자와 `second` 의 앞 j 글자의 답을 표로 채우면 된다. 다만 **표 전체를
들고 있을 필요가 없다** — 한 행을 채우는 데 필요한 것은 바로 앞 행뿐이다. 두 문자열이
15,000 자면 표 전체는 2 억 2 천만 칸이고, 그것을 정수로 두면 메모리 한도의 세 배가 넘는다.
""",
    drill_doc="""
Drill.compare(i, j)           // 두 글자를 견줬다
Drill.write(j, length)        // 그 자리까지의 답
""",
    constraints="""
- `0 <= first.length, second.length <= 15_000`
- 영문 소문자만
- 메모리 256MB — 표 전체는 여기 들어가지 않는다
""",
    signature=dict(name="lcsLength", parameters=[("first", "STRING"), ("second", "STRING")],
                   returns="INT"),
    groups=perf_groups(),
    reference=_lcs_length,
    cases={
        "sample": [
            ("01", ["abcde", "ace"]),
            ("02", ["abc", "def"]),
        ],
        "boundary": [
            ("01-both-empty", ["", ""]),
            ("02-one-empty", ["abc", ""]),
            ("03-identical", ["kotlin", "kotlin"]),
            ("04-single-match", ["a", "a"]),
            ("05-single-mismatch", ["a", "b"]),
            # 같은 글자가 여러 번. 한 번 맞춘 글자를 다시 쓰면 안 된다.
            ("06-repeats", ["aaaa", "aa"]),
            # 순서가 뒤집혔다. 공통 글자는 많지만 순서를 지키면 하나뿐이다.
            ("07-reversed", ["abcd", "dcba"]),
            # 앞 행의 대각선 값을 써야 한다. 같은 행을 쓰면 하나 더 센다.
            ("08-diagonal", ["ab", "ba"]),
        ],
        "hidden": [
            ("01-random-small", [_letters(40, "abc", salt=1901), _letters(35, "abc", salt=1902)]),
            ("02-random-medium", [_letters(800, "abcd", salt=1903), _letters(900, "abcd", salt=1904)]),
            ("03-subsequence", ["abcdefghij" * 20, "acegi" * 20]),
            ("04-disjoint", ["a" * 500, "b" * 500]),
        ],
        "performance": [
            ("01-small", [_letters(2000, "abcd", salt=1905), _letters(2000, "abcd", salt=1906)]),
            ("02-medium", [_letters(8000, "abcd", salt=1907), _letters(8000, "abcd", salt=1908)]),
            # 15,000 × 15,000. 표 전체는 900MB — 시간은 넉넉한데 메모리에서 진다.
            ("03-large", [_letters(15000, "abcd", salt=1909), _letters(15000, "abcd", salt=1910)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 행 두 개만 든다.
fun lcsLength(first: String, second: String): Int {
    var previous = IntArray(second.length + 1)
    var current = IntArray(second.length + 1)
    for (i in 1..first.length) {
        val a = first[i - 1]
        for (j in 1..second.length) {
            current[j] = if (a == second[j - 1]) previous[j - 1] + 1 else maxOf(previous[j], current[j - 1])
        }
        Drill.write(i, current[second.length])
        val swap = previous
        previous = current
        current = swap
    }
    return previous[second.length]
}
""",
    mutants=[
        ("full-table--memory", "PERFORMANCE",
         "표 전체를 든다. 시간은 넉넉한데 15,000 × 15,000 의 정수 표는 메모리 한도의 세 배다.",
         """
fun lcsLength(first: String, second: String): Int {
    val table = Array(first.length + 1) { IntArray(second.length + 1) }
    for (i in 1..first.length) {
        for (j in 1..second.length) {
            table[i][j] = if (first[i - 1] == second[j - 1]) table[i - 1][j - 1] + 1 else maxOf(table[i - 1][j], table[i][j - 1])
        }
    }
    return table[first.length][second.length]
}
"""),
        ("same-row-diagonal", "WRONG_BRANCH",
         "글자가 맞았을 때 앞 행의 대각선이 아니라 같은 행의 왼쪽에 1 을 더한다. 한 글자를 두 번 센다.",
         """
fun lcsLength(first: String, second: String): Int {
    var previous = IntArray(second.length + 1)
    var current = IntArray(second.length + 1)
    for (i in 1..first.length) {
        for (j in 1..second.length) {
            current[j] = if (first[i - 1] == second[j - 1]) current[j - 1] + 1 else maxOf(previous[j], current[j - 1])
        }
        val swap = previous; previous = current; current = swap
    }
    return previous[second.length]
}
"""),
        ("greedy-match--first-occurrence", "WRONG_ALGORITHM",
         "first 를 훑으며 second 에서 다음에 나오는 같은 글자를 탐욕으로 짝짓는다. 뒤의 더 긴 짝을 놓친다.",
         """
fun lcsLength(first: String, second: String): Int {
    var j = 0
    var count = 0
    for (a in first) {
        var k = j
        while (k < second.length && second[k] != a) k += 1
        if (k < second.length) { count += 1; j = k + 1 }
    }
    return count
}
"""),
        ("stale-row--no-swap", "OFF_BY_ONE",
         "행을 바꿔 끼우지 않고 현재 행을 앞 행 위에 덮어쓴다. 대각선 값이 이미 갱신된 것을 읽는다.",
         """
fun lcsLength(first: String, second: String): Int {
    val row = IntArray(second.length + 1)
    for (i in 1..first.length) {
        for (j in 1..second.length) {
            row[j] = if (first[i - 1] == second[j - 1]) row[j - 1] + 1 else maxOf(row[j], row[j - 1])
        }
    }
    return row[second.length]
}
"""),
    ],
))


# --- 95. 풍선 터뜨리기 (구간 DP) -------------------------------------------------------

def _burst_balloons(nums):
    vals = [1] + list(nums) + [1]
    n = len(vals)
    best = [[0] * n for _ in range(n)]
    for length in range(2, n):
        for left in range(0, n - length):
            right = left + length
            top = 0
            for k in range(left + 1, right):
                top = max(top, best[left][k] + best[k][right] + vals[left] * vals[k] * vals[right])
            best[left][right] = top
    return best[0][n - 1]


PROBLEMS.append(Problem(
    id="burst-balloons",
    title="풍선 터뜨리기",
    summary="""
풍선이 한 줄로 있고 `nums[i]` 는 그 풍선의 수다. 풍선 `i` 를 터뜨리면 **양옆에 남아 있는**
풍선의 수를 곱한 `nums[left] · nums[i] · nums[right]` 만큼 점수를 얻고, 그 풍선은 사라져
양옆이 이웃이 된다. 줄의 바깥은 수가 `1` 인 풍선이 있는 것으로 본다.

전부 터뜨려 얻을 수 있는 최대 점수를 반환한다.

예: `[3, 1, 5, 8]` → `167` (1, 5, 3, 8 순서: 3·1·5 + 3·5·8 + 1·3·8 + 1·8·1).
""",
    notes="""
"먼저 무엇을 터뜨릴까"로 생각하면 터뜨린 뒤 이웃이 바뀌어 부분 문제가 겹치지 않는다.
거꾸로 **구간에서 마지막에 터뜨릴 풍선**을 고르면, 그때 양옆은 구간의 바깥 경계라 고정이고
왼쪽 구간과 오른쪽 구간은 서로 독립이다. 그것이 구간 DP 다 — 짧은 구간부터 채운다.
""",
    drill_doc="""
Drill.visit(left, right)      // 구간을 채우고 있다
Drill.compare(k, best)        // k 를 마지막으로 골라 봤다
Drill.write(left, best)       // 그 구간의 최댓값
""",
    constraints="""
- `1 <= nums.size <= 300`
- `0 <= nums[i] <= 100`
""",
    signature=dict(name="burstBalloons", parameters=[("nums", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(),
    reference=_burst_balloons,
    cases={
        "sample": [
            ("01", [[3, 1, 5, 8]]),
            ("02", [[1, 5]]),
        ],
        "boundary": [
            ("01-single", [[7]]),
            ("02-zero", [[0]]),
            # 0 이 사이에 있다. 0 을 먼저 터뜨려야 이웃끼리 곱해진다.
            ("03-zero-between", [[5, 0, 5]]),
            ("04-all-ones", [[1, 1, 1, 1]]),
            # 큰 수를 마지막까지 남겨야 한다.
            ("05-keep-big-last", [[9, 1, 1, 9]]),
            ("06-two", [[4, 6]]),
        ],
        "hidden": [
            ("01-random-small", [randoms(8, 0, 20, salt=5101)]),
            ("02-random-medium", [randoms(30, 0, 100, salt=5102)]),
            ("03-random-zeros", [[v if v > 30 else 0 for v in randoms(25, 0, 100, salt=5103)]]),
            ("04-max-values", [[100] * 40]),
        ],
        "performance": [
            ("01-small", [randoms(100, 0, 100, salt=5104)]),
            ("02-medium", [randoms(200, 0, 100, salt=5105)]),
            ("03-large", [randoms(300, 0, 100, salt=5106)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 구간의 마지막 풍선을 고르는 구간 DP. O(n³).
fun burstBalloons(nums: IntArray): Int {
    val vals = IntArray(nums.size + 2) { 1 }
    for (i in nums.indices) vals[i + 1] = nums[i]
    val n = vals.size
    val best = Array(n) { IntArray(n) }
    for (length in 2 until n) {
        for (left in 0 until n - length) {
            val right = left + length
            Drill.visit(left, right)
            var top = 0
            for (k in left + 1 until right) {
                val score = best[left][k] + best[k][right] + vals[left] * vals[k] * vals[right]
                if (score > top) { top = score; Drill.compare(k, top) }
            }
            best[left][right] = top
            Drill.write(left, top)
        }
    }
    return best[0][n - 1]
}
""",
    mutants=[
        ("greedy-smallest-first", "WRONG_ALGORITHM",
         "가장 작은 풍선부터 터뜨린다. 국소 선택이 전체 최적이 아니다.",
         """
fun burstBalloons(nums: IntArray): Int {
    val list = ArrayList<Int>().apply { add(1); nums.forEach { add(it) }; add(1) }
    var total = 0
    while (list.size > 2) {
        var at = 1
        for (i in 1 until list.size - 1) if (list[i] < list[at]) at = i
        total += list[at - 1] * list[at] * list[at + 1]
        list.removeAt(at)
    }
    return total
}
"""),
        ("uses-original-neighbors", "WRONG_ALGORITHM",
         "구간의 첫 풍선을 먼저 터뜨리는 것으로 나눈다 — 이웃이 원래 자리의 것이라 부분 문제가 독립이 아니다.",
         """
fun burstBalloons(nums: IntArray): Int {
    val vals = IntArray(nums.size + 2) { 1 }
    for (i in nums.indices) vals[i + 1] = nums[i]
    val n = vals.size
    val best = Array(n) { IntArray(n) }
    for (length in 2 until n) for (left in 0 until n - length) {
        val right = left + length
        var top = 0
        for (k in left + 1 until right) top = maxOf(top, best[left][k] + best[k][right] + vals[k - 1] * vals[k] * vals[k + 1])
        best[left][right] = top
    }
    return best[0][n - 1]
}
"""),
        ("skips-last-candidate", "OFF_BY_ONE",
         "마지막 풍선 후보를 right-1 앞까지만 본다. 구간의 끝 풍선을 마지막에 터뜨리는 경우가 빠진다.",
         """
fun burstBalloons(nums: IntArray): Int {
    val vals = IntArray(nums.size + 2) { 1 }
    for (i in nums.indices) vals[i + 1] = nums[i]
    val n = vals.size
    val best = Array(n) { IntArray(n) }
    for (length in 2 until n) for (left in 0 until n - length) {
        val right = left + length
        var top = 0
        for (k in left + 1 until maxOf(left + 2, right - 1)) top = maxOf(top, best[left][k] + best[k][right] + vals[left] * vals[k] * vals[right])
        best[left][right] = top
    }
    return best[0][n - 1]
}
"""),
        ("recursion-without-memo", "PERFORMANCE",
         "구간을 재귀로 풀되 기억하지 않는다. 지수적이다.",
         """
fun burstBalloons(nums: IntArray): Int {
    val vals = IntArray(nums.size + 2) { 1 }
    for (i in nums.indices) vals[i + 1] = nums[i]
    fun solve(left: Int, right: Int): Int {
        if (right - left < 2) return 0
        var top = 0
        for (k in left + 1 until right) { Drill.compare(k, 0); top = maxOf(top, solve(left, k) + solve(k, right) + vals[left] * vals[k] * vals[right]) }
        return top
    }
    return solve(0, vals.size - 1)
}
"""),
    ],
))


# --- 100. 숫자열 해독 (재귀에서 DP 로) ---------------------------------------------------

def _decode_ways(digits):
    n = len(digits)
    if n == 0:
        return 0
    MOD = 1_000_000_007
    prev2, prev1 = 1, 1 if digits[0] != "0" else 0
    for i in range(1, n):
        cur = 0
        if digits[i] != "0":
            cur += prev1
        pair = int(digits[i - 1:i + 1])
        if 10 <= pair <= 26:
            cur += prev2
        prev2, prev1 = prev1, cur % MOD
    return prev1


PROBLEMS.append(Problem(
    id="decode-ways",
    title="숫자열 해독",
    summary="""
`A` 는 `1`, `B` 는 `2`, …, `Z` 는 `26` 으로 부호화한 숫자열 `digits` 가 주어진다. 이것을
글자열로 되돌리는 **방법의 수**를 `1_000_000_007` 로 나눈 나머지로 반환한다.

`"12"` 는 `AB` (1, 2) 또는 `L` (12) 로 `2`, `"226"` 은 `BZ`, `VF`, `BBF` 로 `3`. `"06"` 은
`0` — `0` 으로 시작하는 조각은 없다. 빈 문자열의 답은 `0` 이다.
""",
    notes="""
앞에서부터 "여기까지 해독하는 방법의 수"를 세면, 지금 자리는 **한 글자로** (0 이 아니면)
또는 **앞 자리와 두 글자로** (10~26 이면) 온다. 그래서 `f(i) = f(i-1) [한 글자] + f(i-2)
[두 글자]` — 계단 오르기와 같은 모양에 조건이 붙은 것이다. 재귀로 하면 두 갈래가 겹쳐
지수적이다.
""",
    drill_doc="""
Drill.visit(i, ways)          // i 번째 자리까지의 방법 수
Drill.compare(i - 1, i)       // 두 글자로 묶어 봤다
""",
    constraints="""
- `0 <= digits.length <= 100_000`, 숫자만
""",
    signature=dict(name="decodeWays", parameters=[("digits", "STRING")], returns="INT"),
    groups=perf_groups(),
    reference=_decode_ways,
    cases={
        "sample": [
            ("01", ["12"]),
            ("02", ["226"]),
        ],
        "boundary": [
            ("01-empty", [""]),
            ("02-leading-zero", ["06"]),
            ("03-zero-alone", ["0"]),
            # 0 은 앞 자리와 묶여야만 한다: 10, 20.
            ("04-ten", ["10"]),
            ("05-twenty-seven", ["27"]),
            # 30 은 30 도 안 되고 0 도 안 된다.
            ("06-thirty", ["30"]),
            ("07-double-zero", ["100"]),
            ("08-ones", ["1111"]),
            ("09-single-nine", ["9"]),
        ],
        "hidden": [
            ("01-mixed", ["11106"]),
            ("02-long-ones", ["1" * 60]),
            ("03-random", ["".join(str(v) for v in randoms(200, 0, 9, salt=6401))]),
            ("04-random-no-zero", ["".join(str(v) for v in randoms(500, 1, 9, salt=6402))]),
            ("05-zeros-inside", ["2020202010"]),
        ],
        "performance": [
            ("01-small", ["1" * 2000]),
            ("02-medium", ["2" * 20000]),
            ("03-large", ["1" * 100000]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). f(i) = f(i-1)[한 글자] + f(i-2)[두 글자].
fun decodeWays(digits: String): Int {
    if (digits.isEmpty()) return 0
    val mod = 1_000_000_007L
    var prev2 = 1L
    var prev1 = if (digits[0] != '0') 1L else 0L
    for (i in 1 until digits.length) {
        var cur = 0L
        if (digits[i] != '0') cur += prev1
        val pair = (digits[i - 1] - '0') * 10 + (digits[i] - '0')
        if (pair in 10..26) { cur += prev2; Drill.compare(i - 1, i) }
        prev2 = prev1
        prev1 = cur % mod
        Drill.visit(i, prev1.toInt())
    }
    return prev1.toInt()
}
""",
    mutants=[
        ("zero-as-single", "MISSING_EDGE_CASE",
         "0 을 한 글자로도 센다. 0 으로 시작하는 조각은 없다.",
         """
fun decodeWays(digits: String): Int {
    if (digits.isEmpty()) return 0
    val mod = 1_000_000_007L
    var prev2 = 1L; var prev1 = 1L
    for (i in 1 until digits.length) {
        var cur = prev1
        val pair = (digits[i - 1] - '0') * 10 + (digits[i] - '0')
        if (pair in 10..26) cur += prev2
        prev2 = prev1; prev1 = cur % mod
    }
    return prev1.toInt()
}
"""),
        ("pair-upper-bound-27", "OFF_BY_ONE",
         "두 글자 조각을 27 까지 허용한다.",
         """
fun decodeWays(digits: String): Int {
    if (digits.isEmpty()) return 0
    val mod = 1_000_000_007L
    var prev2 = 1L; var prev1 = if (digits[0] != '0') 1L else 0L
    for (i in 1 until digits.length) {
        var cur = 0L
        if (digits[i] != '0') cur += prev1
        val pair = (digits[i - 1] - '0') * 10 + (digits[i] - '0')
        if (pair in 10..27) cur += prev2
        prev2 = prev1; prev1 = cur % mod
    }
    return prev1.toInt()
}
"""),
        ("pair-with-leading-zero", "WRONG_BRANCH",
         "앞 자리가 0 인 두 글자 조각(01~09)도 허용한다.",
         """
fun decodeWays(digits: String): Int {
    if (digits.isEmpty()) return 0
    val mod = 1_000_000_007L
    var prev2 = 1L; var prev1 = if (digits[0] != '0') 1L else 0L
    for (i in 1 until digits.length) {
        var cur = 0L
        if (digits[i] != '0') cur += prev1
        val pair = (digits[i - 1] - '0') * 10 + (digits[i] - '0')
        if (pair in 1..26) cur += prev2
        prev2 = prev1; prev1 = cur % mod
    }
    return prev1.toInt()
}
"""),
        ("recursion-without-memo", "PERFORMANCE",
         "앞에서부터 두 갈래로 재귀한다. 기억이 없어 지수적이다.",
         """
fun decodeWays(digits: String): Int {
    if (digits.isEmpty()) return 0
    val mod = 1_000_000_007L
    fun ways(i: Int): Long {
        if (i == digits.length) return 1L
        if (digits[i] == '0') return 0L
        Drill.visit(i, 0)
        var total = ways(i + 1)
        if (i + 1 < digits.length) {
            val pair = (digits[i] - '0') * 10 + (digits[i + 1] - '0')
            if (pair <= 26) total += ways(i + 2)
        }
        return total % mod
    }
    return ways(0).toInt()
}
"""),
    ],
))


# --- 111. 정규식 맞추기 ('.' 과 '*') ----------------------------------------------------------

def _regex_match(s, p):
    m, n = len(s), len(p)
    dp = [[False] * (n + 1) for _ in range(m + 1)]
    dp[0][0] = True
    for j in range(2, n + 1):
        if p[j - 1] == "*":
            dp[0][j] = dp[0][j - 2]
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            if p[j - 1] == "*":
                dp[i][j] = dp[i][j - 2] or ((p[j - 2] == "." or p[j - 2] == s[i - 1]) and dp[i - 1][j])
            else:
                dp[i][j] = (p[j - 1] == "." or p[j - 1] == s[i - 1]) and dp[i - 1][j - 1]
    return 1 if dp[m][n] else 0


PROBLEMS.append(Problem(
    id="regex-match",
    title="정규식 맞추기",
    summary="""
문자열 `s` 와 패턴 `p` 가 주어진다. `p` 는 소문자, `.` (아무 글자 하나), `*` (바로 앞
원소의 0 번 이상 반복) 으로 되어 있다. 패턴이 `s` **전체**에 맞으면 `1`, 아니면 `0` 이다.

예: `"aa"`, `"a*"` → `1`. `"ab"`, `".*"` → `1`. `"aab"`, `"c*a*b"` → `1`. `"aa"`, `"a"` → `0`.
""",
    notes="""
`dp[i][j]` = `s` 의 앞 `i` 글자가 `p` 의 앞 `j` 글자에 맞는가. `p[j-1]` 이 `*` 면 두 갈래다:
그 원소를 0 번 쓰거나 (`dp[i][j-2]`), 한 번 더 써서 `s[i-1]` 을 먹거나 (`dp[i-1][j]`, 앞
원소가 `s[i-1]` 과 맞을 때). 빈 `s` 에 `a*b*` 가 맞는 것이 첫 행이고, 그것을 잊으면
`x*` 로 시작하는 패턴이 전부 틀린다. 재귀로 하면 `a*a*a*…b` 에서 지수적이다.
""",
    drill_doc="""
Drill.visit(i, j)             // dp 칸을 채웠다
Drill.match(i, j)             // 맞았다
""",
    constraints="""
- `0 <= s.length <= 1_000`, `0 <= p.length <= 1_000`
- `*` 앞에는 항상 원소가 있다
""",
    signature=dict(name="regexMatch", parameters=[("s", "STRING"), ("p", "STRING")], returns="INT"),
    groups=perf_groups(),
    reference=_regex_match,
    cases={
        "sample": [("01", ["aa", "a*"]), ("02", ["aa", "a"])],
        "boundary": [
            ("01-both-empty", ["", ""]),
            # 빈 s 에 x* 는 맞는다. 첫 행을 잊으면 틀린다.
            ("02-empty-s-star", ["", "a*b*"]),
            ("03-empty-p", ["a", ""]),
            ("04-dot-star", ["ab", ".*"]),
            ("05-star-zero-times", ["aab", "c*a*b"]),
            # .* 가 앞을 먹고 나머지가 맞아야 한다.
            ("06-dot-star-then-literal", ["abcd", ".*d"]),
            ("07-dot-star-then-wrong", ["abcd", ".*e"]),
            # 부분이 아니라 전체가 맞아야 한다.
            ("08-prefix-only", ["abc", "ab"]),
            ("09-star-must-match-same", ["aab", "a*b*"]),
            ("10-mississippi", ["mississippi", "mis*is*p*."]),
        ],
        "hidden": [
            ("01-mixed", ["mississippi", "mis*is*ip*."]),
            ("02-many-stars", ["aaaaaaaab", "a*a*a*a*b"]),
            ("03-dots", ["abcde", "a.c.e"]),
            ("04-dots-wrong-length", ["abcde", "a.c."]),
            ("05-long-literal", ["ab" * 200, "ab" * 200]),
        ],
        "performance": [
            # 재귀는 a*a*…b 에서 지수적이다.
            ("01-small", ["a" * 25, "a*" * 12 + "b"]),
            ("02-medium", ["a" * 30, "a*" * 15 + "b"]),
            ("03-large", ["a" * 1000, "a*" * 500 + "b"]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). dp[i][j] = s 의 앞 i 글자가 p 의 앞 j 글자에 맞는가.
fun regexMatch(s: String, p: String): Int {
    val m = s.length; val n = p.length
    val dp = Array(m + 1) { BooleanArray(n + 1) }
    dp[0][0] = true
    for (j in 2..n) if (p[j - 1] == '*') dp[0][j] = dp[0][j - 2]
    for (i in 1..m) for (j in 1..n) {
        dp[i][j] = if (p[j - 1] == '*') {
            dp[i][j - 2] || ((p[j - 2] == '.' || p[j - 2] == s[i - 1]) && dp[i - 1][j])
        } else {
            (p[j - 1] == '.' || p[j - 1] == s[i - 1]) && dp[i - 1][j - 1]
        }
        if (dp[i][j]) Drill.visit(i, j)
    }
    if (dp[m][n]) Drill.match(m, n)
    return if (dp[m][n]) 1 else 0
}
""",
    mutants=[
        ("no-empty-row", "MISSING_EDGE_CASE", "빈 s 에 a*b* 가 맞는 첫 행을 채우지 않는다.", """
fun regexMatch(s: String, p: String): Int {
    val m = s.length; val n = p.length
    val dp = Array(m + 1) { BooleanArray(n + 1) }
    dp[0][0] = true
    for (i in 1..m) for (j in 1..n) {
        dp[i][j] = if (p[j - 1] == '*') dp[i][j - 2] || ((p[j - 2] == '.' || p[j - 2] == s[i - 1]) && dp[i - 1][j])
        else (p[j - 1] == '.' || p[j - 1] == s[i - 1]) && dp[i - 1][j - 1]
    }
    return if (dp[m][n]) 1 else 0
}
"""),
        ("star-at-least-once", "WRONG_BRANCH", "* 를 1 번 이상으로 본다. 0 번 갈래가 없다.", """
fun regexMatch(s: String, p: String): Int {
    val m = s.length; val n = p.length
    val dp = Array(m + 1) { BooleanArray(n + 1) }
    dp[0][0] = true
    for (i in 1..m) for (j in 1..n) {
        dp[i][j] = if (p[j - 1] == '*') (p[j - 2] == '.' || p[j - 2] == s[i - 1]) && (dp[i - 1][j] || dp[i - 1][j - 2])
        else (p[j - 1] == '.' || p[j - 1] == s[i - 1]) && dp[i - 1][j - 1]
    }
    return if (dp[m][n]) 1 else 0
}
"""),
        ("prefix-match", "WRONG_ALGORITHM", "패턴이 앞부분에 맞으면 맞다고 본다. 전체여야 한다.", """
fun regexMatch(s: String, p: String): Int {
    val m = s.length; val n = p.length
    val dp = Array(m + 1) { BooleanArray(n + 1) }
    dp[0][0] = true
    for (j in 2..n) if (p[j - 1] == '*') dp[0][j] = dp[0][j - 2]
    for (i in 1..m) for (j in 1..n) {
        dp[i][j] = if (p[j - 1] == '*') dp[i][j - 2] || ((p[j - 2] == '.' || p[j - 2] == s[i - 1]) && dp[i - 1][j])
        else (p[j - 1] == '.' || p[j - 1] == s[i - 1]) && dp[i - 1][j - 1]
    }
    return if ((0..m).any { dp[it][n] }) 1 else 0
}
"""),
        ("recursion-without-memo", "PERFORMANCE", "재귀로 두 갈래를 뻗는다. a*a*…b 에서 지수적이다.", """
fun regexMatch(s: String, p: String): Int {
    fun go(i: Int, j: Int): Boolean {
        if (j == p.length) return i == s.length
        Drill.visit(i, j)
        val first = i < s.length && (p[j] == '.' || p[j] == s[i])
        return if (j + 1 < p.length && p[j + 1] == '*') go(i, j + 2) || (first && go(i + 1, j))
        else first && go(i + 1, j + 1)
    }
    return if (go(0, 0)) 1 else 0
}
"""),
    ],
))
