"""수학·비트 (역량: 성질을 찾아 계산을 줄이기)."""

from author import Problem, standard_groups, perf_groups, randoms

PROBLEMS = []
MOD = 1_000_000_007


# --- 20. 소수의 개수 ---------------------------------------------------------

def _count_primes(n):
    if n < 2:
        return 0
    sieve = bytearray([1]) * (n + 1)
    sieve[0] = sieve[1] = 0
    i = 2
    while i * i <= n:
        if sieve[i]:
            sieve[i * i::i] = bytearray(len(sieve[i * i::i]))
        i += 1
    return sum(sieve)


PROBLEMS.append(Problem(
    id="count-primes",
    title="n 이하 소수의 개수",
    summary="""
정수 `n` 이 주어진다. `n` **이하**의 소수 개수를 반환한다.
""",
    notes="""
`n` 자신도 센다. 수마다 나눠 보는 방법은 `n` 이 커지면 무너지고, 배수를 지워 나가면
한 번에 끝난다.
""",
    drill_doc="""
Drill.visit(value, 1)   // 소수를 찾았다
Drill.write(value, 0)   // 배수를 지웠다
""",
    constraints="""
- `0 <= n <= 10_000_000`
""",
    signature=dict(name="countPrimes", parameters=[("n", "INT")], returns="INT"),
    groups=perf_groups(),
    reference=_count_primes,
    cases={
        "sample": [("01", [10]), ("02", [30])],
        "boundary": [
            ("01-zero", [0]),
            ("02-one", [1]),
            # 2 는 유일한 짝수 소수다. 홀수만 보는 풀이가 걸린다.
            ("03-two", [2]),
            ("04-three", [3]),
            # n 자신이 소수다. n 미만만 세는 풀이가 걸린다.
            ("05-n-is-prime", [97]),
            ("06-n-is-square-of-prime", [49]),
        ],
        "hidden": [
            ("01-hundred", [100]),
            ("02-thousand", [1000]),
            ("03-ten-thousand", [10000]),
        ],
        # 나눠 보는 풀이의 비용은 **소수에서만** 크다. 합성수는 대부분 2 에서 걸러지므로,
        # n 을 충분히 키워야 소수의 개수가 비용을 지배한다.
        "performance": [
            ("01-small", [200000]),
            ("02-medium", [2000000]),
            ("03-large", [10000000]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 에라토스테네스의 체.
//
// 나눠 보는 대신 지워 나간다. i 의 배수를 지울 때 i*i 부터 시작하는 것이 요령이다 —
// 그보다 작은 배수는 더 작은 소수가 이미 지웠다.
fun countPrimes(n: Int): Int {
    if (n < 2) return 0

    val composite = BooleanArray(n + 1)
    var count = 0

    for (value in 2..n) {
        if (composite[value]) continue
        count += 1
        Drill.visit(value, 1)

        if (value.toLong() * value > n) continue
        var multiple = value.toLong() * value
        while (multiple <= n) {
            composite[multiple.toInt()] = true
            multiple += value
        }
    }
    return count
}
""",
    mutants=[
        ("exclusive--counts-below-n", "OFF_BY_ONE",
         "n 미만만 센다. n 자신이 소수일 때 하나 모자란다.",
         """
fun countPrimes(n: Int): Int {
    if (n < 2) return 0
    val composite = BooleanArray(n + 1)
    var count = 0
    for (value in 2 until n) {
        if (composite[value]) continue
        count += 1
        var multiple = value.toLong() * value
        while (multiple <= n) { composite[multiple.toInt()] = true; multiple += value }
    }
    return count
}
"""),
        ("odd-only--misses-two", "MISSING_EDGE_CASE",
         "홀수만 소수 후보로 봐서 2 를 놓친다.",
         """
fun countPrimes(n: Int): Int {
    if (n < 3) return 0
    val composite = BooleanArray(n + 1)
    var count = 0
    var value = 3
    while (value <= n) {
        if (!composite[value]) {
            count += 1
            var multiple = value.toLong() * value
            while (multiple <= n) { composite[multiple.toInt()] = true; multiple += value }
        }
        value += 2
    }
    return count
}
"""),
        # 2 부터 value-1 까지 전부 나눠 본다. 소수 판정을 처음 쓸 때 가장 흔한 형태다.
        #
        # 제곱근까지만 보는 형태는 오답으로 쓰지 않았다. n=10,000,000 에서 대략 10억 번을
        # 도는데 그 10억 번이 2초 한도와 같은 자릿수라, 한가한 머신에서는 통과하고 바쁜
        # 머신에서는 잡혔다 — 공개 게이트가 머신 속도에 좌우된다는 뜻이다 (§12.1).
        ("trial-division--divides-by-everything", "PERFORMANCE",
         "2 부터 value-1 까지 전부 나눠 본다. 소수 하나를 판정하는 데 값에 비례한다.",
         """
fun countPrimes(n: Int): Int {
    var count = 0
    for (value in 2..n) {
        var prime = true
        var d = 2
        while (d < value) {
            Drill.compare(value, d)
            if (value % d == 0) { prime = false; break }
            d += 1
        }
        if (prime) count += 1
    }
    return count
}
"""),
    ],
))


# --- 21. 홀로 남은 수 --------------------------------------------------------

def _single(nums):
    out = 0
    for value in nums:
        out ^= value
    return out


PROBLEMS.append(Problem(
    id="single-number",
    title="홀로 남은 수",
    summary="""
정수 배열 `nums` 에서 **한 값만 한 번 나오고 나머지는 모두 정확히 두 번** 나온다.
한 번만 나오는 값을 반환한다.
""",
    notes="""
추가 자료구조 없이 상수 메모리로 풀 수 있다. 같은 값을 두 번 XOR 하면 사라진다는
성질을 쓰면 순서와 무관하게 한 번 훑기로 끝난다.
""",
    drill_doc="""
Drill.visit(index, value)  // 원소를 봤다
Drill.write(0, acc)        // 누적 XOR
""",
    constraints="""
- `nums.size` 는 홀수이며 `1 <= nums.size <= 200_001`
- `-10^9 <= nums[i] <= 10^9`
""",
    signature=dict(name="singleNumber", parameters=[("nums", "INT_ARRAY")], returns="INT"),
    groups=standard_groups(),
    reference=_single,
    cases={
        "sample": [
            ("01", [[2, 2, 1]]),
            ("02", [[4, 1, 2, 1, 2]]),
        ],
        "boundary": [
            ("01-single-element", [[7]]),
            # 홀로 남은 값이 0 이다. "합에서 빼기"류 풀이가 흔들린다.
            ("02-zero-is-answer", [[0, 5, 5]]),
            ("03-negative-answer", [[-3, 8, 8]]),
            # 답이 맨 앞. 짝을 찾아 지우는 구현의 순서 가정이 깨진다.
            ("04-answer-first", [[9, 1, 1, 2, 2]]),
            ("05-answer-last", [[1, 1, 2, 2, 9]]),
            ("06-large-values", [[1000000000, -1000000000, 1000000000]]),
        ],
        "hidden": [
            ("01-shuffled", [[3, 5, 3, 7, 5, 9, 7]]),
            ("02-long", [[v for i in range(1, 101) for v in (i, i)] + [12345]]),
            ("03-adjacent-pairs", [[1, 1, 2, 2, 3, 3, 4, 4, 99]]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 누적 XOR.
//
// 같은 값을 두 번 XOR 하면 0 이 되고, 0 과 XOR 하면 그대로 남는다. 그래서 전부
// XOR 하면 짝이 있는 값은 서로 지워지고 홀로 남은 값만 남는다 — 순서도, 값의
// 부호도 상관없다.
fun singleNumber(nums: IntArray): Int {
    var accumulator = 0
    for (index in nums.indices) {
        Drill.visit(index, nums[index])
        accumulator = accumulator xor nums[index]
        Drill.write(0, accumulator)
    }
    return accumulator
}
""",
    mutants=[
        ("first-unpaired-neighbour--assumes-sorted", "WRONG_BRANCH",
         "이웃한 두 개씩 짝지어 본다. 정렬돼 있다고 가정한 셈이라 섞여 있으면 틀린다.",
         """
fun singleNumber(nums: IntArray): Int {
    var index = 0
    while (index + 1 < nums.size) {
        if (nums[index] != nums[index + 1]) return nums[index]
        index += 2
    }
    return nums[nums.size - 1]
}
"""),
        ("or-instead-of-xor--never-cancels", "WRONG_BRANCH",
         "xor 대신 or 를 쓴다. 짝이 서로를 지우지 않아 비트가 쌓이기만 한다.",
         """
fun singleNumber(nums: IntArray): Int {
    var accumulator = 0
    for (value in nums) accumulator = accumulator or value
    return accumulator
}
"""),
        ("returns-first--ignores-pairs", "MISSING_EDGE_CASE",
         "첫 원소를 그냥 돌려준다.",
         """
fun singleNumber(nums: IntArray): Int = nums[0]
"""),
    ],
))


# --- 22. 거듭제곱 나머지 -----------------------------------------------------

def _power_mod(base, exponent):
    return pow(base, exponent, MOD)


PROBLEMS.append(Problem(
    id="power-mod",
    # v2: 성능 케이스를 키웠다. 두 겹 풀이가 한도를 배로만 넘겨, 한가한 머신에서는
    # 통과하고 바쁜 머신에서만 잡혔다 (§12.1 재현성).
    version=2,
    title="거듭제곱의 나머지",
    summary="""
정수 `base` 와 `exponent` 가 주어진다. `base^exponent` 를 `1_000_000_007` 로 나눈
나머지를 반환한다.
""",
    notes="""
지수가 크므로 곱셈을 지수 번 하면 끝나지 않는다. 지수를 반으로 접어 가면 `log`
번이면 된다.

곱하는 중간값이 `Int` 를 훌쩍 넘으므로 `Long` 으로 계산해야 한다.
""",
    drill_doc="""
Drill.write(0, result)  // 지금까지의 결과
Drill.visit(bit, 1)     // 지수의 이 비트가 켜져 있었다
""",
    constraints="""
- `0 <= base <= 1_000_000`
- `0 <= exponent <= 1_000_000_000`
""",
    signature=dict(name="powerMod", parameters=[("base", "INT"), ("exponent", "INT")],
                   returns="INT"),
    # 오답이 지수만큼 곱하는데 지수는 Int 상한에서 멈춘다. 입력을 더 못 키우므로
    # 성능 그룹의 시계를 조인다 — 분할 제곱은 한도의 1% 도 안 쓴다.
    groups=perf_groups(time_multiplier=0.5),
    reference=_power_mod,
    cases={
        "sample": [("01", [2, 10]), ("02", [3, 5])],
        "boundary": [
            # 어떤 수의 0 제곱은 1 이다.
            ("01-exponent-zero", [7, 0]),
            ("02-base-zero", [0, 5]),
            # 0^0 은 1 로 정의한다.
            ("03-zero-zero", [0, 0]),
            ("04-base-one", [1, 1000000000]),
            # 나머지를 취하지 않으면 여기서 넘친다.
            ("05-overflow-point", [2, 31]),
            ("06-just-past-mod", [1000000, 2]),
        ],
        "hidden": [
            ("01-medium", [123, 456]),
            ("02-large-exponent", [2, 1000000]),
            ("03-base-near-mod", [1000000, 1000000]),
        ],
        # 지수만큼 곱하는 풀이는 여기서 무너진다.
        "performance": [
            ("01-small", [3, 1000000]),
            ("02-medium", [5, 100000000]),
            ("03-large", [7, 2000000000]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 분할 제곱.
//
// 지수를 반으로 접는다. 지수의 비트를 훑으며 켜져 있으면 곱하고, 매번 밑을 제곱한다.
// 곱셈 횟수가 지수가 아니라 지수의 자릿수만큼이라 1_000_000_000 도 30번이면 끝난다.
//
// Long 으로 곱한다. 두 수가 각각 10^9 에 가까우면 곱은 10^18 이라 Int 로는 넘친다.
fun powerMod(base: Int, exponent: Int): Int {
    val mod = 1_000_000_007L
    var result = 1L
    var current = base.toLong() % mod
    var remaining = exponent
    var bit = 0

    while (remaining > 0) {
        if (remaining and 1 == 1) {
            result = result * current % mod
            Drill.visit(bit, 1)
            Drill.write(0, result.toInt())
        }
        current = current * current % mod
        remaining = remaining shr 1
        bit += 1
    }
    return result.toInt()
}
""",
    mutants=[
        ("int-multiply--overflows", "MISSING_EDGE_CASE",
         "Int 로 곱해 중간값이 넘친다.",
         """
fun powerMod(base: Int, exponent: Int): Int {
    val mod = 1_000_000_007
    var result = 1
    var current = base % mod
    var remaining = exponent
    while (remaining > 0) {
        if (remaining and 1 == 1) result = result * current % mod
        current = current * current % mod
        remaining = remaining shr 1
    }
    return result
}
"""),
        ("zero-exponent--returns-zero", "MISSING_EDGE_CASE",
         "지수가 0 일 때 1 이 아니라 0 을 돌려준다.",
         """
fun powerMod(base: Int, exponent: Int): Int {
    val mod = 1_000_000_007L
    if (exponent == 0) return 0
    var result = 1L
    var current = base.toLong() % mod
    var remaining = exponent
    while (remaining > 0) {
        if (remaining and 1 == 1) result = result * current % mod
        current = current * current % mod
        remaining = remaining shr 1
    }
    return result.toInt()
}
"""),
        ("linear-multiply--too-slow", "PERFORMANCE",
         "지수만큼 곱한다. 작은 지수는 통과한다.",
         """
fun powerMod(base: Int, exponent: Int): Int {
    val mod = 1_000_000_007L
    var result = 1L
    for (step in 0 until exponent) {
        Drill.write(step, (result % 1_000_000).toInt())
        result = result * base % mod
    }
    return result.toInt()
}
"""),
    ],
))


# --- 23. 배열 전체의 최대공약수 ----------------------------------------------

def _gcd_all(nums):
    from math import gcd
    out = 0
    for value in nums:
        out = gcd(out, abs(value))
    return out


PROBLEMS.append(Problem(
    id="gcd-of-array",
    title="배열 전체의 최대공약수",
    summary="""
정수 배열 `nums` 의 모든 원소를 나누는 가장 큰 양의 정수를 반환한다. 음수는 절댓값으로
본다. 원소가 모두 `0` 이면 `0` 을 반환한다.
""",
    notes="""
`gcd(0, x) = x` 라는 성질을 쓰면 0 을 시작값으로 두고 그냥 접어 갈 수 있다. 최댓값부터
하나씩 나눠 보는 방법은 값이 커지면 무너진다.
""",
    drill_doc="""
Drill.visit(index, value)  // 원소를 봤다
Drill.write(0, current)    // 지금까지의 최대공약수
""",
    constraints="""
- `1 <= nums.size <= 200_000`
- `-2*10^9 <= nums[i] <= 2*10^9`
""",
    signature=dict(name="gcdOfArray", parameters=[("nums", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(),
    reference=_gcd_all,
    cases={
        "sample": [
            ("01", [[12, 18, 24]]),
            ("02", [[7, 13]]),
        ],
        "boundary": [
            ("01-single", [[42]]),
            ("02-all-zero", [[0, 0, 0]]),
            # 0 이 **가운데** 있다. 앞에 있으면 첫 값으로 시작하는 구현도 우연히 맞으므로,
            # gcd(0, x) = x 를 실제로 쓰는지 보려면 중간에 둬야 한다.
            ("03-zero-in-middle", [[15, 0, 25]]),
            ("04-negative", [[-12, 18]]),
            ("05-all-negative", [[-8, -12, -20]]),
            ("06-coprime", [[9, 28]]),
            ("07-large", [[1000000000, 500000000]]),
        ],
        "hidden": [
            ("01-multiples", [[6, 12, 18, 24, 30]]),
            ("02-one-present", [[1, 999983, 123456]]),
            ("03-same-value", [[77] * 30]),
        ],
        # 최댓값부터 하나씩 나눠 보는 풀이는 값이 클 때 무너진다.
        "performance": [
            ("01-small", [[v * 6 for v in randoms(3000, 1, 100000, salt=91)]]),
            ("02-medium", [[v * 6 for v in randoms(50000, 1, 100000, salt=92)]]),
            # 서로소인 두 큰 값. 가장 작은 값부터 나눠 보는 풀이는 1 까지 20억 번을
            # 헛돈다. 연속한 두 정수라 서로소인 것이 보장된다.
            ("03-large-coprime", [[1999999973, 1999999972] * 100000]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 유클리드 호제법을 접어 간다.
//
// gcd(0, x) = x 이므로 0 에서 시작하면 특별한 경우가 없다. 모두 0 이면 결과도 0 이고,
// 그것이 이 문제가 정의한 답이다.
fun gcdOfArray(nums: IntArray): Int {
    fun gcd(first: Int, second: Int): Int {
        var a = first
        var b = second
        while (b != 0) {
            val next = a % b
            a = b
            b = next
        }
        return a
    }

    var current = 0
    for (index in nums.indices) {
        Drill.visit(index, nums[index])
        val value = if (nums[index] < 0) -nums[index] else nums[index]
        current = gcd(current, value)
        Drill.write(0, current)
        // 1 보다 작아질 수 없다. 더 볼 필요가 없다.
        if (current == 1) break
    }
    return current
}
""",
    mutants=[
        ("zero-breaks--returns-zero", "MISSING_EDGE_CASE",
         "0 을 만나면 결과를 0 으로 만든다. gcd(0, x) = x 를 쓰지 않았다.",
         """
fun gcdOfArray(nums: IntArray): Int {
    fun gcd(first: Int, second: Int): Int {
        var a = first; var b = second
        while (b != 0) { val next = a % b; a = b; b = next }
        return a
    }
    var current = if (nums[0] < 0) -nums[0] else nums[0]
    for (index in 1 until nums.size) {
        val value = if (nums[index] < 0) -nums[index] else nums[index]
        if (value == 0) return 0
        current = gcd(current, value)
    }
    return current
}
"""),
        ("keeps-sign--returns-negative", "WRONG_BRANCH",
         "절댓값을 취하지 않아 음수만 있으면 음수를 돌려준다.",
         """
fun gcdOfArray(nums: IntArray): Int {
    fun gcd(first: Int, second: Int): Int {
        var a = first; var b = second
        while (b != 0) { val next = a % b; a = b; b = next }
        return a
    }
    var current = 0
    for (value in nums) current = gcd(current, value)
    return current
}
"""),
        # 뺄셈식 유클리드. 교과서에 나오는 형태라 실제로 자주 쓰는데, 나눗셈식과 달리
        # **값에 비례**한다 — gcd(1, 20억) 하나에 20억 번을 뺀다. 원소마다 그러므로
        # 한도를 자릿수 단위로 넘긴다. 앞의 오답과 달리 머신이 아무리 빨라도 못 넘는다.
        ("subtractive-gcd--linear-in-value", "PERFORMANCE",
         "나눗셈 대신 뺄셈으로 유클리드를 돈다. 값이 크면 값에 비례해 느려진다.",
         """
fun gcdOfArray(nums: IntArray): Int {
    var result = 0
    for (value in nums) {
        var a = if (value < 0) -value else value
        if (a == 0) continue
        var b = result
        if (b == 0) {
            result = a
            continue
        }
        while (a != b) {
            Drill.compare(a, b)
            if (a > b) a -= b else b -= a
        }
        result = a
    }
    return result
}
"""),
    ],
))


# --- 73. 요세푸스 — 마지막에 남는 사람 (큐 시뮬레이션) --------------------------------------

def _josephus(n, k):
    survivor = 0
    for size in range(2, n + 1):
        survivor = (survivor + k) % size
    return survivor + 1


PROBLEMS.append(Problem(
    id="josephus-survivor",
    title="원탁에서 마지막에 남는 사람",
    summary="""
`1` 번부터 `n` 번까지 `n` 명이 원을 이루고 앉아 있다. `1` 번부터 세기 시작해 **k 번째
사람**을 내보내고, 그 다음 사람부터 다시 `1` 로 세어 다시 k 번째를 내보낸다. 마지막까지
남는 한 사람의 번호를 반환한다.

예: `n = 5, k = 2` 이면 `2, 4, 1, 5` 순으로 나가고 `3` 이 남는다.
""",
    notes="""
큐로 그대로 흉내 낼 수 있다 — 앞에서 k-1 명을 뒤로 보내고 k 번째를 내보낸다. 그것은 O(n·k)
다. n 이 크면 다른 관찰이 필요하다: n-1 명일 때의 답을 알면 n 명일 때의 답은 거기에 k 를
더해 n 으로 나눈 나머지다. 사람이 하나 나간 뒤의 원은 "번호를 k 만큼 돌린 n-1 명의 원"이다.
""",
    drill_doc="""
Drill.enqueue(person)         // 뒤로 보냈다
Drill.dequeue(person)         // 내보냈다
Drill.write(size, survivor)   // size 명일 때의 답
""",
    constraints="""
- `1 <= n <= 1_000_000`
- `1 <= k <= 1_000_000`
""",
    signature=dict(name="survivor", parameters=[("n", "INT"), ("k", "INT")], returns="INT"),
    groups=perf_groups(),
    reference=_josephus,
    cases={
        "sample": [
            ("01", [5, 2]),
            ("02", [7, 3]),
        ],
        "boundary": [
            ("01-single", [1, 1]),
            ("02-single-big-k", [1, 1000]),
            # k = 1 이면 차례로 나가고 마지막 사람이 남는다.
            ("03-k-one", [6, 1]),
            # k 가 n 보다 크다. 원을 여러 바퀴 돈다.
            ("04-k-larger-than-n", [3, 7]),
            ("05-k-equals-n", [4, 4]),
            ("06-two", [2, 2]),
        ],
        "hidden": [
            ("01-small", [10, 4]),
            ("02-medium", [100, 13]),
            ("03-big-k", [50, 999983]),
            ("04-classic", [41, 3]),
        ],
        "performance": [
            ("01-small", [20000, 3]),
            ("02-medium", [200000, 777]),
            ("03-large", [1000000, 1000000]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). n-1 명의 답에서 n 명의 답으로.
fun survivor(n: Int, k: Int): Int {
    var result = 0
    for (size in 2..n) {
        result = (result + k) % size
        Drill.write(size, result)
    }
    return result + 1
}
""",
    mutants=[
        ("zero-indexed-answer", "OFF_BY_ONE",
         "0 부터 센 자리를 그대로 답한다. 사람의 번호는 1 부터다.",
         """
fun survivor(n: Int, k: Int): Int {
    var result = 0
    for (size in 2..n) result = (result + k) % size
    return result
}
"""),
        ("recurrence-off-by-one", "WRONG_BRANCH",
         "점화식에 k 가 아니라 k-1 을 더한다. 내보낸 사람 다음부터 다시 센다는 것을 잊었다.",
         """
fun survivor(n: Int, k: Int): Int {
    var result = 0
    for (size in 2..n) result = (result + k - 1) % size
    return result + 1
}
"""),
        ("queue-simulation--n-times-k", "PERFORMANCE",
         "큐로 k-1 명씩 뒤로 보내며 흉내 낸다. O(n·k).",
         """
fun survivor(n: Int, k: Int): Int {
    val queue = ArrayDeque<Int>()
    for (i in 1..n) queue.addLast(i)
    while (queue.size > 1) {
        repeat(k - 1) { val p = queue.removeFirst(); Drill.enqueue(p); queue.addLast(p) }
        Drill.dequeue(queue.removeFirst())
    }
    return queue.first()
}
"""),
    ],
))


# --- 74. n! 의 끝에 붙는 0 의 개수 --------------------------------------------------------

def _trailing_zeros(n):
    count = 0
    power = 5
    while power <= n:
        count += n // power
        power *= 5
    return count


PROBLEMS.append(Problem(
    id="factorial-trailing-zeros",
    title="n! 의 끝에 붙는 0 의 개수",
    summary="""
0 이상의 정수 `n` 이 주어진다. `n!` (n 의 계승)을 십진수로 적었을 때 **끝에 연달아 붙는 0 의
개수**를 반환한다. `n!` 자체를 계산해서는 안 된다 — 20! 만 해도 64비트를 넘는다.

예: `5! = 120` 이므로 `1`, `10! = 3628800` 이므로 `2`, `3! = 6` 이므로 `0` 이다.
""",
    notes="""
끝의 0 하나는 인수 10 하나이고, 10 은 2 × 5 다. 1 부터 n 까지 곱하면 2 는 5 보다 훨씬 많으므로
답은 **5 가 몇 번 곱해졌나**다. 5 의 배수는 n/5 개인데, 25 는 5 를 두 번, 125 는 세 번 갖고
있다 — 그래서 n/5 + n/25 + n/125 + … 이다.
""",
    drill_doc="""
Drill.visit(power, count)     // 5 의 거듭제곱 하나를 셌다
""",
    constraints="""
- `0 <= n <= 2^31 - 1`
""",
    signature=dict(name="trailingZeros", parameters=[("n", "INT")], returns="INT"),
    groups=standard_groups(),
    reference=_trailing_zeros,
    cases={
        "sample": [
            ("01", [5]),
            ("02", [10]),
        ],
        "boundary": [
            ("01-zero", [0]),
            ("02-one", [1]),
            ("03-four", [4]),
            # 25 는 5 를 두 번 갖는다. n/5 만 세면 하나 모자란다.
            ("04-twenty-five", [25]),
            ("05-twenty-four", [24]),
            # 125 는 세 번.
            ("06-one-twenty-five", [125]),
            # 5 의 거듭제곱을 곱해 가다 Int 를 넘길 수 있는 자리.
            ("07-max-int", [2147483647]),
        ],
        "hidden": [
            ("01-hundred", [100]),
            ("02-thousand", [1000]),
            ("03-random", [randoms(1, 0, 1000000, salt=3501)[0]]),
            ("04-large", [1000000000]),
            ("05-just-below-power", [3124]),
            ("06-power", [3125]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 5 의 거듭제곱마다 배수의 수를 더한다.
fun trailingZeros(n: Int): Int {
    var count = 0
    var power = 5L
    while (power <= n) {
        count += (n / power).toInt()
        Drill.visit(power.toInt(), count)
        power *= 5
    }
    return count
}
""",
    mutants=[
        ("fives-only--once", "WRONG_ALGORITHM",
         "5 의 배수만 센다. 25 가 5 를 두 번 갖는다는 것을 잊었다.",
         """
fun trailingZeros(n: Int): Int = n / 5
"""),
        ("counts-tens", "WRONG_BRANCH",
         "10 의 배수를 센다. 2 × 5 처럼 따로 떨어진 인수를 놓친다.",
         """
fun trailingZeros(n: Int): Int {
    var count = 0
    var power = 10L
    while (power <= n) { count += (n / power).toInt(); power *= 10 }
    return count
}
"""),
        ("int-power--overflows", "MISSING_EDGE_CASE",
         "5 의 거듭제곱을 Int 로 곱한다. 큰 n 에서 넘친 값이 양수로 돌아와 엉뚱한 몫을 더한다.",
         """
fun trailingZeros(n: Int): Int {
    var count = 0
    var power = 5
    while (power <= n && power > 0) { count += n / power; power *= 5 }
    return count
}
"""),
    ],
))


# --- 75. 정수 뒤집기 --------------------------------------------------------------------

def _reverse_integer(n):
    sign = -1 if n < 0 else 1
    digits = int(str(abs(n))[::-1])
    result = sign * digits
    return result if -2 ** 31 <= result <= 2 ** 31 - 1 else 0


PROBLEMS.append(Problem(
    id="reverse-integer",
    title="정수 뒤집기",
    summary="""
32비트 정수 `n` 이 주어진다. 십진 자릿수를 **뒤집은** 정수를 반환한다. 부호는 그대로다.
뒤집은 수가 32비트 정수 범위(`-2^31 .. 2^31 - 1`)를 벗어나면 `0` 을 반환한다.

예: `123` → `321`, `-120` → `-21`, `1534236469` → `0` (뒤집으면 9646324351 로 범위 밖).
""",
    notes="""
마지막 자리를 떼어 결과의 뒤에 붙이는 일을 되풀이하면 된다. 문제는 **넘침**이다 — 결과에
자리를 하나 더 붙이기 전에 넘칠지 확인하거나, 더 넓은 정수형으로 계산한 뒤 범위를 본다.
뒤집은 결과가 `Int` 안에 들어가는지를 `Int` 로만 계산해서는 알 수 없다.
""",
    drill_doc="""
Drill.visit(digit, result)    // 자리 하나를 떼어 붙였다
""",
    constraints="""
- `-2^31 <= n <= 2^31 - 1`
""",
    signature=dict(name="reverseInteger", parameters=[("n", "INT")], returns="INT"),
    groups=standard_groups(),
    reference=_reverse_integer,
    cases={
        "sample": [
            ("01", [123]),
            ("02", [-120]),
        ],
        "boundary": [
            ("01-zero", [0]),
            ("02-single-digit", [7]),
            ("03-negative-single", [-3]),
            # 끝의 0 은 사라진다.
            ("04-trailing-zeros", [1000]),
            # 뒤집으면 딱 범위 밖.
            ("05-overflow-positive", [1534236469]),
            ("06-overflow-negative", [-1563847412]),
            # 뒤집으면 범위 안에 아슬아슬하게 든다.
            ("07-fits-barely", [1463847412]),
            ("08-max-int", [2147483647]),
            ("09-min-int", [-2147483648]),
        ],
        "hidden": [
            ("01-palindrome", [12321]),
            ("02-negative-palindrome", [-4554]),
            ("03-random", [randoms(1, -1000000000, 1000000000, salt=3601)[0]]),
            ("04-nine-digits", [987654321]),
            ("05-ten-digits-fit", [1000000003]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). Long 으로 뒤집고 범위를 본다.
fun reverseInteger(n: Int): Int {
    var rest = n.toLong()
    var result = 0L
    while (rest != 0L) {
        val digit = rest % 10
        result = result * 10 + digit
        Drill.visit(digit.toInt(), result.toInt())
        rest /= 10
    }
    return if (result < Int.MIN_VALUE || result > Int.MAX_VALUE) 0 else result.toInt()
}
""",
    mutants=[
        ("int-arithmetic--overflows", "MISSING_EDGE_CASE",
         "Int 로 뒤집고 범위를 보지 않는다. 넘치는 입력에서 엉뚱한 수를 낸다.",
         """
fun reverseInteger(n: Int): Int {
    var rest = n
    var result = 0
    while (rest != 0) { result = result * 10 + rest % 10; rest /= 10 }
    return result
}
"""),
        ("drops-sign", "WRONG_BRANCH",
         "절댓값을 뒤집고 부호를 잊는다.",
         """
fun reverseInteger(n: Int): Int {
    var rest = Math.abs(n.toLong())
    var result = 0L
    while (rest != 0L) { result = result * 10 + rest % 10; rest /= 10 }
    return if (result > Int.MAX_VALUE) 0 else result.toInt()
}
"""),
        ("string-reverse--keeps-zeros", "WRONG_ALGORITHM",
         "문자열로 뒤집어 그대로 둔다. 앞에 온 0 을 지우지 못하는 것이 아니라, 부호 문자가 끝으로 간다.",
         """
fun reverseInteger(n: Int): Int {
    val reversed = n.toString().reversed()
    return reversed.toLongOrNull()?.let { if (it < Int.MIN_VALUE || it > Int.MAX_VALUE) 0 else it.toInt() } ?: 0
}
"""),
    ],
))


# --- 90. 소인수의 합 (제약을 읽는 문제) ----------------------------------------------------

def _prime_factor_sum(n):
    total = 0
    p = 2
    while p * p <= n:
        if n % p == 0:
            total += p
            while n % p == 0:
                n //= p
        p += 1
    if n > 1:
        total += n
    return total


def _prime_factor_sums(nums):
    return [_prime_factor_sum(n) for n in nums]


def _near_primes(count, salt):
    """큰 소수 근처의 수들. 나누어 떨어지는 작은 인수가 없어 시험 나눗셈이 끝까지 간다."""
    big = [999999937, 999999929, 999999893, 999999883, 999999797, 999999761, 999999757, 999999751]
    picks = randoms(count, 0, len(big) - 1, salt=salt)
    return [big[i] for i in picks]


PROBLEMS.append(Problem(
    id="prime-factor-sums",
    title="소인수의 합",
    summary="""
정수 배열 `nums` 가 주어진다. 각 수에 대해 **서로 다른 소인수의 합**을 구해 같은 순서로
반환한다. `1` 의 답은 `0` 이다.

예: `12 = 2² · 3` 이라 `5`, `7` 은 `7`, `1` 은 `0`.
""",
    notes="""
`2` 부터 `n` 까지 나눠 보면 수 하나에 10⁹ 번이다. **제약을 읽는다** — `p · p > n` 이면
남은 `n` 은 소수거나 1 이다. 그래서 √n 까지만 나누면 되고, 소인수를 찾을 때마다 그것으로
`n` 을 다 나누면 다음 나누어떨어지는 수는 저절로 소수다.
""",
    drill_doc="""
Drill.visit(i, n)             // i 번째 수
Drill.compare(p, n)           // p 로 나눠 봤다
Drill.write(i, total)         // 지금까지의 합
""",
    constraints="""
- `1 <= nums.size <= 2_000`
- `1 <= nums[i] <= 10^9`
""",
    signature=dict(name="primeFactorSums", parameters=[("nums", "INT_ARRAY")], returns="INT_ARRAY"),
    groups=perf_groups(),
    reference=_prime_factor_sums,
    cases={
        "sample": [
            ("01", [[12, 7, 1]]),
            ("02", [[30]]),
        ],
        "boundary": [
            ("01-one", [[1]]),
            ("02-two", [[2]]),
            # 소수의 거듭제곱. 소인수는 하나뿐이고 한 번만 더한다.
            ("03-prime-power", [[1024, 243]]),
            # 큰 소수. √n 까지 가도 인수가 없다 — 남은 n 이 답이다.
            ("04-big-prime", [[999999937]]),
            # 두 소수의 곱. √n 바로 아래에서 첫 인수가 나오고, 남은 것이 둘째다.
            ("05-product-of-two-primes", [[31607 * 31627]]),
            # 2 의 거듭제곱 곱하기 큰 소수. 2 로 다 나누고 남은 것이 소수다.
            ("06-two-times-big-prime", [[2 * 499999993]]),
            # 소수의 제곱. √n 에서 정확히 나누어떨어진다 — 경계를 "미만"으로 잡으면 놓친다.
            ("07-prime-square", [[49, 961, 31607 * 31607]]),
        ],
        "hidden": [
            ("01-random-small", [randoms(50, 1, 1000, salt=4601)]),
            ("02-random-medium", [randoms(100, 1, 1000000, salt=4602)]),
            ("03-random-big", [randoms(60, 1, 1000000000, salt=4603)]),
        ],
        "performance": [
            ("01-small", [_near_primes(200, salt=4604)]),
            ("02-medium", [_near_primes(800, salt=4605)]),
            ("03-large", [_near_primes(2000, salt=4606)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). √n 까지의 시험 나눗셈.
fun primeFactorSums(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    for ((i, value) in nums.withIndex()) {
        Drill.visit(i, value)
        var n = value
        var total = 0L
        var p = 2L
        while (p * p <= n) {
            if (n % p == 0L) {
                total += p
                while (n % p == 0L) n = (n / p).toInt()
            }
            p += 1
        }
        if (n > 1) total += n
        out[i] = total.toInt()
        Drill.write(i, out[i])
    }
    return out
}
""",
    mutants=[
        ("counts-multiplicity", "WRONG_BRANCH",
         "같은 소인수를 나올 때마다 더한다. 서로 다른 소인수만 더해야 한다.",
         """
fun primeFactorSums(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    for ((i, value) in nums.withIndex()) {
        var n = value; var total = 0L; var p = 2L
        while (p * p <= n) {
            while (n % p == 0L) { total += p; n = (n / p).toInt() }
            p += 1
        }
        if (n > 1) total += n
        out[i] = total.toInt()
    }
    return out
}
"""),
        ("drops-remaining-prime", "MISSING_EDGE_CASE",
         "√n 까지 나눈 뒤 남은 n 을 더하지 않는다. 큰 소인수가 빠진다.",
         """
fun primeFactorSums(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    for ((i, value) in nums.withIndex()) {
        var n = value; var total = 0L; var p = 2L
        while (p * p <= n) {
            if (n % p == 0L) { total += p; while (n % p == 0L) n = (n / p).toInt() }
            p += 1
        }
        out[i] = total.toInt()
    }
    return out
}
"""),
        ("strict-sqrt-bound", "OFF_BY_ONE",
         "p · p < n 인 동안만 나눈다. n 이 소수의 제곱이면 그 소수를 못 보고 n 자체를 더한다.",
         """
fun primeFactorSums(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    for ((i, value) in nums.withIndex()) {
        var n = value; var total = 0L; var p = 2L
        while (p * p < n) {
            if (n % p == 0L) { total += p; while (n % p == 0L) n = (n / p).toInt() }
            p += 1
        }
        if (n > 1) total += n
        out[i] = total.toInt()
    }
    return out
}
"""),
        ("divides-up-to-n", "PERFORMANCE",
         "2 부터 n 까지 전부 나눠 본다. 수 하나에 10⁹ 번.",
         """
fun primeFactorSums(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    for ((i, value) in nums.withIndex()) {
        var n = value; var total = 0L
        var p = 2
        while (p <= n) {
            if (n % p == 0) { total += p; while (n % p == 0) n /= p; Drill.compare(p, n) }
            p += 1
        }
        out[i] = total.toInt()
    }
    return out
}
"""),
    ],
))
