"""비트 연산 (역량: 문제 독해, 최적화 — "수를 비트의 열로 보면 무엇이 보이나").

비트 문제의 오답은 두 갈래다. 점화를 잘못 잡아 값 자체가 틀리거나, 쌍마다 세어서 맞지만
느리거나. 둘 다 여기 있다.
"""

from author import Problem, standard_groups, perf_groups, randoms, shuffled

PROBLEMS = []


# --- 1. 0 부터 n 까지의 1 비트 수 -------------------------------------------------

def _count_bits(n):
    bits = [0] * (n + 1)
    for i in range(1, n + 1):
        bits[i] = bits[i >> 1] + (i & 1)
    return bits


PROBLEMS.append(Problem(
    id="count-bits",
    title="0부터 n까지의 1비트 수",
    summary="""
정수 `n` 이 주어진다. `0` 부터 `n` 까지 **각 수를 이진수로 적었을 때 1 의 개수**를 순서대로
담은 배열을 반환한다. 배열의 길이는 `n + 1` 이다.

예: `n = 5` 이면 `0, 1, 10, 11, 100, 101` 이므로 `[0, 1, 1, 2, 1, 2]` 다.
""",
    notes="""
수마다 비트를 세어도 되지만, `i` 의 1 비트 수는 `i` 를 오른쪽으로 한 칸 민 수의 1 비트 수에
마지막 비트를 더한 것이다 — 앞에서 센 값을 다시 쓸 수 있다.
""",
    drill_doc="""
Drill.write(i, bits)          // i 의 1 비트 수를 적었다
""",
    constraints="""
- `0 <= n <= 20_000`
""",
    signature=dict(name="countBits", parameters=[("n", "INT")], returns="INT_ARRAY"),
    groups=standard_groups(),
    reference=_count_bits,
    cases={
        "sample": [
            ("01", [5]),
            ("02", [2]),
        ],
        "boundary": [
            # 0 하나. 길이 1 의 배열이다.
            ("01-zero", [0]),
            ("02-one", [1]),
            # 2 의 거듭제곱과 그 직전. 비트가 한꺼번에 바뀌는 자리다.
            ("03-power-of-two", [8]),
            ("04-before-power", [7]),
            ("05-all-ones", [1023]),
        ],
        "hidden": [
            ("01-small", [100]),
            ("02-medium", [4096]),
            ("03-max", [20000]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). bits[i] = bits[i shr 1] + (i and 1).
fun countBits(n: Int): IntArray {
    val bits = IntArray(n + 1)
    for (i in 1..n) {
        bits[i] = bits[i shr 1] + (i and 1)
        Drill.write(i, bits[i])
    }
    return bits
}
""",
    mutants=[
        ("excludes-n--off-by-one", "OFF_BY_ONE",
         "n 을 빼고 n-1 까지만 만든다. 길이가 하나 모자란다.",
         """
fun countBits(n: Int): IntArray {
    val bits = IntArray(n)
    for (i in 1 until n) bits[i] = bits[i shr 1] + (i and 1)
    return bits
}
"""),
        ("drops-low-bit--wrong-recurrence", "WRONG_BRANCH",
         "밀어낸 마지막 비트를 더하지 않는다. 홀수마다 1 씩 모자란다.",
         """
fun countBits(n: Int): IntArray {
    val bits = IntArray(n + 1)
    for (i in 1..n) bits[i] = bits[i shr 1]
    return bits
}
"""),
        ("parity-only--counts-one-bit", "WRONG_ALGORITHM",
         "마지막 비트만 본다. 3 이상에서 갈린다.",
         """
fun countBits(n: Int): IntArray = IntArray(n + 1) { it and 1 }
"""),
    ],
))


# --- 2. 모든 쌍의 해밍 거리 합 ----------------------------------------------------

def _total_hamming(nums):
    n = len(nums)
    total = 0
    for bit in range(16):
        ones = sum((v >> bit) & 1 for v in nums)
        total += ones * (n - ones)
    return total


PROBLEMS.append(Problem(
    id="total-hamming-distance",
    title="모든 쌍의 해밍 거리 합",
    summary="""
두 정수의 **해밍 거리**는 이진수로 적었을 때 서로 다른 자리의 개수다. 정수 배열 `nums` 가
주어진다. **모든 서로 다른 두 원소 쌍**의 해밍 거리를 전부 더해 반환한다.

예: `[4, 14, 2]` 는 `100, 1110, 0010` 이고, 쌍 `(4,14)`, `(4,2)`, `(14,2)` 의 거리는
`2 + 2 + 2 = 6` 이다.
""",
    notes="""
쌍마다 세면 쌍의 수만큼 든다. 자리를 고정하고 보면, 그 자리가 1 인 원소 수와 0 인 원소
수의 **곱**이 그 자리가 기여하는 거리다. 자리 수는 상수다.
""",
    drill_doc="""
Drill.visit(bit, ones)        // bit 번째 자리의 1 개수를 셌다
Drill.write(bit, total)       // 그 자리의 기여를 더했다
""",
    constraints="""
- `1 <= nums.size <= 20_000`
- `0 <= nums[i] < 2^16`
""",
    signature=dict(name="totalHamming", parameters=[("nums", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(time_multiplier=0.25),
    reference=_total_hamming,
    cases={
        "sample": [
            ("01", [[4, 14, 2]]),
            ("02", [[1, 2, 3]]),
        ],
        "boundary": [
            # 원소 하나. 쌍이 없다.
            ("01-single", [[7]]),
            # 전부 같다. 거리 0.
            ("02-all-same", [[5, 5, 5, 5]]),
            ("03-zero-and-max", [[0, 65535]]),
            # 각 자리마다 1 이 딱 하나. 자리 수 × (n-1).
            ("04-powers", [[1, 2, 4, 8, 16]]),
        ],
        "hidden": [
            ("01-random-small", [randoms(50, 0, 65535, salt=1101)]),
            ("02-random-medium", [randoms(500, 0, 65535, salt=1102)]),
            ("03-two-values", [[0, 65535] * 100]),
        ],
        "performance": [
            ("01-small", [randoms(1500, 0, 65535, salt=1103)]),
            ("02-medium", [randoms(5000, 0, 65535, salt=1104)]),
            ("03-large", [randoms(20000, 0, 65535, salt=1105)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 자리마다 1 의 개수 × 0 의 개수.
fun totalHamming(nums: IntArray): Int {
    var total = 0
    for (bit in 0 until 16) {
        var ones = 0
        for (v in nums) ones += (v shr bit) and 1
        Drill.visit(bit, ones)
        total += ones * (nums.size - ones)
        Drill.write(bit, total)
    }
    return total
}
""",
    mutants=[
        ("ones-squared--wrong-product", "WRONG_BRANCH",
         "1 의 개수끼리 곱한다. 서로 다른 쌍이 아니라 같은 쌍을 센다.",
         """
fun totalHamming(nums: IntArray): Int {
    var total = 0
    for (bit in 0 until 16) {
        var ones = 0
        for (v in nums) ones += (v shr bit) and 1
        total += ones * ones
    }
    return total
}
"""),
        ("fifteen-bits--drops-top", "OFF_BY_ONE",
         "자리를 15 개만 본다. 가장 높은 자리가 다른 쌍을 놓친다.",
         """
fun totalHamming(nums: IntArray): Int {
    var total = 0
    for (bit in 0 until 15) {
        var ones = 0
        for (v in nums) ones += (v shr bit) and 1
        total += ones * (nums.size - ones)
    }
    return total
}
"""),
        ("pairwise--counts-each-pair", "PERFORMANCE",
         "쌍마다 xor 의 비트를 하나씩 센다. O(n² · 비트 수).",
         """
fun totalHamming(nums: IntArray): Int {
    var total = 0
    for (i in nums.indices) {
        for (j in i + 1 until nums.size) {
            var x = nums[i] xor nums[j]
            Drill.compare(i, j)
            while (x != 0) { total += x and 1; x = x shr 1 }
        }
    }
    return total
}
"""),
    ],
))


# --- 89. 빠진 수 (XOR) ----------------------------------------------------------------

def _missing_number(nums):
    n = len(nums)
    return n * (n + 1) // 2 - sum(nums)


PROBLEMS.append(Problem(
    id="missing-number",
    title="빠진 수",
    summary="""
`0` 부터 `n` 까지의 정수 중 **하나만 빠진** `n` 개가 순서 없이 `nums` 에 있다. 빠진 수를
반환한다.

예: `[3, 0, 1]` 은 `0..3` 에서 `2` 가 빠졌다.
""",
    notes="""
정렬하면 O(n log n), 집합이면 O(n) 공간이다. `0..n` 의 합에서 배열의 합을 빼면 O(1) 공간
이고, XOR 로 하면 넘침 걱정도 없다 — `a ^ a = 0` 이라 `0..n` 전부와 배열 전부를 XOR 하면
짝이 없는 것만 남는다.
""",
    drill_doc="""
Drill.visit(i, nums[i])       // 원소를 봤다
Drill.write(0, acc)           // 누적 XOR
""",
    constraints="""
- `1 <= n <= 200_000`
- `nums` 의 값은 서로 다르고 `0 <= nums[i] <= n`
""",
    signature=dict(name="missingNumber", parameters=[("nums", "INT_ARRAY")], returns="INT"),
    groups=standard_groups(),
    reference=_missing_number,
    cases={
        "sample": [
            ("01", [[3, 0, 1]]),
            ("02", [[9, 6, 4, 2, 3, 5, 7, 0, 1]]),
        ],
        "boundary": [
            # n = 1. 0 이 빠졌거나 1 이 빠졌거나.
            ("01-zero-missing", [[1]]),
            ("02-n-missing", [[0]]),
            # 빠진 것이 n 이다 — 배열이 0..n-1 이다.
            ("03-last-missing", [[0, 1, 2, 3]]),
            ("04-first-missing", [[4, 3, 2, 1]]),
            ("05-two", [[2, 0]]),
        ],
        "hidden": [
            ("01-random", [shuffled([v for v in range(51) if v != 17], salt=4501)]),
            ("02-random-big", [shuffled([v for v in range(2001) if v != 1999], salt=4502)]),
            # 합이 Int 범위를 넘는다 — 200000 · 200001 / 2 > 2^31.
            ("03-sum-overflows", [shuffled([v for v in range(200001) if v != 123456], salt=4503)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 0..n 과 배열을 전부 XOR 하면 짝 없는 것만 남는다.
fun missingNumber(nums: IntArray): Int {
    var acc = nums.size
    for ((i, v) in nums.withIndex()) {
        Drill.visit(i, v)
        acc = acc xor i xor v
        Drill.write(0, acc)
    }
    return acc
}
""",
    mutants=[
        ("int-sum--overflows", "MISSING_EDGE_CASE",
         "0..n 의 합을 Int 로 구한다. n 이 20 만이면 넘친다.",
         """
fun missingNumber(nums: IntArray): Int {
    val n = nums.size
    val expected = n * (n + 1) / 2
    return expected - nums.sum()
}
"""),
        ("xor-to-n-minus-one", "OFF_BY_ONE",
         "0..n-1 만 XOR 한다. n 이 빠진 경우를 놓친다.",
         """
fun missingNumber(nums: IntArray): Int {
    var acc = 0
    for ((i, v) in nums.withIndex()) acc = acc xor i xor v
    return acc
}
"""),
        ("first-gap-in-sorted--ignores-last", "WRONG_ALGORITHM",
         "정렬해 첫 빈 자리를 찾는다. 끝까지 빈 자리가 없으면 0 을 답한다.",
         """
fun missingNumber(nums: IntArray): Int {
    val sorted = nums.sorted()
    for ((i, v) in sorted.withIndex()) if (v != i) return i
    return 0
}
"""),
    ],
))


# --- 105. 2 의 거듭제곱인가 (입문) --------------------------------------------------------

def _is_power_of_two(n):
    return 1 if n > 0 and n & (n - 1) == 0 else 0


PROBLEMS.append(Problem(
    id="power-of-two",
    title="2 의 거듭제곱인가",
    summary="""
정수 `n` 이 `2` 의 거듭제곱 (`1, 2, 4, 8, ...`) 이면 `1`, 아니면 `0` 을 반환한다.
""",
    notes="""
2 의 거듭제곱은 이진수로 1 이 하나뿐이다. `n & (n - 1)` 은 가장 낮은 1 을 지우므로, 그 결과가
0 이면 1 이 하나였던 것이다. `n` 이 0 이거나 음수면 아니다 — 0 은 `0 & -1 = 0` 이라 따로
막아야 한다.
""",
    drill_doc="""
Drill.compare(n, n - 1)       // 비트를 견줬다
""",
    constraints="""
- `-2^31 <= n <= 2^31 - 1`
""",
    signature=dict(name="isPowerOfTwo", parameters=[("n", "INT")], returns="INT"),
    groups=standard_groups(),
    reference=_is_power_of_two,
    cases={
        "sample": [("01", [16]), ("02", [12])],
        "boundary": [
            ("01-one", [1]),
            # 0 은 아니다 — n & (n-1) 만 보면 0 이 통과한다.
            ("02-zero", [0]),
            ("03-negative-power", [-8]),
            ("04-max-power", [1073741824]),
            ("05-max-int", [2147483647]),
            ("06-min-int", [-2147483648]),
            ("07-three", [3]),
        ],
        "hidden": [
            ("01-two", [2]),
            ("02-big-not", [1073741825]),
            ("03-random-powers", [1 << 20]),
            ("04-six", [6]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 1 이 하나뿐인가.
fun isPowerOfTwo(n: Int): Int {
    Drill.compare(n, n - 1)
    return if (n > 0 && n and (n - 1) == 0) 1 else 0
}
""",
    mutants=[
        ("accepts-zero", "MISSING_EDGE_CASE", "0 을 막지 않는다. 0 & -1 = 0 이라 통과한다.", """
fun isPowerOfTwo(n: Int): Int = if (n and (n - 1) == 0) 1 else 0
"""),
        ("accepts-negative", "MISSING_EDGE_CASE", "음수를 막지 않는다. Int.MIN_VALUE 는 1 이 하나라 통과한다.", """
fun isPowerOfTwo(n: Int): Int = if (n != 0 && n and (n - 1) == 0) 1 else 0
"""),
        ("divides-while-even", "OFF_BY_ONE", "2 로 나누어떨어지는 동안 나누고 1 인지 본다 — 시작을 1 로 잡지 않아 1 이 아니라고 본다.", """
fun isPowerOfTwo(n: Int): Int {
    if (n <= 1) return 0
    var v = n
    while (v % 2 == 0) v /= 2
    return if (v == 1) 1 else 0
}
"""),
    ],
))
