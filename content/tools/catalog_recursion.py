"""재귀·백트래킹·문자열 (역량: 정확성 — 가지치기가 답을 놓치지 않는가).

백트래킹의 오답은 대개 **가지를 잘못 치거나 되돌리기를 빠뜨리는 것**이다. 오답도 그
모양으로 만든다. 입력은 지수 탐색이 제한 안에 끝나는 크기로 묶는다 — 그것이 문제의
전제이고, 그 전제를 읽는 것이 곧 조건 추출이다.
"""

from author import Problem, standard_groups, perf_groups, randoms

PROBLEMS = []


# --- 1. 합이 k 인 부분집합의 수 -------------------------------------------------

def _subset_count(nums, target):
    count = 0
    def go(i, remaining):
        nonlocal count
        if i == len(nums):
            if remaining == 0:
                count += 1
            return
        go(i + 1, remaining - nums[i])
        go(i + 1, remaining)
    go(0, target)
    return count


PROBLEMS.append(Problem(
    id="subset-sum-count",
    title="합이 k 인 부분집합의 수",
    summary="""
정수 배열 `nums` 와 정수 `target` 이 주어진다. 원소의 합이 정확히 `target` 인 **부분집합**의
개수를 반환한다. 빈 부분집합도 부분집합이다 — `target` 이 `0` 이면 빈 집합이 하나 세어진다.

원소는 음수일 수 있고, 같은 값이 여러 개면 서로 다른 원소로 센다.
""",
    notes="""
`nums.size <= 20` 이라 2^20 가지를 다 세어도 된다. 원소마다 "넣는다/안 넣는다"로 갈라지는
재귀 하나면 되고, 남은 합으로 가지치기를 하고 싶어지지만 **음수가 있으면 가지치기가
답을 버린다.**
""",
    drill_doc="""
Drill.call("i=" + i)           // i 번째 원소 앞에서 갈라졌다
Drill.ret("i=" + i, count)     // 그 가지에서 찾은 개수를 돌려줬다
""",
    constraints="""
- `0 <= nums.size <= 20`
- `-1000 <= nums[i] <= 1000`, `-10^5 <= target <= 10^5`
""",
    signature=dict(name="subsetCount", parameters=[("nums", "INT_ARRAY"), ("target", "INT")], returns="INT"),
    groups=standard_groups(),
    reference=_subset_count,
    cases={
        "sample": [
            ("01", [[1, 2, 3], 3]),
            ("02", [[2, 2, 2], 4]),
        ],
        "boundary": [
            # 빈 배열. target 0 이면 빈 집합 하나.
            ("01-empty-zero", [[], 0]),
            ("02-empty-nonzero", [[], 5]),
            # 음수가 섞이면 "남은 합이 음수면 가지치기"가 틀린다.
            ("03-negative", [[-1, 1, 2], 1]),
            # target 0 인데 원소로 0 을 만들 수 있다. 빈 집합 + 나머지.
            ("04-zero-target", [[1, -1, 0], 0]),
            # 최대 크기, 전부 같은 값.
            ("05-all-same-max", [[1] * 20, 10]),
        ],
        "hidden": [
            ("01-random", [randoms(18, -20, 20, salt=971), 7]),
            ("02-no-way", [[5, 10, 15], 3]),
            ("03-large-values", [[1000, -1000, 999, 1], 999]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 원소마다 둘로 갈라지는 완전탐색. 2^20 은 충분히 작다.
fun subsetCount(nums: IntArray, target: Int): Int {
    fun go(i: Int, remaining: Int): Int {
        Drill.call("i=$i")
        val found = if (i == nums.size) {
            if (remaining == 0) 1 else 0
        } else {
            go(i + 1, remaining - nums[i]) + go(i + 1, remaining)
        }
        Drill.ret("i=$i", found)
        return found
    }
    return go(0, target)
}
""",
    mutants=[
        ("prunes-negative--breaks-with-negatives", "WRONG_BRANCH",
         "남은 합이 음수면 가지를 친다. 음수 원소가 있으면 그 뒤에서 다시 커질 수 있다.",
         """
fun subsetCount(nums: IntArray, target: Int): Int {
    fun go(i: Int, remaining: Int): Int {
        if (remaining < 0) return 0
        if (i == nums.size) return if (remaining == 0) 1 else 0
        return go(i + 1, remaining - nums[i]) + go(i + 1, remaining)
    }
    return go(0, target)
}
"""),
        ("excludes-empty--nonempty-only", "MISSING_EDGE_CASE",
         "빈 부분집합을 세지 않는다. target 이 0 일 때 하나 적다.",
         """
fun subsetCount(nums: IntArray, target: Int): Int {
    fun go(i: Int, remaining: Int, taken: Int): Int {
        if (i == nums.size) return if (remaining == 0 && taken > 0) 1 else 0
        return go(i + 1, remaining - nums[i], taken + 1) + go(i + 1, remaining, taken)
    }
    return go(0, target, 0)
}
"""),
        ("stops-at-first--returns-early", "WRONG_BRANCH",
         "합이 맞는 순간 그 가지 아래를 더 보지 않는다. 0 을 더해 같은 합이 되는 집합을 놓친다.",
         """
fun subsetCount(nums: IntArray, target: Int): Int {
    fun go(i: Int, remaining: Int): Int {
        if (remaining == 0) return 1
        if (i == nums.size) return 0
        return go(i + 1, remaining - nums[i]) + go(i + 1, remaining)
    }
    return go(0, target)
}
"""),
    ],
))


# --- 2. N-퀸 -----------------------------------------------------------------

def _queens(n):
    count = 0
    cols = set(); d1 = set(); d2 = set()
    def go(r):
        nonlocal count
        if r == n:
            count += 1
            return
        for c in range(n):
            if c in cols or (r - c) in d1 or (r + c) in d2:
                continue
            cols.add(c); d1.add(r - c); d2.add(r + c)
            go(r + 1)
            cols.remove(c); d1.remove(r - c); d2.remove(r + c)
    go(0)
    return count


PROBLEMS.append(Problem(
    id="n-queens-count",
    title="N-퀸의 해 개수",
    summary="""
`n × n` 체스판에 퀸 `n` 개를 서로 공격하지 못하게 놓는 방법의 수를 반환한다. 퀸은 같은
행·열·대각선 위의 말을 공격한다.
""",
    notes="""
행마다 하나씩 놓으므로 행 충돌은 저절로 없다. 열과 두 대각선을 집합으로 들고, 놓았다가
되돌린다. **되돌리기를 빠뜨리면** 형제 가지가 오염된다.
""",
    drill_doc="""
Drill.call("row=" + r)         // r 행에 놓을 자리를 찾는다
Drill.push(c)                  // c 열에 놓았다
Drill.pop(c)                   // 되돌렸다
Drill.match(r, c)              // 판을 완성했다
""",
    constraints="""
- `1 <= n <= 9`
""",
    signature=dict(name="queens", parameters=[("n", "INT")], returns="INT"),
    groups=standard_groups(),
    reference=_queens,
    cases={
        "sample": [
            ("01", [4]),
            ("02", [1]),
        ],
        "boundary": [
            # 해가 없는 크기 둘.
            ("01-two", [2]),
            ("02-three", [3]),
            ("03-five", [5]),
            ("04-six", [6]),
        ],
        "hidden": [
            ("01-seven", [7]),
            ("02-eight", [8]),
            ("03-nine", [9]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 행마다 하나, 열·두 대각선 표시, 되돌리기.
fun queens(n: Int): Int {
    val cols = BooleanArray(n)
    val diag1 = BooleanArray(2 * n)
    val diag2 = BooleanArray(2 * n)
    var count = 0
    fun go(r: Int) {
        Drill.call("row=$r")
        if (r == n) { count += 1; Drill.match(r, count); return }
        for (c in 0 until n) {
            if (cols[c] || diag1[r - c + n] || diag2[r + c]) continue
            cols[c] = true; diag1[r - c + n] = true; diag2[r + c] = true
            Drill.push(c)
            go(r + 1)
            Drill.pop(c)
            cols[c] = false; diag1[r - c + n] = false; diag2[r + c] = false
        }
    }
    go(0)
    return count
}
""",
    mutants=[
        ("no-undo--leaks-marks", "WRONG_BRANCH",
         "되돌리기를 빠뜨린다. 한 가지가 남긴 표시가 형제 가지를 막는다.",
         """
fun queens(n: Int): Int {
    val cols = BooleanArray(n); val diag1 = BooleanArray(2 * n); val diag2 = BooleanArray(2 * n)
    var count = 0
    fun go(r: Int) {
        if (r == n) { count += 1; return }
        for (c in 0 until n) {
            if (cols[c] || diag1[r - c + n] || diag2[r + c]) continue
            cols[c] = true; diag1[r - c + n] = true; diag2[r + c] = true
            go(r + 1)
        }
    }
    go(0)
    return count
}
"""),
        ("one-diagonal--forgets-anti", "MISSING_EDGE_CASE",
         "대각선을 하나만 본다. 반대 방향 대각선의 공격을 허용한다.",
         """
fun queens(n: Int): Int {
    val cols = BooleanArray(n); val diag1 = BooleanArray(2 * n)
    var count = 0
    fun go(r: Int) {
        if (r == n) { count += 1; return }
        for (c in 0 until n) {
            if (cols[c] || diag1[r - c + n]) continue
            cols[c] = true; diag1[r - c + n] = true
            go(r + 1)
            cols[c] = false; diag1[r - c + n] = false
        }
    }
    go(0)
    return count
}
"""),
        ("stops-at-first--finds-one", "WRONG_BRANCH",
         "해를 하나 찾으면 멈춘다. 있느냐를 묻는 문제로 읽었다.",
         """
fun queens(n: Int): Int {
    val cols = BooleanArray(n); val diag1 = BooleanArray(2 * n); val diag2 = BooleanArray(2 * n)
    fun go(r: Int): Boolean {
        if (r == n) return true
        for (c in 0 until n) {
            if (cols[c] || diag1[r - c + n] || diag2[r + c]) continue
            cols[c] = true; diag1[r - c + n] = true; diag2[r + c] = true
            if (go(r + 1)) return true
            cols[c] = false; diag1[r - c + n] = false; diag2[r + c] = false
        }
        return false
    }
    return if (go(0)) 1 else 0
}
"""),
    ],
))


# --- 3. 가장 긴 회문 부분 문자열의 길이 -----------------------------------------

def _longest_palindrome(text):
    best = 0
    n = len(text)
    for center in range(n):
        for lo, hi in ((center, center), (center, center + 1)):
            while lo >= 0 and hi < n and text[lo] == text[hi]:
                lo -= 1
                hi += 1
            best = max(best, hi - lo - 1)
    return best


PROBLEMS.append(Problem(
    id="longest-palindrome-substring",
    title="가장 긴 회문 부분 문자열",
    summary="""
문자열 `text` 가 주어진다. **연속한** 부분 문자열 중 회문인 것의 최대 길이를 반환한다.
빈 문자열이면 `0` 이다.
""",
    notes="""
회문은 중심에서 양쪽으로 자란다. 중심이 될 자리는 글자 n 개와 글자 사이 n-1 개, 2n-1 개다.
짝수 길이 회문의 중심을 빠뜨리는 것이 가장 흔한 실수다.
""",
    drill_doc="""
Drill.pointer("lo", lo)        // 왼쪽으로 넓혔다
Drill.pointer("hi", hi)        // 오른쪽으로 넓혔다
Drill.compare(lo, hi)          // 양끝 글자를 견줬다
Drill.match(lo, hi)            // 최댓값이 갱신됐다
""",
    constraints="""
- `0 <= text.length <= 2_000`
- 영문 소문자
""",
    signature=dict(name="longestPalindrome", parameters=[("text", "STRING")], returns="INT"),
    groups=standard_groups(),
    reference=_longest_palindrome,
    cases={
        "sample": [
            ("01", ["babad"]),
            ("02", ["cbbd"]),
        ],
        "boundary": [
            ("01-empty", [""]),
            ("02-single", ["a"]),
            # 전체가 짝수 회문.
            ("03-even-whole", ["abba"]),
            # 전체가 홀수 회문.
            ("04-odd-whole", ["racecar"]),
            # 회문이 글자 하나뿐.
            ("05-no-repeat", ["abcdef"]),
            # 같은 글자만.
            ("06-all-same", ["aaaaaaa"]),
        ],
        "hidden": [
            ("01-long-mixed", ["abacdfgdcaba"]),
            ("02-even-in-middle", ["xyzabccbazyq"]),
            ("03-max-length", ["ab" * 1000]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 중심 2n-1 개에서 양쪽으로 넓힌다.
fun longestPalindrome(text: String): Int {
    val n = text.length
    var best = 0
    for (center in 0 until n) {
        for (even in 0..1) {
            var lo = center
            var hi = center + even
            while (lo >= 0 && hi < n && text[lo] == text[hi]) {
                Drill.compare(lo, hi)
                lo -= 1
                hi += 1
                Drill.pointer("lo", lo)
                Drill.pointer("hi", hi)
            }
            val length = hi - lo - 1
            if (length > best) { best = length; Drill.match(lo + 1, hi - 1) }
        }
    }
    return best
}
""",
    mutants=[
        ("odd-centers-only--misses-even", "MISSING_EDGE_CASE",
         "글자 위의 중심만 본다. 짝수 길이 회문을 놓친다.",
         """
fun longestPalindrome(text: String): Int {
    val n = text.length
    var best = 0
    for (center in 0 until n) {
        var lo = center; var hi = center
        while (lo >= 0 && hi < n && text[lo] == text[hi]) { lo -= 1; hi += 1 }
        best = maxOf(best, hi - lo - 1)
    }
    return best
}
"""),
        ("length-off-by-one--hi-minus-lo", "OFF_BY_ONE",
         "길이를 hi - lo 로 센다. 넓히기가 한 칸 넘어간 뒤라 하나 크다.",
         """
fun longestPalindrome(text: String): Int {
    val n = text.length
    var best = 0
    for (center in 0 until n) {
        for (even in 0..1) {
            var lo = center; var hi = center + even
            while (lo >= 0 && hi < n && text[lo] == text[hi]) { lo -= 1; hi += 1 }
            best = maxOf(best, hi - lo)
        }
    }
    return best
}
"""),
        ("empty-returns-one--assumes-char", "MISSING_EDGE_CASE",
         "빈 문자열에서 1 을 돌려준다. 글자가 하나는 있다고 가정했다.",
         """
fun longestPalindrome(text: String): Int {
    val n = text.length
    var best = 1
    for (center in 0 until n) {
        for (even in 0..1) {
            var lo = center; var hi = center + even
            while (lo >= 0 && hi < n && text[lo] == text[hi]) { lo -= 1; hi += 1 }
            best = maxOf(best, hi - lo - 1)
        }
    }
    return best
}
"""),
    ],
))


# --- 4. 괄호를 맞추는 최소 삽입 수 ---------------------------------------------

def _min_add(text):
    open_count = 0
    insertions = 0
    for ch in text:
        if ch == "(":
            open_count += 1
        elif open_count > 0:
            open_count -= 1
        else:
            insertions += 1
    return insertions + open_count


PROBLEMS.append(Problem(
    id="min-add-parentheses",
    title="괄호를 맞추는 최소 삽입",
    summary="""
`(` 와 `)` 로만 이루어진 문자열 `text` 가 주어진다. 어디든 괄호를 끼워 넣어 올바른 괄호열로
만들 때 필요한 **최소 삽입 수**를 반환한다.
""",
    notes="""
스택이 필요 없다. 열린 괄호 수 하나만 들고 가면, 닫는 괄호가 왔는데 열린 것이 없으면
여는 괄호 하나를 앞에 끼워야 하고, 끝까지 열린 채 남은 것만큼 닫는 괄호를 뒤에 끼워야
한다.
""",
    drill_doc="""
Drill.push(i)                  // 여는 괄호를 만났다
Drill.pop(i)                   // 짝을 맞췄다
Drill.write(i, insertions)     // 끼워 넣어야 할 수가 늘었다
""",
    constraints="""
- `0 <= text.length <= 100_000`
""",
    signature=dict(name="minAdd", parameters=[("text", "STRING")], returns="INT"),
    groups=standard_groups(),
    reference=_min_add,
    cases={
        "sample": [
            ("01", ["())"]),
            ("02", ["((("]),
        ],
        "boundary": [
            ("01-empty", [""]),
            ("02-balanced", ["(())()"]),
            # 닫는 것만.
            ("03-close-only", [")))"]),
            # 닫힘이 먼저 오고 나중에 열림이 남는다. 둘 다 세야 한다.
            ("04-both-sides", [")("]),
            ("05-nested-broken", ["(()))(("]),
        ],
        "hidden": [
            ("01-alternating", [")(" * 500]),
            ("02-deep", ["(" * 50000 + ")" * 49999]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 열린 수 하나로 센다.
fun minAdd(text: String): Int {
    var open = 0
    var insertions = 0
    for (i in text.indices) {
        if (text[i] == '(') {
            open += 1
            Drill.push(i)
        } else if (open > 0) {
            open -= 1
            Drill.pop(i)
        } else {
            insertions += 1
            Drill.write(i, insertions)
        }
    }
    return insertions + open
}
""",
    mutants=[
        ("forgets-open-tail--counts-closers-only", "MISSING_EDGE_CASE",
         "끝에 열린 채 남은 괄호를 세지 않는다.",
         """
fun minAdd(text: String): Int {
    var open = 0; var insertions = 0
    for (ch in text) {
        if (ch == '(') open += 1 else if (open > 0) open -= 1 else insertions += 1
    }
    return insertions
}
"""),
        ("abs-difference--ignores-order", "WRONG_ALGORITHM",
         "여는 수와 닫는 수의 차이만 본다. `)(` 는 차이가 0 이지만 두 개를 끼워야 한다.",
         """
fun minAdd(text: String): Int {
    var open = 0; var close = 0
    for (ch in text) if (ch == '(') open += 1 else close += 1
    return if (open > close) open - close else close - open
}
"""),
        ("negative-open--lets-counter-drop", "WRONG_BRANCH",
         "열린 수를 음수로 내려가게 둔다. 뒤에 오는 여는 괄호가 앞의 빚을 갚는 것으로 잘못 센다.",
         """
fun minAdd(text: String): Int {
    var open = 0
    for (ch in text) if (ch == '(') open += 1 else open -= 1
    return if (open < 0) -open else open
}
"""),
    ],
))


# --- 5. 배 실을 최소 용량 (답에 대한 이분 탐색) -------------------------------------

def _min_capacity(weights, days):
    def fits(cap):
        used = 1
        load = 0
        for w in weights:
            if load + w > cap:
                used += 1
                load = 0
            load += w
        return used <= days
    lo, hi = max(weights), sum(weights)
    while lo < hi:
        mid = (lo + hi) // 2
        if fits(mid):
            hi = mid
        else:
            lo = mid + 1
    return lo


PROBLEMS.append(Problem(
    id="min-ship-capacity",
    title="배에 실을 최소 용량",
    summary="""
짐의 무게가 `weights` 순서로 주어진다. **순서를 바꾸지 않고** 앞에서부터 차례로 실어
`days` 일 안에 전부 옮겨야 한다. 하루에 한 번 싣고, 하루에 싣는 무게의 합이 배의 용량을
넘을 수 없다. 이것이 가능한 **최소 용량**을 반환한다.
""",
    notes="""
용량이 클수록 날 수가 준다 — 단조롭다. 그래서 답에 대해 이분 탐색할 수 있다: 어떤 용량으로
며칠이 드는지는 한 번 훑어 안다. 용량의 하한은 가장 무거운 짐, 상한은 전체 합이다.
""",
    drill_doc="""
Drill.pointer("lo", lo)        // 탐색 구간의 아래쪽
Drill.pointer("hi", hi)        // 탐색 구간의 위쪽
Drill.compare(mid, days)       // 용량 mid 로 며칠 드는지 봤다
""",
    constraints="""
- `1 <= weights.size <= 100_000`, `1 <= weights[i] <= 500`
- `1 <= days <= weights.size`
""",
    signature=dict(name="minCapacity", parameters=[("weights", "INT_ARRAY"), ("days", "INT")], returns="INT"),
    groups=perf_groups(),
    reference=_min_capacity,
    cases={
        "sample": [
            ("01", [[1, 2, 3, 4, 5, 6, 7, 8, 9, 10], 5]),
            ("02", [[3, 2, 2, 4, 1, 4], 3]),
        ],
        "boundary": [
            # 하루면 전체 합.
            ("01-one-day", [[1, 2, 3], 1]),
            # 날 수가 짐 수와 같으면 가장 무거운 짐.
            ("02-days-equals-count", [[7, 2, 5], 3]),
            # 짐 하나.
            ("03-single", [[42], 1]),
            # 가장 무거운 짐이 답을 정한다 — 합을 나눈 값보다 크다.
            ("04-heavy-one", [[1, 1, 1, 10, 1, 1], 3]),
        ],
        "hidden": [
            ("01-random", [randoms(200, 1, 500, salt=981), 20]),
            ("02-all-same", [[5] * 100, 7]),
        ],
        "performance": [
            # 날 수가 적을수록 답이 커지고, 하나씩 올려 보는 풀이는 그만큼 오래 걸린다.
            ("01-small", [randoms(3000, 1, 500, salt=982), 5]),
            ("02-medium", [randoms(30000, 1, 500, salt=983), 20]),
            ("03-large", [randoms(100000, 1, 500, salt=984), 50]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 답에 대한 이분 탐색.
fun minCapacity(weights: IntArray, days: Int): Int {
    fun daysNeeded(cap: Int): Int {
        var used = 1
        var load = 0
        for (w in weights) {
            if (load + w > cap) { used += 1; load = 0 }
            load += w
        }
        return used
    }
    var lo = weights.max()
    var hi = weights.sum()
    while (lo < hi) {
        val mid = lo + (hi - lo) / 2
        Drill.pointer("lo", lo)
        Drill.pointer("hi", hi)
        Drill.compare(mid, days)
        if (daysNeeded(mid) <= days) hi = mid else lo = mid + 1
    }
    return lo
}
""",
    mutants=[
        ("lower-bound-zero--allows-impossible", "MISSING_EDGE_CASE",
         "하한을 0 으로 잡는다. 가장 무거운 짐보다 작은 용량을 답으로 낼 수 있다.",
         """
fun minCapacity(weights: IntArray, days: Int): Int {
    fun daysNeeded(cap: Int): Int {
        var used = 1; var load = 0
        for (w in weights) { if (load + w > cap) { used += 1; load = 0 }; load += w }
        return used
    }
    var lo = 1; var hi = weights.sum()
    while (lo < hi) {
        val mid = lo + (hi - lo) / 2
        if (daysNeeded(mid) <= days) hi = mid else lo = mid + 1
    }
    return lo
}
"""),
        ("upper-bias--returns-mid-plus", "OFF_BY_ONE",
         "가능한 용량을 찾았을 때 hi 를 mid - 1 로 줄인다. 답 자체를 구간에서 밀어낸다.",
         """
fun minCapacity(weights: IntArray, days: Int): Int {
    fun daysNeeded(cap: Int): Int {
        var used = 1; var load = 0
        for (w in weights) { if (load + w > cap) { used += 1; load = 0 }; load += w }
        return used
    }
    var lo = weights.max(); var hi = weights.sum()
    while (lo < hi) {
        val mid = lo + (hi - lo) / 2
        if (daysNeeded(mid) <= days) hi = mid - 1 else lo = mid + 1
    }
    return lo
}
"""),
        ("sorts-weights--breaks-order", "WRONG_ALGORITHM",
         "짐을 정렬해 싣는다. 순서를 바꾸면 안 된다는 조건을 무시했다.",
         """
fun minCapacity(weights: IntArray, days: Int): Int {
    val sorted = weights.sortedArray()
    fun daysNeeded(cap: Int): Int {
        var used = 1; var load = 0
        for (w in sorted) { if (load + w > cap) { used += 1; load = 0 }; load += w }
        return used
    }
    var lo = sorted.last(); var hi = sorted.sum()
    while (lo < hi) {
        val mid = lo + (hi - lo) / 2
        if (daysNeeded(mid) <= days) hi = mid else lo = mid + 1
    }
    return lo
}
"""),
        ("linear-scan--tries-every-capacity", "PERFORMANCE",
         "용량을 하한부터 하나씩 올려 본다. 답까지 O(합 × n).",
         """
fun minCapacity(weights: IntArray, days: Int): Int {
    fun daysNeeded(cap: Int): Int {
        var used = 1; var load = 0
        for (i in weights.indices) { Drill.visit(i, weights[i]); if (load + weights[i] > cap) { used += 1; load = 0 }; load += weights[i] }
        return used
    }
    var cap = weights.max()
    while (daysNeeded(cap) > days) cap += 1
    return cap
}
"""),
    ],
))


# --- 68. 격자에서 수열 경로 찾기 (백트래킹) --------------------------------------------------

def _path_exists(grid, sequence):
    rows = len(grid)
    cols = len(grid[0]) if rows else 0
    if not sequence:
        return 1
    used = [[False] * cols for _ in range(rows)]

    def walk(r, c, k):
        if grid[r][c] != sequence[k]:
            return False
        if k == len(sequence) - 1:
            return True
        used[r][c] = True
        for dr, dc in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nr, nc = r + dr, c + dc
            if 0 <= nr < rows and 0 <= nc < cols and not used[nr][nc] and walk(nr, nc, k + 1):
                used[r][c] = False
                return True
        used[r][c] = False
        return False

    return 1 if any(walk(r, c, 0) for r in range(rows) for c in range(cols)) else 0


PROBLEMS.append(Problem(
    id="grid-path-sequence",
    title="격자에서 수열 경로 찾기",
    summary="""
정수 격자 `grid` 와 정수 수열 `sequence` 가 주어진다. 격자의 어느 칸에서 시작해 **상하좌우로
한 칸씩** 옮겨 가며 `sequence` 를 순서대로 밟을 수 있으면 `1`, 없으면 `0` 을 반환한다.
**같은 칸을 두 번 밟을 수 없다.** 빈 수열은 항상 `1` 이다.
""",
    notes="""
시작 칸마다 깊이 우선으로 따라가 본다. 지나온 칸을 표시했다가 **되돌아올 때 지워야**
다른 시작점이나 다른 갈래가 그 칸을 다시 쓸 수 있다. 표시를 지우지 않는 것이 이 문제에서
가장 흔한 실수다.
""",
    drill_doc="""
Drill.visit(r * cols + c, k)      // 수열의 k 번째를 이 칸에서 시험했다
Drill.match(r * cols + c, k)      // 수열을 끝까지 밟았다
""",
    constraints="""
- `0 <= rows, cols <= 12`
- `0 <= sequence.size <= 12`
- 격자와 수열의 값은 `0` 이상 `9` 이하
""",
    signature=dict(name="pathExists", parameters=[("grid", "INT_MATRIX"), ("sequence", "INT_ARRAY")],
                   returns="INT"),
    groups=standard_groups(),
    reference=_path_exists,
    cases={
        "sample": [
            ("01", [[[1, 2, 3], [4, 5, 6], [7, 8, 9]], [1, 2, 5, 8, 9]]),
            ("02", [[[1, 2, 3], [4, 5, 6], [7, 8, 9]], [1, 5, 9]]),
        ],
        "boundary": [
            ("01-empty-sequence", [[[1]], []]),
            ("02-empty-grid", [[], [1]]),
            ("03-single-match", [[[7]], [7]]),
            ("04-single-mismatch", [[[7]], [8]]),
            # 같은 칸을 두 번 밟아야만 만들 수 있는 수열. 답은 0.
            ("05-reuse-needed", [[[1, 2]], [1, 2, 1]]),
            # 첫 갈래는 막히고 둘째 갈래로 가야 한다. 되돌아올 때 표시를 안 지우면 막힌다.
            ("06-backtrack-needed", [[[1, 1, 1], [1, 0, 1], [1, 1, 2]], [1, 1, 1, 1, 1, 1, 1, 2]]),
            # 시작점이 여럿. 첫 시작점에서 실패한 표시가 남으면 둘째 시작점이 막힌다.
            ("07-two-starts", [[[1, 2, 1], [3, 3, 2]], [1, 2, 2]]),
            # 대각선은 이동이 아니다.
            ("08-diagonal", [[[1, 0], [0, 2]], [1, 2]]),
        ],
        "hidden": [
            ("01-spiral", [[[1, 2, 3], [8, 9, 4], [7, 6, 5]], [1, 2, 3, 4, 5, 6, 7, 8, 9]]),
            ("02-long-dead-ends", [[[1, 1, 1, 1], [1, 0, 0, 1], [1, 1, 1, 1], [0, 0, 0, 2]], [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2]]),
            ("03-random-hit", [[[3, 1, 4, 1], [5, 9, 2, 6], [5, 3, 5, 8]], [9, 2, 6, 8, 5, 3]]),
            ("04-random-miss", [[[3, 1, 4, 1], [5, 9, 2, 6], [5, 3, 5, 8]], [9, 2, 6, 8, 5, 5]]),
            # 전부 같은 값. 경로가 많아 한 번에 찾지만, 표시를 안 지우는 풀이도 우연히 맞을 수 있다 —
            # 그래서 위의 막다른 길 케이스가 따로 있다.
            ("05-all-same", [[[1] * 4 for _ in range(4)], [1] * 12]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). DFS + 되돌리기.
fun pathExists(grid: Array<IntArray>, sequence: IntArray): Int {
    if (sequence.isEmpty()) return 1
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val used = Array(rows) { BooleanArray(cols) }
    val dr = intArrayOf(1, -1, 0, 0)
    val dc = intArrayOf(0, 0, 1, -1)
    fun walk(r: Int, c: Int, k: Int): Boolean {
        if (grid[r][c] != sequence[k]) return false
        Drill.visit(r * cols + c, k)
        if (k == sequence.size - 1) { Drill.match(r * cols + c, k); return true }
        used[r][c] = true
        for (d in 0 until 4) {
            val nr = r + dr[d]
            val nc = c + dc[d]
            if (nr in 0 until rows && nc in 0 until cols && !used[nr][nc] && walk(nr, nc, k + 1)) {
                used[r][c] = false
                return true
            }
        }
        used[r][c] = false
        return false
    }
    for (r in 0 until rows) for (c in 0 until cols) if (walk(r, c, 0)) return 1
    return 0
}
""",
    mutants=[
        ("never-unmarks", "WRONG_BRANCH",
         "되돌아올 때 표시를 지우지 않는다. 막힌 갈래가 지나간 칸을 다른 갈래가 쓰지 못한다.",
         """
fun pathExists(grid: Array<IntArray>, sequence: IntArray): Int {
    if (sequence.isEmpty()) return 1
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val used = Array(rows) { BooleanArray(cols) }
    fun walk(r: Int, c: Int, k: Int): Boolean {
        if (grid[r][c] != sequence[k]) return false
        if (k == sequence.size - 1) return true
        used[r][c] = true
        for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols && !used[nr][nc] && walk(nr, nc, k + 1)) return true
        }
        return false
    }
    for (r in 0 until rows) for (c in 0 until cols) if (walk(r, c, 0)) return 1
    return 0
}
"""),
        ("allows-reuse", "MISSING_EDGE_CASE",
         "같은 칸을 다시 밟는 것을 막지 않는다.",
         """
fun pathExists(grid: Array<IntArray>, sequence: IntArray): Int {
    if (sequence.isEmpty()) return 1
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    fun walk(r: Int, c: Int, k: Int): Boolean {
        if (grid[r][c] != sequence[k]) return false
        if (k == sequence.size - 1) return true
        for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols && walk(nr, nc, k + 1)) return true
        }
        return false
    }
    for (r in 0 until rows) for (c in 0 until cols) if (walk(r, c, 0)) return 1
    return 0
}
"""),
        ("first-start-only", "WRONG_ALGORITHM",
         "수열의 첫 값과 같은 첫 칸에서만 시작한다. 다른 시작점을 보지 않는다.",
         """
fun pathExists(grid: Array<IntArray>, sequence: IntArray): Int {
    if (sequence.isEmpty()) return 1
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val used = Array(rows) { BooleanArray(cols) }
    fun walk(r: Int, c: Int, k: Int): Boolean {
        if (grid[r][c] != sequence[k]) return false
        if (k == sequence.size - 1) return true
        used[r][c] = true
        for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols && !used[nr][nc] && walk(nr, nc, k + 1)) { used[r][c] = false; return true }
        }
        used[r][c] = false
        return false
    }
    for (r in 0 until rows) for (c in 0 until cols) if (grid[r][c] == sequence[0]) return if (walk(r, c, 0)) 1 else 0
    return 0
}
"""),
    ],
))


# --- 69. k 개의 같은 합 묶음 (가지치기가 곧 답이다) ------------------------------------------

def _can_partition_k(nums, k):
    total = sum(nums)
    if k <= 0 or total % k != 0:
        return 0
    target = total // k
    values = sorted(nums, reverse=True)
    if values and values[0] > target:
        return 0
    buckets = [0] * k

    def place(i):
        if i == len(values):
            return True
        v = values[i]
        seen = set()
        for b in range(k):
            if buckets[b] + v > target or buckets[b] in seen:
                continue
            seen.add(buckets[b])
            buckets[b] += v
            if place(i + 1):
                return True
            buckets[b] -= v
            if buckets[b] == 0:
                break
        return False

    return 1 if place(0) else 0


PROBLEMS.append(Problem(
    id="partition-k-equal-sums",
    title="k 개의 같은 합 묶음",
    summary="""
양의 정수 배열 `nums` 와 정수 `k` 가 주어진다. 배열을 **합이 같은 k 개의 묶음**으로 남김없이
나눌 수 있으면 `1`, 없으면 `0` 을 반환한다. 각 원소는 정확히 한 묶음에 들어간다.
""",
    notes="""
답을 찾는 것은 되추적이다. 그런데 되추적은 **가지치기 없이는 끝나지 않는다** — 원소 16 개에
묶음 4 개면 4^16 이다. 큰 원소부터 놓고, 같은 값의 빈 묶음은 하나만 시험하고, 원소를 넣었을 때
목표를 넘으면 곧바로 물러선다. 그러면 같은 문제가 순식간에 끝난다.
""",
    drill_doc="""
Drill.visit(i, bucket)        // i 번째 원소를 그 묶음에 넣어 봤다
Drill.match(i, bucket)        // 놓을 자리를 찾았다
""",
    constraints="""
- `1 <= nums.size <= 16`
- `1 <= nums[i] <= 10_000`
- `1 <= k <= nums.size`
""",
    signature=dict(name="canPartition", parameters=[("nums", "INT_ARRAY"), ("k", "INT")],
                   returns="INT"),
    # 성능 그룹의 케이스도 16 개다. 가지치기 없는 되추적은 그 크기에서 끝나지 않는다.
    groups=perf_groups(time_multiplier=0.5),
    reference=_can_partition_k,
    cases={
        "sample": [
            ("01", [[4, 3, 2, 3, 5, 2, 1], 4]),
            ("02", [[1, 2, 3, 4], 3]),
        ],
        "boundary": [
            ("01-k-one", [[5, 7], 1]),
            ("02-k-equals-size", [[3, 3, 3], 3]),
            ("03-k-equals-size-unequal", [[3, 4, 3], 3]),
            # 합은 나누어떨어지지만 한 원소가 목표보다 크다.
            ("04-element-too-big", [[10, 1, 1], 2]),
            ("05-sum-not-divisible", [[1, 1, 1], 2]),
            ("06-single", [[9], 1]),
            # 같은 값이 많다. 같은 값의 빈 묶음을 전부 시험하면 오래 걸린다.
            ("07-many-equal", [[2] * 12, 4]),
            # 마지막 원소 하나가 들어갈 자리가 없다. 마지막을 놓기 전에 끝내면 1 을 낸다.
            ("08-last-element-decides", [[2, 2, 2, 3, 3], 3]),
        ],
        "hidden": [
            ("01-possible", [[2, 2, 2, 2, 3, 4, 5], 4]),
            ("02-impossible-close", [[2, 2, 2, 2, 3, 4, 6], 4]),
            ("03-big-values", [[10000, 5000, 5000, 3000, 7000, 2500, 2500, 5000], 4]),
            ("04-sixteen-possible", [[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16], 4]),
            ("05-sixteen-impossible", [[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 20], 4]),
        ],
        "performance": [
            # 합은 나누어떨어지는데 답이 없는 입력. 가지치기 없이는 4^16 을 다 돈다.
            ("01-hard-no", [[15, 15, 15, 15, 15, 15, 15, 15, 14, 14, 14, 14, 14, 14, 14, 26], 4]),
            ("02-hard-no-2", [[9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 33], 8]),
            ("03-hard-no-3", [[7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 21], 6]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 큰 것부터, 같은 값의 묶음은 하나만, 넘치면 물러선다.
fun canPartition(nums: IntArray, k: Int): Int {
    val total = nums.sum()
    if (k <= 0 || total % k != 0) return 0
    val target = total / k
    val values = nums.sortedDescending()
    if (values.first() > target) return 0
    val buckets = IntArray(k)
    fun place(i: Int): Boolean {
        if (i == values.size) return true
        val v = values[i]
        var tried = -1
        for (b in 0 until k) {
            if (buckets[b] + v > target || buckets[b] == tried) continue
            tried = buckets[b]
            Drill.visit(i, b)
            buckets[b] += v
            if (place(i + 1)) { Drill.match(i, b); return true }
            buckets[b] -= v
            if (buckets[b] == 0) break
        }
        return false
    }
    return if (place(0)) 1 else 0
}
""",
    mutants=[
        ("last-element-unplaced", "OFF_BY_ONE",
         "마지막 원소를 놓기 전에 성공으로 본다. 마지막 하나가 들어갈 자리가 없어도 1 을 낸다.",
         """
fun canPartition(nums: IntArray, k: Int): Int {
    val total = nums.sum()
    if (k <= 0 || total % k != 0) return 0
    val target = total / k
    val values = nums.sortedDescending()
    if (values.first() > target) return 0
    val buckets = IntArray(k)
    fun place(i: Int): Boolean {
        if (i >= values.size - 1) return true
        var tried = -1
        for (b in 0 until k) {
            if (buckets[b] + values[i] > target || buckets[b] == tried) continue
            tried = buckets[b]
            buckets[b] += values[i]
            if (place(i + 1)) return true
            buckets[b] -= values[i]
            if (buckets[b] == 0) break
        }
        return false
    }
    return if (place(0)) 1 else 0
}
"""),
        ("greedy-largest-first", "WRONG_ALGORITHM",
         "큰 것부터 지금 가장 덜 찬 묶음에 넣고 끝낸다. 되돌아오지 않는다.",
         """
fun canPartition(nums: IntArray, k: Int): Int {
    val total = nums.sum()
    if (total % k != 0) return 0
    val target = total / k
    val buckets = IntArray(k)
    for (v in nums.sortedDescending()) {
        val b = (0 until k).minByOrNull { buckets[it] }!!
        buckets[b] += v
    }
    return if (buckets.all { it == target }) 1 else 0
}
"""),
        ("no-pruning--exponential", "PERFORMANCE",
         "가지치기 없이 모든 배치를 시험한다. 4^16 이다.",
         """
fun canPartition(nums: IntArray, k: Int): Int {
    val total = nums.sum()
    if (k <= 0 || total % k != 0) return 0
    val target = total / k
    val buckets = IntArray(k)
    fun place(i: Int): Boolean {
        if (i == nums.size) return buckets.all { it == target }
        for (b in 0 until k) {
            Drill.visit(i, b)
            buckets[b] += nums[i]
            if (place(i + 1)) return true
            buckets[b] -= nums[i]
        }
        return false
    }
    return if (place(0)) 1 else 0
}
"""),
    ],
))
