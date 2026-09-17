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


# --- 133. 연산자를 끼워 목표를 만드는 방법의 수 (백트래킹) ------------------------------------------------

def _expression_targets(num, target):
    n = len(num)
    count = 0

    def go(pos, total, last):
        nonlocal count
        if pos == n:
            if total == target:
                count += 1
            return
        for end in range(pos + 1, n + 1):
            piece = num[pos:end]
            if len(piece) > 1 and piece[0] == "0":
                break
            value = int(piece)
            if pos == 0:
                go(end, value, value)
            else:
                go(end, total + value, value)
                go(end, total - value, -value)
                go(end, total - last + last * value, last * value)

    go(0, 0, 0)
    return count


def _digits_str(n, salt):
    return "".join(str(d) for d in randoms(n, 0, 9, salt=salt))


PROBLEMS.append(Problem(
    id="expression-targets-count",
    title="연산자를 끼워 목표 만들기",
    summary="""
숫자로만 된 문자열 `num` 과 정수 `target` 이 주어진다. 자리 사이에 `+`, `-`, `*` 중 하나를 끼우거나
아무것도 끼우지 않아(자리를 이어 붙여) 식을 만든다. 만든 식의 값이 `target` 인 **서로 다른 식의
수**를 반환한다.

이어 붙인 수는 앞에 `0` 이 올 수 없다 — `"05"` 는 안 되고 `"0"` 은 된다. 곱셈은 덧셈·뺄셈보다
먼저 계산한다.
""",
    notes="""
자리마다 세 연산자와 "이어 붙이기"를 다 해 본다 — 가지가 최대 4ⁿ⁻¹ 이라 `n <= 10` 에서
26만 갈래이고 그것이면 충분하다. 곱셈이 먼저라는 것이 함정이다: 지금까지의 합에 곱을 그냥
곱할 수 없고, **마지막으로 더한 항**을 기억했다가 그 항을 빼고 `항 × 값` 을 더해야 한다.
""",
    drill_doc="""
Drill.call("2+3")             // 식을 하나 더 늘렸다
Drill.ret("2+3", total)       // 그 가지의 값
""",
    constraints="""
- `1 <= num.length <= 10`
- `num` 은 `0`~`9` 로만 되어 있다
- `-10^9 <= target <= 10^9`
""",
    signature=dict(name="expressionTargets", parameters=[("num", "STRING"), ("target", "INT")], returns="INT"),
    groups=standard_groups(),
    reference=_expression_targets,
    limits={"timeMillis": 4000, "memoryMb": 256, "outputBytes": 65536},
    cases={
        "sample": [("01", ["123", 6]), ("02", ["232", 8])],
        "boundary": [
            ("01-single-digit", ["7", 7]),
            ("02-single-digit-miss", ["7", 8]),
            # 앞에 0 이 오는 수는 안 된다 — "05" 는 없다.
            ("03-leading-zero", ["105", 5]),
            ("04-zero-alone", ["00", 0]),
            # 곱셈이 먼저다: 2+3*4 = 14.
            ("05-precedence", ["234", 14]),
            # 곱셈 뒤에 뺄셈이 오는 경우: 1-2*3 = -5.
            ("06-minus-then-multiply", ["123", -5]),
            ("07-concat-all", ["345623749", 345623749]),
            ("08-negative-target", ["123", -4]),
        ],
        "hidden": [
            ("01-random-6", [_digits_str(6, salt=8521), 30]),
            ("02-random-8", [_digits_str(8, salt=8522), 0]),
            ("03-random-10", [_digits_str(10, salt=8523), 100]),
            ("04-many-ways", ["1111111111", 0]),
            ("05-zeros", ["0000000000", 0]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 마지막 항을 들고 다니는 백트래킹.
fun expressionTargets(num: String, target: Int): Int {
    val n = num.length
    var count = 0
    fun go(pos: Int, total: Long, last: Long) {
        if (pos == n) { if (total == target.toLong()) count += 1; return }
        var value = 0L
        for (end in pos until n) {
            if (end > pos && num[pos] == '0') break
            value = value * 10 + (num[end] - '0')
            if (pos == 0) {
                Drill.call("start")
                go(end + 1, value, value)
            } else {
                Drill.call("op")
                go(end + 1, total + value, value)
                go(end + 1, total - value, -value)
                go(end + 1, total - last + last * value, last * value)
            }
        }
    }
    go(0, 0L, 0L)
    return count
}
""",
    mutants=[
        ("multiplies-total--ignores-precedence", "WRONG_ALGORITHM",
         "곱셈을 지금까지의 합 전체에 적용한다. 곱셈이 먼저라는 규칙을 어긴다.",
         """
fun expressionTargets(num: String, target: Int): Int {
    val n = num.length
    var count = 0
    fun go(pos: Int, total: Long) {
        if (pos == n) { if (total == target.toLong()) count += 1; return }
        var value = 0L
        for (end in pos until n) {
            if (end > pos && num[pos] == '0') break
            value = value * 10 + (num[end] - '0')
            if (pos == 0) go(end + 1, value) else { go(end + 1, total + value); go(end + 1, total - value); go(end + 1, total * value) }
        }
    }
    go(0, 0L)
    return count
}
"""),
        ("allows-leading-zeros", "MISSING_EDGE_CASE",
         "앞에 0 이 오는 수를 허용한다. 05 를 5 로 센다.",
         """
fun expressionTargets(num: String, target: Int): Int {
    val n = num.length
    var count = 0
    fun go(pos: Int, total: Long, last: Long) {
        if (pos == n) { if (total == target.toLong()) count += 1; return }
        var value = 0L
        for (end in pos until n) {
            value = value * 10 + (num[end] - '0')
            if (pos == 0) go(end + 1, value, value) else {
                go(end + 1, total + value, value); go(end + 1, total - value, -value); go(end + 1, total - last + last * value, last * value)
            }
        }
    }
    go(0, 0L, 0L)
    return count
}
"""),
        ("no-concatenation", "MISSING_EDGE_CASE",
         "자리를 이어 붙이는 경우를 빼고 한 자리씩만 쓴다.",
         """
fun expressionTargets(num: String, target: Int): Int {
    val n = num.length
    var count = 0
    fun go(pos: Int, total: Long, last: Long) {
        if (pos == n) { if (total == target.toLong()) count += 1; return }
        val value = (num[pos] - '0').toLong()
        if (pos == 0) go(pos + 1, value, value) else {
            go(pos + 1, total + value, value); go(pos + 1, total - value, -value); go(pos + 1, total - last + last * value, last * value)
        }
    }
    go(0, 0L, 0L)
    return count
}
"""),
        ("last-term-wrong-after-minus", "WRONG_BRANCH",
         "뺄셈 뒤의 곱셈에서 마지막 항의 부호를 잃는다. 1-2*3 을 1-(-2*3) 처럼 계산한다.",
         """
fun expressionTargets(num: String, target: Int): Int {
    val n = num.length
    var count = 0
    fun go(pos: Int, total: Long, last: Long) {
        if (pos == n) { if (total == target.toLong()) count += 1; return }
        var value = 0L
        for (end in pos until n) {
            if (end > pos && num[pos] == '0') break
            value = value * 10 + (num[end] - '0')
            if (pos == 0) go(end + 1, value, value) else {
                go(end + 1, total + value, value); go(end + 1, total - value, value); go(end + 1, total - last + last * value, last * value)
            }
        }
    }
    go(0, 0L, 0L)
    return count
}
"""),
    ],
))


# --- 137. 서로 다른 이진 탐색 트리의 수 (루트로 나누기) -------------------------------------------------

def _unique_bst(n):
    memo = {0: 1, 1: 1}

    def go(k):
        if k in memo:
            return memo[k]
        total = 0
        for root in range(1, k + 1):
            total += go(root - 1) * go(k - root)
        memo[k] = total
        return total

    return go(n)


PROBLEMS.append(Problem(
    id="unique-bst-count",
    title="서로 다른 이진 탐색 트리의 수",
    summary="""
`1` 부터 `n` 까지의 값을 모두 담는 **구조가 서로 다른** 이진 탐색 트리의 수를 반환한다.
`n = 0` 이면 빈 트리 하나로 `1` 이다.
""",
    notes="""
루트를 `r` 로 잡으면 왼쪽에는 `1..r-1`, 오른쪽에는 `r+1..n` 이 들어간다. 왼쪽의 모양 수는
값이 무엇이든 크기만으로 정해진다 — 그래서 답은 `sum over r of f(r-1) * f(n-r)` 이다.
같은 크기를 거듭 부르니 기억하지 않으면 지수다. 카탈란 수이지만 그 이름을 몰라도 된다.
""",
    drill_doc="""
Drill.compare(left, right)    // 왼쪽·오른쪽 크기의 곱을 봤다
Drill.write(size, count)      // 크기의 답을 정했다
""",
    constraints="""
- `0 <= n <= 19` (답은 `Int` 안이다)
""",
    signature=dict(name="uniqueBstCount", parameters=[("n", "INT")], returns="INT"),
    # 공개 뒤 한도(케이스)를 고쳤다 — 판정이 바뀌므로 새 버전이다 (§6.1 공개 후 불변).
    version=2,
    groups=standard_groups(),
    reference=_unique_bst,
    # 기억 없는 재귀는 n = 19 에서 3^19 번쯤 부르고 1초 남짓이다. 기본 한도 2초 안에 들어와
    # 살아남았고, 400ms 에서 1.9배, 200ms 에서도 전체 검증에서 3.0배였다. 정답은 마이크로초라 150ms.
    limits={"timeMillis": 150, "memoryMb": 256, "outputBytes": 65536},
    cases={
        "sample": [("01", [3]), ("02", [1])],
        "boundary": [
            ("01-zero", [0]),
            ("02-two", [2]),
            ("03-four", [4]),
            ("04-max", [19]),
            ("05-eighteen", [18]),
        ],
        "hidden": [
            ("01-seven", [7]),
            ("02-ten", [10]),
            ("03-thirteen", [13]),
            ("04-sixteen", [16]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 크기별로 기억하는 재귀.
fun uniqueBstCount(n: Int): Int {
    val memo = IntArray(n + 1) { -1 }
    fun go(k: Int): Int {
        if (k <= 1) return 1
        if (memo[k] >= 0) return memo[k]
        var total = 0
        for (root in 1..k) { Drill.compare(root - 1, k - root); total += go(root - 1) * go(k - root) }
        memo[k] = total
        Drill.write(k, total)
        return total
    }
    return go(n)
}
""",
    mutants=[
        ("root-ranges-to-n-minus-one", "OFF_BY_ONE",
         "루트를 1..k-1 만 잡는다. 마지막 값이 루트인 트리를 빠뜨린다.",
         """
fun uniqueBstCount(n: Int): Int {
    val memo = IntArray(n + 1) { -1 }
    fun go(k: Int): Int {
        if (k <= 1) return 1
        if (memo[k] >= 0) return memo[k]
        var total = 0
        for (root in 1 until k) total += go(root - 1) * go(k - root)
        memo[k] = total
        return total
    }
    return go(n)
}
"""),
        ("adds-instead-of-multiplies", "WRONG_ALGORITHM",
         "왼쪽과 오른쪽의 모양 수를 곱하지 않고 더한다.",
         """
fun uniqueBstCount(n: Int): Int {
    val memo = IntArray(n + 1) { -1 }
    fun go(k: Int): Int {
        if (k <= 1) return 1
        if (memo[k] >= 0) return memo[k]
        var total = 0
        for (root in 1..k) total += go(root - 1) + go(k - root)
        memo[k] = total
        return total
    }
    return go(n)
}
"""),
        ("empty-subtree-counts-zero", "MISSING_EDGE_CASE",
         "빈 서브트리의 모양 수를 0 으로 둔다. 한쪽이 비는 루트가 전부 사라진다.",
         """
fun uniqueBstCount(n: Int): Int {
    if (n == 0) return 1
    val memo = IntArray(n + 1) { -1 }
    fun go(k: Int): Int {
        if (k == 0) return 0
        if (k == 1) return 1
        if (memo[k] >= 0) return memo[k]
        var total = 0
        for (root in 1..k) total += go(root - 1) * go(k - root)
        memo[k] = total
        return total
    }
    return go(n)
}
"""),
        ("no-memo--exponential", "PERFORMANCE",
         "기억하지 않는다. 같은 크기를 지수 번 다시 센다.",
         """
fun uniqueBstCount(n: Int): Int {
    fun go(k: Int): Int {
        if (k <= 1) return 1
        var total = 0
        for (root in 1..k) { Drill.compare(root - 1, k - root); total += go(root - 1) * go(k - root) }
        return total
    }
    return go(n)
}
"""),
    ],
))


# --- 138. 괄호를 넣는 모든 방법의 값 (연산자에서 나누기) ------------------------------------------------

def _different_ways(expression):
    memo = {}

    def go(lo, hi):
        if (lo, hi) in memo:
            return memo[(lo, hi)]
        piece = expression[lo:hi]
        if piece.isdigit():
            result = [int(piece)]
        else:
            result = []
            for i in range(lo, hi):
                op = expression[i]
                if op in "+-*":
                    for a in go(lo, i):
                        for b in go(i + 1, hi):
                            result.append(a + b if op == "+" else a - b if op == "-" else a * b)
        memo[(lo, hi)] = result
        return result

    return sorted(go(0, len(expression)))


def _expression(count, salt):
    digits = randoms(count + 1, 0, 9, salt=salt)
    ops = randoms(count, 0, 2, salt=salt + 1)
    out = [str(digits[0])]
    for i in range(count):
        out.append("+-*"[ops[i]])
        out.append(str(digits[i + 1]))
    return "".join(out)


PROBLEMS.append(Problem(
    id="different-ways-to-compute",
    title="괄호를 넣는 모든 방법",
    summary="""
한 자리 숫자와 `+`, `-`, `*` 로 된 식이 주어진다. 괄호를 넣어 계산 순서를 정하는 **모든**
방법의 결과를 오름차순으로 담은 배열을 반환한다. 다른 괄호가 같은 값을 내면 그 값은 그
수만큼 여러 번 들어간다.
""",
    notes="""
마지막에 계산되는 연산자를 고르면 식은 그 왼쪽과 오른쪽으로 나뉘고, 답은 왼쪽의 모든 값과
오른쪽의 모든 값의 조합이다. 숫자 하나는 값 하나다. 같은 부분식이 여러 번 나오니 구간을
기억하면 좋지만, 이 제약에서는 없이도 된다 — 결과의 수 자체가 카탈란 수라 그것이 비용의
하한이다.
""",
    drill_doc="""
Drill.compare(lo, hi)         // 구간을 나누기 시작했다
Drill.write(i, value)         // 결과 하나를 얻었다
""",
    constraints="""
- `1 <= expression.length <= 17` — 숫자는 한 자리, 연산자는 최대 8 개
- 모든 중간값과 결과는 `Int` 안이다
""",
    signature=dict(name="differentWaysToCompute", parameters=[("expression", "STRING")], returns="INT_ARRAY"),
    groups=standard_groups(),
    reference=_different_ways,
    cases={
        "sample": [("01", ["2-1-1"]), ("02", ["2*3-4*5"])],
        "boundary": [
            ("01-single-digit", ["7"]),
            ("02-one-operator", ["3*4"]),
            # 같은 값이 여러 번 — 중복을 지우면 틀린다.
            ("03-duplicates-kept", ["1+1+1"]),
            ("04-zero-digits", ["0*0-0"]),
            # 빼기는 순서가 답을 바꾼다.
            ("05-subtraction-chain", ["9-5-3-1"]),
            ("06-max-operators", ["1+2*3-4+5*6-7+8*9"]),
        ],
        "hidden": [
            ("01-random-three", [_expression(3, salt=8671)]),
            ("02-random-five", [_expression(5, salt=8673)]),
            ("03-random-seven", [_expression(7, salt=8675)]),
            ("04-all-multiply", ["9*9*9*9*9*9*9*9*9"]),
            ("05-random-eight", [_expression(8, salt=8677)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 연산자마다 나누고 양쪽의 값을 조합한다.
fun differentWaysToCompute(expression: String): IntArray {
    val memo = HashMap<Long, List<Int>>()
    fun go(lo: Int, hi: Int): List<Int> {
        val key = lo.toLong() * 100 + hi
        memo[key]?.let { return it }
        Drill.compare(lo, hi)
        val result = ArrayList<Int>()
        var split = false
        for (i in lo until hi) {
            val op = expression[i]
            if (op == '+' || op == '-' || op == '*') {
                split = true
                for (a in go(lo, i)) for (b in go(i + 1, hi)) {
                    result.add(when (op) { '+' -> a + b; '-' -> a - b; else -> a * b })
                }
            }
        }
        if (!split) result.add(expression.substring(lo, hi).toInt())
        memo[key] = result
        return result
    }
    val out = go(0, expression.length).sorted().toIntArray()
    out.forEachIndexed { i, v -> Drill.write(i, v) }
    return out
}
""",
    mutants=[
        ("dedups-results", "WRONG_BRANCH",
         "같은 값을 하나로 합친다. 다른 괄호가 같은 값을 내면 그 수만큼 들어가야 한다.",
         """
fun differentWaysToCompute(expression: String): IntArray {
    fun go(lo: Int, hi: Int): List<Int> {
        val result = ArrayList<Int>()
        var split = false
        for (i in lo until hi) {
            val op = expression[i]
            if (op == '+' || op == '-' || op == '*') {
                split = true
                for (a in go(lo, i)) for (b in go(i + 1, hi)) result.add(when (op) { '+' -> a + b; '-' -> a - b; else -> a * b })
            }
        }
        if (!split) result.add(expression.substring(lo, hi).toInt())
        return result
    }
    return go(0, expression.length).distinct().sorted().toIntArray()
}
"""),
        ("subtraction-reversed", "WRONG_ALGORITHM",
         "빼기를 오른쪽에서 왼쪽으로 뺀다.",
         """
fun differentWaysToCompute(expression: String): IntArray {
    fun go(lo: Int, hi: Int): List<Int> {
        val result = ArrayList<Int>()
        var split = false
        for (i in lo until hi) {
            val op = expression[i]
            if (op == '+' || op == '-' || op == '*') {
                split = true
                for (a in go(lo, i)) for (b in go(i + 1, hi)) result.add(when (op) { '+' -> a + b; '-' -> b - a; else -> a * b })
            }
        }
        if (!split) result.add(expression.substring(lo, hi).toInt())
        return result
    }
    return go(0, expression.length).sorted().toIntArray()
}
"""),
        ("splits-only-at-first-operator", "MISSING_EDGE_CASE",
         "첫 연산자에서만 나눈다. 왼쪽이 먼저 계산되는 괄호만 센다.",
         """
fun differentWaysToCompute(expression: String): IntArray {
    fun go(lo: Int, hi: Int): List<Int> {
        val result = ArrayList<Int>()
        for (i in lo until hi) {
            val op = expression[i]
            if (op == '+' || op == '-' || op == '*') {
                for (a in go(lo, i)) for (b in go(i + 1, hi)) result.add(when (op) { '+' -> a + b; '-' -> a - b; else -> a * b })
                return result
            }
        }
        result.add(expression.substring(lo, hi).toInt())
        return result
    }
    return go(0, expression.length).sorted().toIntArray()
}
"""),
        ("left-to-right-only", "WRONG_ALGORITHM",
         "왼쪽에서 오른쪽으로 한 번 계산한 값 하나만 낸다.",
         """
fun differentWaysToCompute(expression: String): IntArray {
    var value = expression[0] - '0'
    var i = 1
    while (i < expression.length) {
        val b = expression[i + 1] - '0'
        value = when (expression[i]) { '+' -> value + b; '-' -> value - b; else -> value * b }
        i += 2
    }
    return intArrayOf(value)
}
"""),
    ],
))


# --- 151. 아름다운 배열의 수 (가지치기 백트래킹) -------------------------------------------------------

def _beautiful_arrangements(n):
    memo = {}

    def go(mask, i):
        if i > n:
            return 1
        if mask in memo:
            return memo[mask]
        total = 0
        for v in range(1, n + 1):
            if not (mask >> v) & 1 and (v % i == 0 or i % v == 0):
                total += go(mask | (1 << v), i + 1)
        memo[mask] = total
        return total

    return go(0, 1)


PROBLEMS.append(Problem(
    id="beautiful-arrangement-count",
    title="아름다운 배열의 수",
    summary="""
`1..n` 의 순열 `perm` (자리는 `1` 부터) 이 **아름답다**는 것은 모든 자리 `i` 에서 `perm[i] % i == 0`
또는 `i % perm[i] == 0` 인 것이다. 아름다운 순열의 수를 반환한다.
""",
    notes="""
순열 전부는 n! 이다. 자리 `1` 부터 채우며 **그 자리에 올 수 있는 값만** 시도하는 백트래킹은
가지가 급격히 줄어 `n = 15` 에서도 빠르다. 조건이 자리마다 독립이라 "어떤 값을 썼는가"(비트
집합)만 상태이고, 같은 집합에서의 답을 기억하면 더 빠르지만 가지치기만으로도 제한 안이다.
""",
    drill_doc="""
Drill.compare(i, v)           // 자리 i 에 값 v 를 시도했다
Drill.write(0, count)         // 답을 하나 늘렸다
""",
    constraints="""
- `1 <= n <= 15`
""",
    signature=dict(name="beautifulArrangementCount", parameters=[("n", "INT")], returns="INT"),
    groups=standard_groups(),
    reference=_beautiful_arrangements,
    limits={"timeMillis": 1000, "memoryMb": 256, "outputBytes": 65536},
    cases={
        "sample": [("01", [2]), ("02", [1])],
        "boundary": [
            ("01-three", [3]),
            ("02-four", [4]),
            ("03-prime-seven", [7]),
            ("04-max", [15]),
            ("05-fourteen", [14]),
        ],
        "hidden": [
            ("01-six", [6]),
            ("02-nine", [9]),
            ("03-eleven", [11]),
            ("04-thirteen", [13]),
            ("05-twelve", [12]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 자리마다 가능한 값만 시도하는 백트래킹.
fun beautifulArrangementCount(n: Int): Int {
    val used = BooleanArray(n + 1)
    var count = 0
    fun go(i: Int) {
        if (i > n) { count += 1; Drill.write(0, count); return }
        for (v in 1..n) {
            if (used[v] || (v % i != 0 && i % v != 0)) continue
            Drill.compare(i, v)
            used[v] = true
            go(i + 1)
            used[v] = false
        }
    }
    go(1)
    return count
}
""",
    mutants=[
        ("checks-only-divides-one-way", "WRONG_BRANCH",
         "perm[i] % i == 0 만 본다. i % perm[i] == 0 인 배치를 놓친다.",
         """
fun beautifulArrangementCount(n: Int): Int {
    val used = BooleanArray(n + 1)
    var count = 0
    fun go(i: Int) {
        if (i > n) { count += 1; return }
        for (v in 1..n) {
            if (used[v] || v % i != 0) continue
            used[v] = true; go(i + 1); used[v] = false
        }
    }
    go(1)
    return count
}
"""),
        ("forgets-to-unmark", "MISSING_EDGE_CASE",
         "되돌아올 때 사용 표시를 지우지 않는다. 첫 가지 뒤의 배치가 전부 사라진다.",
         """
fun beautifulArrangementCount(n: Int): Int {
    val used = BooleanArray(n + 1)
    var count = 0
    fun go(i: Int) {
        if (i > n) { count += 1; return }
        for (v in 1..n) {
            if (used[v] || (v % i != 0 && i % v != 0)) continue
            used[v] = true; go(i + 1)
        }
    }
    go(1)
    return count
}
"""),
        ("zero-based-positions", "OFF_BY_ONE",
         "자리를 0 부터 센다. 0 으로 나누는 자리를 피하려 조건이 어긋난다.",
         """
fun beautifulArrangementCount(n: Int): Int {
    val used = BooleanArray(n + 1)
    var count = 0
    fun go(i: Int) {
        if (i >= n) { count += 1; return }
        for (v in 1..n) {
            if (used[v] || (i != 0 && v % i != 0 && i % v != 0)) continue
            used[v] = true; go(i + 1); used[v] = false
        }
    }
    go(0)
    return count
}
"""),
        ("all-permutations-then-check", "PERFORMANCE",
         "순열 전부를 만든 뒤 검사한다. 15! 이다.",
         """
fun beautifulArrangementCount(n: Int): Int {
    val perm = IntArray(n + 1)
    val used = BooleanArray(n + 1)
    var count = 0
    fun go(i: Int) {
        if (i > n) {
            var ok = true
            for (p in 1..n) if (perm[p] % p != 0 && p % perm[p] != 0) { ok = false; break }
            if (ok) count += 1
            return
        }
        for (v in 1..n) {
            if (used[v]) continue
            Drill.compare(i, v)
            used[v] = true; perm[i] = v; go(i + 1); used[v] = false
        }
    }
    go(1)
    return count
}
"""),
    ],
))


# --- 152. 올바른 IP 나누기의 수 (백트래킹) ---------------------------------------------------------------

def _ip_splits(digits):
    n = len(digits)

    def valid(piece):
        if not piece or len(piece) > 3:
            return False
        if piece[0] == "0" and len(piece) > 1:
            return False
        return int(piece) <= 255

    def go(start, parts):
        if parts == 4:
            return 1 if start == n else 0
        total = 0
        for length in (1, 2, 3):
            piece = digits[start:start + length]
            if start + length <= n and valid(piece):
                total += go(start + length, parts + 1)
        return total

    return go(0, 0)


PROBLEMS.append(Problem(
    id="valid-ip-splits-count",
    title="올바른 IP 나누기의 수",
    summary="""
숫자만 있는 문자열 `digits` 를 점 세 개로 나눠 IPv4 주소로 만드는 방법의 수를 반환한다. 각 조각은
`0..255` 의 정수이고, `0` 이 아닌 조각은 앞에 `0` 이 올 수 없다(`01` 은 안 되고 `0` 은 된다).
문자열 전부를 써야 하고, 순서를 바꾸지 못한다.
""",
    notes="""
조각은 넷이고 길이는 각 1~3 이라 가지는 3³ = 27 개뿐이다. 자리마다 길이 1·2·3 을 시도하고 조각이
올바른지 보며 내려가고, 네 조각에서 정확히 끝에 닿았을 때만 하나로 센다. 앞의 `0` 규칙과 남는
글자를 다 쓰는 규칙이 답을 가른다. 길이가 4 미만이거나 12 초과면 0 이다.
""",
    drill_doc="""
Drill.compare(start, length)  // 조각을 시도했다
Drill.write(0, count)         // 답을 하나 늘렸다
""",
    constraints="""
- `1 <= digits.length <= 20`, 숫자만
""",
    signature=dict(name="validIpSplitsCount", parameters=[("digits", "STRING")], returns="INT"),
    groups=standard_groups(),
    reference=_ip_splits,
    cases={
        "sample": [("01", ["25525511135"]), ("02", ["0000"])],
        "boundary": [
            ("01-too-short", ["123"]),
            ("02-too-long", ["1234567890123"]),
            # 앞의 0: "010" 은 안 되고 "0" 은 된다.
            ("03-leading-zero", ["010010"]),
            ("04-over-255", ["256256256256"]),
            ("05-exactly-255", ["255255255255"]),
            ("06-all-ones", ["1111"]),
            ("07-many-ways", ["111111"]),
            ("08-length-twelve-with-zeros", ["100100100100"]),
        ],
        "hidden": [
            ("01-mixed", ["19216801"]),
            ("02-mixed-zeros", ["10101010"]),
            ("03-long-small-digits", ["1111111111"]),
            ("04-all-zeros-eight", ["00000000"]),
            ("05-borderline", ["2552552552"]),
            ("06-twelve-nines", ["999999999999"]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 길이 1·2·3 을 시도하는 깊이 4 의 백트래킹.
fun validIpSplitsCount(digits: String): Int {
    val n = digits.length
    fun valid(piece: String): Boolean {
        if (piece.isEmpty() || piece.length > 3) return false
        if (piece[0] == '0' && piece.length > 1) return false
        return piece.toInt() <= 255
    }
    var count = 0
    fun go(start: Int, parts: Int) {
        if (parts == 4) { if (start == n) { count += 1; Drill.write(0, count) }; return }
        for (length in 1..3) {
            if (start + length > n) break
            Drill.compare(start, length)
            if (valid(digits.substring(start, start + length))) go(start + length, parts + 1)
        }
    }
    go(0, 0)
    return count
}
""",
    mutants=[
        ("allows-leading-zero", "MISSING_EDGE_CASE",
         "앞의 0 을 허용한다. 010 이 조각이 된다.",
         """
fun validIpSplitsCount(digits: String): Int {
    val n = digits.length
    fun valid(piece: String) = piece.isNotEmpty() && piece.length <= 3 && piece.toInt() <= 255
    var count = 0
    fun go(start: Int, parts: Int) {
        if (parts == 4) { if (start == n) count += 1; return }
        for (length in 1..3) { if (start + length > n) break; if (valid(digits.substring(start, start + length))) go(start + length, parts + 1) }
    }
    go(0, 0)
    return count
}
"""),
        ("counts-when-four-parts--ignores-leftover", "OFF_BY_ONE",
         "네 조각이 되면 남은 글자와 무관하게 센다.",
         """
fun validIpSplitsCount(digits: String): Int {
    val n = digits.length
    fun valid(piece: String): Boolean { if (piece.isEmpty() || piece.length > 3) return false; if (piece[0] == '0' && piece.length > 1) return false; return piece.toInt() <= 255 }
    var count = 0
    fun go(start: Int, parts: Int) {
        if (parts == 4) { count += 1; return }
        for (length in 1..3) { if (start + length > n) break; if (valid(digits.substring(start, start + length))) go(start + length, parts + 1) }
    }
    go(0, 0)
    return count
}
"""),
        ("strict-255", "OFF_BY_ONE",
         "255 를 넘지 못한다고 하면서 255 자체도 거른다.",
         """
fun validIpSplitsCount(digits: String): Int {
    val n = digits.length
    fun valid(piece: String): Boolean { if (piece.isEmpty() || piece.length > 3) return false; if (piece[0] == '0' && piece.length > 1) return false; return piece.toInt() < 255 }
    var count = 0
    fun go(start: Int, parts: Int) {
        if (parts == 4) { if (start == n) count += 1; return }
        for (length in 1..3) { if (start + length > n) break; if (valid(digits.substring(start, start + length))) go(start + length, parts + 1) }
    }
    go(0, 0)
    return count
}
"""),
        ("rejects-single-zero", "WRONG_BRANCH",
         "0 으로 시작하는 조각을 전부 거른다. 조각 0 하나도 안 된다고 한다.",
         """
fun validIpSplitsCount(digits: String): Int {
    val n = digits.length
    fun valid(piece: String): Boolean { if (piece.isEmpty() || piece.length > 3) return false; if (piece[0] == '0') return false; return piece.toInt() <= 255 }
    var count = 0
    fun go(start: Int, parts: Int) {
        if (parts == 4) { if (start == n) count += 1; return }
        for (length in 1..3) { if (start + length > n) break; if (valid(digits.substring(start, start + length))) go(start + length, parts + 1) }
    }
    go(0, 0)
    return count
}
"""),
    ],
))
