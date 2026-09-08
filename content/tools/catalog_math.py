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
    for (step in 0 until exponent) result = result * base % mod
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
            if (a > b) a -= b else b -= a
        }
        result = a
    }
    return result
}
"""),
    ],
))
