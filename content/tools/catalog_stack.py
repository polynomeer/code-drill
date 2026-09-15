"""스택·단조 자료구조 (역량: 나중에 쓸 정보를 미뤄 두기).

문자열 타입이 없으므로 괄호와 연산자는 정수로 인코딩한다. 인코딩 규칙은 본문에 적어
두며, 그것을 읽는 것도 문제의 일부다 — 실무에서 스펙을 읽는 일과 다르지 않다.
"""

from author import Problem, standard_groups, perf_groups, randoms, flat

PROBLEMS = []


# --- 7. 괄호 짝 맞추기 -------------------------------------------------------

def _brackets(tokens):
    pairs = {-1: 1, -2: 2, -3: 3}
    stack = []
    for token in tokens:
        if token > 0:
            stack.append(token)
        else:
            if not stack or stack.pop() != pairs[token]:
                return 0
    return 1 if not stack else 0


PROBLEMS.append(Problem(
    id="bracket-balance",
    title="괄호 짝 맞추기",
    summary="""
괄호 열을 정수 배열로 받는다. 여는 괄호는 양수, 닫는 괄호는 그 짝의 음수다.

| 값 | 뜻 | | 값 | 뜻 |
|---|---|---|---|---|
| `1` | `(` | | `-1` | `)` |
| `2` | `[` | | `-2` | `]` |
| `3` | `{` | | `-3` | `}` |

올바르게 짝이 맞으면 `1`, 아니면 `0` 을 반환한다.
""",
    notes="""
올바르다는 것은 세 가지를 모두 만족한다는 뜻이다. 닫을 때 짝이 맞고, 닫을 것이 남아
있고, 끝났을 때 열린 채로 남은 것이 없다. **셋 중 하나만 빠져도 통과하는 입력이 많아**
어느 하나만 검사한 풀이는 쉬운 예제에서 잘 돌아간다.
""",
    drill_doc="""
Drill.push(token)  // 여는 괄호를 쌓았다
Drill.pop(token)   // 닫는 괄호와 짝을 맞췄다
""",
    constraints="""
- `1 <= tokens.size <= 200_000`
- `tokens[i]` 는 `-3..-1` 또는 `1..3`
""",
    signature=dict(name="isBalanced", parameters=[("tokens", "INT_ARRAY")], returns="INT"),
    groups=standard_groups(),
    reference=_brackets,
    cases={
        "sample": [
            ("01", [[1, -1, 2, -2]]),
            ("02", [[1, 2, -1, -2]]),
        ],
        "boundary": [
            ("01-single-open", [[1]]),
            ("02-single-close", [[-1]]),
            # 짝은 맞는데 종류가 다르다. "개수만 세는" 풀이가 걸린다.
            ("03-type-mismatch", [[1, -2]]),
            # 닫을 것이 없는데 닫는다. 빈 스택 검사가 없으면 터지거나 통과시킨다.
            ("04-close-first", [[-1, 1]]),
            # 끝났는데 열린 채로 남았다. 마지막 검사가 없으면 통과시킨다.
            ("05-left-open", [[1, 2, -2]]),
            ("06-nested-deep", [[1, 2, 3, -3, -2, -1]]),
        ],
        "hidden": [
            ("01-long-balanced", [[1] * 50 + [-1] * 50]),
            ("02-long-unbalanced", [[1] * 50 + [-1] * 49]),
            ("03-interleaved-wrong", [[1, 2, -1, -2, 3, -3]]),
            ("04-mixed-ok", [[3, 1, -1, 2, 3, -3, -2, -3]]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 스택.
//
// 세 가지를 모두 본다. 닫을 때 (1) 스택이 비어 있지 않고 (2) 짝이 맞아야 하며,
// (3) 끝났을 때 스택이 비어 있어야 한다. 하나라도 빼면 통과하는 잘못된 입력이 생긴다.
fun isBalanced(tokens: IntArray): Int {
    val stack = ArrayDeque<Int>()

    for (token in tokens) {
        if (token > 0) {
            stack.addLast(token)
            Drill.push(token)
        } else {
            if (stack.isEmpty()) return 0
            val open = stack.removeLast()
            Drill.pop(open)
            if (open != -token) return 0
        }
    }
    return if (stack.isEmpty()) 1 else 0
}
""",
    mutants=[
        ("counts-only--ignores-type", "WRONG_BRANCH",
         "여는 것과 닫는 것의 개수만 센다. 종류가 어긋난 경우를 통과시킨다.",
         """
fun isBalanced(tokens: IntArray): Int {
    var depth = 0
    for (token in tokens) {
        depth += if (token > 0) 1 else -1
        if (depth < 0) return 0
    }
    return if (depth == 0) 1 else 0
}
"""),
        ("no-final-check--leaves-open", "MISSING_EDGE_CASE",
         "끝났을 때 스택이 비었는지 보지 않아, 열린 채로 끝나도 통과시킨다.",
         """
fun isBalanced(tokens: IntArray): Int {
    val stack = ArrayDeque<Int>()
    for (token in tokens) {
        if (token > 0) {
            stack.addLast(token)
        } else {
            if (stack.isEmpty()) return 0
            if (stack.removeLast() != -token) return 0
        }
    }
    return 1
}
"""),
        ("no-empty-check--closes-nothing", "MISSING_EDGE_CASE",
         "빈 스택에서 닫는 경우를 보지 않는다.",
         """
fun isBalanced(tokens: IntArray): Int {
    val stack = ArrayDeque<Int>()
    for (token in tokens) {
        if (token > 0) {
            stack.addLast(token)
        } else if (stack.isNotEmpty()) {
            if (stack.removeLast() != -token) return 0
        }
    }
    return if (stack.isEmpty()) 1 else 0
}
"""),
    ],
))


# --- 8. 다음 더 큰 값까지의 거리 ---------------------------------------------

def _next_greater(values):
    out = [0] * len(values)
    stack = []
    for i, value in enumerate(values):
        while stack and values[stack[-1]] < value:
            j = stack.pop()
            out[j] = i - j
        stack.append(i)
    return out


PROBLEMS.append(Problem(
    id="next-greater-distance",
    # v2: 성능 케이스를 키웠다. 두 겹 풀이가 한도를 배로만 넘겨, 한가한 머신에서는
    # 통과하고 바쁜 머신에서만 잡혔다 (§12.1 재현성).
    version=2,
    title="다음 더 큰 값까지의 거리",
    summary="""
정수 배열 `values` 가 주어진다. 각 위치마다 **자기보다 큰 값이 처음 나오는 곳까지의
거리**를 담은 배열을 반환한다. 그런 값이 없으면 `0` 이다.
""",
    notes="""
값이 같은 경우는 "더 크다"에 해당하지 않는다. 두 겹으로 돌면 O(n^2) 이고, 아직 답을
못 찾은 위치를 스택에 쌓아 두면 한 번 훑기로 끝난다.
""",
    drill_doc="""
Drill.visit(index, value)  // 새 값을 봤다
Drill.push(index)          // 답을 못 찾은 위치를 미뤄 뒀다
Drill.pop(index)           // 그 위치의 답을 찾았다
Drill.write(index, dist)   // 거리를 적었다
""",
    constraints="""
- `1 <= values.size <= 200_000`
- `-10^9 <= values[i] <= 10^9`
""",
    signature=dict(name="nextGreater", parameters=[("values", "INT_ARRAY")],
                   returns="INT_ARRAY"),
    # 배열을 돌려주므로 기본 64KB 로는 성능 케이스의 정답조차 담지 못한다.
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 800000},
    groups=perf_groups(),
    reference=_next_greater,
    cases={
        "sample": [
            ("01", [[73, 74, 75, 71, 69, 72, 76, 73]]),
            ("02", [[30, 40, 50, 60]]),
        ],
        "boundary": [
            ("01-single", [[5]]),
            # 단조 감소. 아무도 답을 못 찾는다.
            ("02-decreasing", [[9, 7, 5, 3]]),
            # 전부 같다. "크거나 같다"로 쓴 풀이가 걸린다.
            ("03-all-equal", [[4, 4, 4, 4]]),
            ("04-increasing", [[1, 2, 3, 4]]),
            # 마지막에 가장 큰 값이 온다. 스택을 끝까지 비우는지 본다.
            ("05-max-at-tail", [[3, 3, 3, 3, 10]]),
            ("06-negative", [[-5, -9, -1, -3]]),
        ],
        "hidden": [
            ("01-valley", [[5, 1, 1, 1, 6]]),
            ("02-plateau-then-rise", [[2, 2, 2, 3, 2, 2, 4]]),
            ("03-random", [randoms(300, -100, 100, salt=31)]),
        ],
        # 배열을 돌려주는 문제라 원소 수가 곧 출력 크기다. 성능 케이스를 더 키우면
        # 정답 풀이가 OUTPUT_LIMIT 으로 떨어진다 — limits.outputBytes 와 함께 본다.
        "performance": [
            ("01-small", [randoms(3000, -1000000000, 1000000000, salt=32)]),
            ("02-medium", [randoms(20000, -1000000000, 1000000000, salt=33)]),
            # 내림차순이면 두 겹 풀이가 최악이다. 각 위치가 끝까지 훑고 아무것도
            # 찾지 못한다. 답이 전부 0 이라 출력이 짧아 크게 잡을 수 있다.
            ("03-worst-case", [list(range(300000, 0, -1))]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 단조 감소 스택.
//
// 아직 답을 못 찾은 위치를 스택에 쌓아 둔다. 새 값이 들어오면 그보다 작은 것들의
// 답이 한꺼번에 정해진다. 각 위치는 한 번 쌓이고 한 번 빠지므로 O(n) 이다.
fun nextGreater(values: IntArray): IntArray {
    val out = IntArray(values.size)
    val stack = ArrayDeque<Int>()

    for (index in values.indices) {
        Drill.visit(index, values[index])

        while (stack.isNotEmpty() && values[stack.last()] < values[index]) {
            val waiting = stack.removeLast()
            Drill.pop(waiting)
            out[waiting] = index - waiting
            Drill.write(waiting, out[waiting])
        }
        stack.addLast(index)
        Drill.push(index)
    }
    return out
}
""",
    mutants=[
        ("greater-or-equal--counts-ties", "WRONG_BRANCH",
         "같은 값도 '더 크다'로 본다. 값이 이어지는 구간에서 틀린다.",
         """
fun nextGreater(values: IntArray): IntArray {
    val out = IntArray(values.size)
    val stack = ArrayDeque<Int>()
    for (index in values.indices) {
        while (stack.isNotEmpty() && values[stack.last()] <= values[index]) {
            val waiting = stack.removeLast()
            out[waiting] = index - waiting
        }
        stack.addLast(index)
    }
    return out
}
"""),
        ("absolute-index--not-distance", "WRONG_BRANCH",
         "거리가 아니라 인덱스를 적는다.",
         """
fun nextGreater(values: IntArray): IntArray {
    val out = IntArray(values.size)
    val stack = ArrayDeque<Int>()
    for (index in values.indices) {
        while (stack.isNotEmpty() && values[stack.last()] < values[index]) {
            out[stack.removeLast()] = index
        }
        stack.addLast(index)
    }
    return out
}
"""),
        ("quadratic--scans-forward", "PERFORMANCE",
         "각 위치에서 앞을 끝까지 훑어 O(n^2) 다. 내림차순 입력이 최악이다.",
         """
fun nextGreater(values: IntArray): IntArray {
    val out = IntArray(values.size)
    for (index in values.indices) {
        for (next in index + 1 until values.size) {
            Drill.compare(index, next)
            if (values[next] > values[index]) {
                out[index] = next - index
                break
            }
        }
    }
    return out
}
"""),
    ],
))


# --- 9. 히스토그램에서 가장 큰 직사각형 --------------------------------------

def _histogram(heights):
    stack = []
    best = 0
    for i in range(len(heights) + 1):
        current = 0 if i == len(heights) else heights[i]
        while stack and heights[stack[-1]] >= current:
            height = heights[stack.pop()]
            left = stack[-1] + 1 if stack else 0
            best = max(best, height * (i - left))
        stack.append(i)
    return best


PROBLEMS.append(Problem(
    id="histogram-rectangle",
    # v2: 성능 케이스를 키웠다. 두 겹 풀이가 한도를 배로만 넘겨, 한가한 머신에서는
    # 통과하고 바쁜 머신에서만 잡혔다 (§12.1 재현성).
    version=2,
    title="히스토그램에서 가장 큰 직사각형",
    summary="""
너비가 1 인 막대의 높이 배열 `heights` 가 주어진다. 이 히스토그램 안에 들어가는 가장
넓은 직사각형의 넓이를 반환한다.
""",
    notes="""
어떤 막대를 높이로 삼았을 때, 그 직사각형은 **자기보다 낮은 막대를 만나기 전까지**
좌우로 뻗는다. 그 경계를 스택으로 찾으면 한 번 훑기로 끝난다.
""",
    drill_doc="""
Drill.push(index)     // 아직 경계를 못 만난 막대
Drill.pop(index)      // 오른쪽 경계를 만났다
Drill.write(0, area)  // 지금까지의 최대 넓이
""",
    constraints="""
- `1 <= heights.size <= 100_000`
- `0 <= heights[i] <= 10_000`
- 정답은 `Int` 범위를 넘지 않는다
""",
    signature=dict(name="largestRectangle", parameters=[("heights", "INT_ARRAY")],
                   returns="INT"),
    groups=perf_groups(),
    reference=_histogram,
    cases={
        "sample": [
            ("01", [[2, 1, 5, 6, 2, 3]]),
            ("02", [[2, 4]]),
        ],
        "boundary": [
            ("01-single", [[7]]),
            ("02-zero-height", [[0]]),
            # 전부 같으면 답은 전체 폭이다.
            ("03-flat", [[3, 3, 3, 3]]),
            ("04-increasing", [[1, 2, 3, 4, 5]]),
            ("05-decreasing", [[5, 4, 3, 2, 1]]),
            # 가운데가 0 이라 좌우가 끊긴다.
            ("06-split-by-zero", [[4, 4, 0, 5, 5]]),
            # 가장 높은 막대 하나보다 낮고 넓은 쪽이 이긴다.
            ("07-wide-beats-tall", [[10000, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]]),
        ],
        "hidden": [
            ("01-valley", [[6, 2, 5, 4, 5, 1, 6]]),
            ("02-plateau", [[4] * 30]),
            ("03-random", [randoms(200, 0, 10000, salt=41)]),
        ],
        "performance": [
            ("01-small", [randoms(3000, 0, 10000, salt=42)]),
            ("02-medium", [randoms(30000, 0, 10000, salt=43)]),
            # 오름차순이면 두 겹 풀이가 매 위치에서 끝까지 뻗는다. 높이를 10_000 에서
            # 멈춰 두는 이유는 넓이가 Int 를 넘지 않게 하기 위해서다 — 제약에 적은
            # "정답은 Int 범위를 넘지 않는다"를 테스트 데이터가 먼저 지켜야 한다.
            ("03-worst-case", [[min(i, 10000) for i in range(1, 200001)]]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 단조 증가 스택.
//
// 스택에는 "아직 오른쪽 경계를 못 만난 막대"만 남는다. 더 낮은 막대가 오면 그것이
// 경계이므로, 그 순간 넓이를 확정할 수 있다.
//
// 마지막에 높이 0 을 한 번 더 흘려보내 스택을 비운다. 그러지 않으면 오른쪽 끝까지
// 뻗는 직사각형이 계산되지 않는다.
fun largestRectangle(heights: IntArray): Int {
    val stack = ArrayDeque<Int>()
    var best = 0

    for (index in 0..heights.size) {
        val current = if (index == heights.size) 0 else heights[index]

        while (stack.isNotEmpty() && heights[stack.last()] >= current) {
            val top = stack.removeLast()
            Drill.pop(top)
            val height = heights[top]
            val left = if (stack.isEmpty()) 0 else stack.last() + 1
            val area = height * (index - left)
            if (area > best) {
                best = area
                Drill.write(0, best)
            }
        }
        stack.addLast(index)
        Drill.push(index)
    }
    return best
}
""",
    mutants=[
        ("no-flush--misses-tail", "MISSING_EDGE_CASE",
         "마지막에 스택을 비우지 않아 오른쪽 끝까지 뻗는 직사각형을 놓친다.",
         """
fun largestRectangle(heights: IntArray): Int {
    val stack = ArrayDeque<Int>()
    var best = 0
    for (index in heights.indices) {
        while (stack.isNotEmpty() && heights[stack.last()] >= heights[index]) {
            val top = stack.removeLast()
            val left = if (stack.isEmpty()) 0 else stack.last() + 1
            val area = heights[top] * (index - left)
            if (area > best) best = area
        }
        stack.addLast(index)
    }
    return best
}
"""),
        ("single-bar-only--ignores-width", "MISSING_EDGE_CASE",
         "막대 하나씩만 본다. 낮지만 넓은 직사각형을 놓친다.",
         """
fun largestRectangle(heights: IntArray): Int {
    var best = 0
    for (height in heights) if (height > best) best = height
    return best
}
"""),
        ("quadratic--expands-each-bar", "PERFORMANCE",
         "막대마다 좌우로 뻗어 O(n^2) 다. 오름차순 입력이 최악이다.",
         """
fun largestRectangle(heights: IntArray): Int {
    var best = 0
    for (index in heights.indices) {
        var height = heights[index]
        var left = index
        var right = index
        while (left > 0 && heights[left - 1] >= height) { Drill.compare(index, left - 1); left -= 1 }
        while (right < heights.size - 1 && heights[right + 1] >= height) { Drill.compare(index, right + 1); right += 1 }
        val area = height * (right - left + 1)
        if (area > best) best = area
    }
    return best
}
"""),
    ],
))


# --- 70. 후위 표기식 계산 ------------------------------------------------------------

def _evaluate_rpn(tokens):
    stack = []
    for token in tokens:
        if token >= 0:
            stack.append(token)
            continue
        right = stack.pop()
        left = stack.pop()
        if token == -1:
            stack.append(left + right)
        elif token == -2:
            stack.append(left - right)
        elif token == -3:
            stack.append(left * right)
        else:
            # 0 으로 향하는 나눗셈. 파이썬의 // 는 음수에서 내림이라 따로 맞춘다.
            quotient = abs(left) // abs(right)
            stack.append(quotient if (left >= 0) == (right >= 0) else -quotient)
    return stack[-1]


PROBLEMS.append(Problem(
    id="evaluate-rpn",
    title="후위 표기식 계산",
    summary="""
후위 표기식(연산자가 피연산자 뒤에 오는 식)을 정수 배열 `tokens` 로 받는다. `0` 이상은
피연산자이고, 음수는 연산자다.

| 값 | 연산 | 뜻 |
|---|---|---|
| `-1` | `+` | 더하기 |
| `-2` | `-` | 빼기 (앞 것에서 뒤 것을) |
| `-3` | `*` | 곱하기 |
| `-4` | `/` | 나누기 — **0 을 향해** 자른다 (`7 / -2 = -3`) |

식의 값을 반환한다. 식은 항상 올바르고, 0 으로 나누는 일은 없으며, 중간 값도 `Int` 안이다.

예: `[2, 1, -1, 3, -3]` 은 `(2 + 1) * 3 = 9` 다.
""",
    notes="""
피연산자는 쌓고, 연산자를 만나면 **둘을 꺼내 계산한 값을 다시 쌓는다.** 빼기와 나누기는
순서가 있다 — 나중에 꺼낸 것이 앞 피연산자다. 중간 값이 음수가 될 수 있고, 그 음수를
피연산자로 쓸 때 "음수는 연산자"라는 인코딩과 헷갈리면 안 된다 — 인코딩은 입력에만 있다.
""",
    drill_doc="""
Drill.push(value)             // 피연산자를 쌓았다
Drill.pop(value)              // 피연산자를 꺼냈다
Drill.write(0, result)        // 연산 결과를 다시 쌓았다
""",
    constraints="""
- `1 <= tokens.size <= 100_000`
- 피연산자는 `0 <= v <= 10^4`, 연산자는 `-1`, `-2`, `-3`, `-4`
- 중간 값과 답은 `Int` 범위 안이고, 0 으로 나누지 않는다
""",
    signature=dict(name="evaluateRpn", parameters=[("tokens", "INT_ARRAY")], returns="INT"),
    groups=standard_groups(),
    reference=_evaluate_rpn,
    cases={
        "sample": [
            ("01", [[2, 1, -1, 3, -3]]),
            ("02", [[4, 13, 5, -4, -1]]),
        ],
        "boundary": [
            ("01-single", [[42]]),
            # 순서. 5 - 3 이지 3 - 5 가 아니다.
            ("02-subtract-order", [[5, 3, -2]]),
            ("03-divide-order", [[20, 4, -4]]),
            # 0 을 향한 나눗셈. 내림이면 -4 가 된다.
            ("04-divide-toward-zero", [[7, 0, 2, -2, -4]]),
            ("05-divide-negative-by-negative", [[0, 7, -2, 0, 2, -2, -4]]),
            # 중간 값이 음수인 피연산자.
            ("06-negative-intermediate", [[1, 5, -2, 3, -3]]),
            ("07-zero-operand", [[0, 5, -3]]),
        ],
        "hidden": [
            ("01-deep", [[1] + flat([1, -1] for _ in range(50))]),
            ("02-mixed", [[10, 6, 9, 3, -1, 11, -3, -4, -3, 17, -1, 5, -1]]),
            ("03-long-chain", [[3] + flat([2, -3, 1, -2] for _ in range(20))]),
            ("04-large", [[1] + flat([v % 10, -1] for v in randoms(20000, 0, 9, salt=2801))]),
            # 중간 값 -4 가 스택에 둘 이상 남은 채로 나온다. 그것을 연산자로 읽으면 무너진다.
            ("05-negative-intermediate-deep", [[7, 8, 1, 5, -2, 3, -3, -1, -1]]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 피연산자는 쌓고 연산자는 둘을 꺼낸다.
fun evaluateRpn(tokens: IntArray): Int {
    val stack = ArrayDeque<Int>()
    for (token in tokens) {
        if (token >= 0) { stack.addLast(token); Drill.push(token); continue }
        val right = stack.removeLast()
        val left = stack.removeLast()
        Drill.pop(right)
        Drill.pop(left)
        val value = when (token) {
            -1 -> left + right
            -2 -> left - right
            -3 -> left * right
            else -> left / right
        }
        stack.addLast(value)
        Drill.write(0, value)
    }
    return stack.last()
}
""",
    mutants=[
        ("swapped-operands", "WRONG_BRANCH",
         "먼저 꺼낸 것을 앞 피연산자로 쓴다. 빼기와 나누기의 순서가 뒤집힌다.",
         """
fun evaluateRpn(tokens: IntArray): Int {
    val stack = ArrayDeque<Int>()
    for (token in tokens) {
        if (token >= 0) { stack.addLast(token); continue }
        val left = stack.removeLast()
        val right = stack.removeLast()
        stack.addLast(when (token) { -1 -> left + right; -2 -> left - right; -3 -> left * right; else -> left / right })
    }
    return stack.last()
}
"""),
        ("floor-division", "MISSING_EDGE_CASE",
         "나눗셈을 내림으로 한다. 음수에서 0 을 향해 자르지 않는다.",
         """
fun evaluateRpn(tokens: IntArray): Int {
    val stack = ArrayDeque<Int>()
    for (token in tokens) {
        if (token >= 0) { stack.addLast(token); continue }
        val right = stack.removeLast()
        val left = stack.removeLast()
        stack.addLast(when (token) { -1 -> left + right; -2 -> left - right; -3 -> left * right; else -> Math.floorDiv(left, right) })
    }
    return stack.last()
}
"""),
        ("negative-intermediate-as-operator", "WRONG_ALGORITHM",
         "중간 값이 음수면 그것을 연산자로 착각한다. 인코딩은 입력에만 있다.",
         """
fun evaluateRpn(tokens: IntArray): Int {
    val stack = ArrayDeque<Int>()
    fun apply(op: Int) {
        val right = stack.removeLast(); val left = stack.removeLast()
        val value = when (op) { -1 -> left + right; -2 -> left - right; -3 -> left * right; else -> left / right }
        if (value < 0 && value >= -4 && stack.size >= 2) apply(value) else stack.addLast(value)
    }
    for (token in tokens) if (token >= 0) stack.addLast(token) else apply(token)
    return stack.last()
}
"""),
    ],
))


# --- 71. 소행성 충돌 ---------------------------------------------------------------

def _asteroids(sizes):
    stack = []
    for size in sizes:
        alive = True
        while alive and size < 0 and stack and stack[-1] > 0:
            if stack[-1] < -size:
                stack.pop()
            elif stack[-1] == -size:
                stack.pop()
                alive = False
            else:
                alive = False
        if alive:
            stack.append(size)
    return stack


PROBLEMS.append(Problem(
    id="asteroid-collision",
    title="소행성 충돌",
    summary="""
한 줄로 늘어선 소행성들이 배열 `sizes` 로 주어진다. 절댓값이 크기이고, 부호가 방향이다 —
양수는 오른쪽으로, 음수는 왼쪽으로 움직인다. 속도는 모두 같다.

두 소행성이 만나면 **작은 쪽이 부서진다.** 크기가 같으면 둘 다 부서진다. 같은 방향으로
움직이는 소행성은 만나지 않는다. 모든 충돌이 끝난 뒤 남는 소행성들을 순서대로 반환한다.

예: `[5, 10, -5]` 는 `10` 과 `-5` 가 만나 `-5` 가 부서지고 `[5, 10]` 이 남는다.
""",
    notes="""
충돌은 **오른쪽으로 가는 것 뒤에 왼쪽으로 가는 것이 올 때만** 일어난다. 오른쪽으로 가는
것들을 쌓아 두고, 왼쪽으로 가는 것이 오면 스택의 꼭대기와 견준다 — 이기면 계속 견주고,
지면 사라지고, 같으면 둘 다 사라진다. 왼쪽으로 가는 것이 살아남으면 그것도 쌓인다.
""",
    drill_doc="""
Drill.push(size)              // 살아남아 쌓였다
Drill.pop(size)               // 부서졌다
Drill.compare(left, right)    // 두 소행성이 만났다
""",
    constraints="""
- `1 <= sizes.size <= 100_000`
- `-1000 <= sizes[i] <= 1000`, `0` 은 없다
""",
    signature=dict(name="asteroids", parameters=[("sizes", "INT_ARRAY")], returns="INT_ARRAY"),
    groups=standard_groups(),
    reference=_asteroids,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 2000000},
    cases={
        "sample": [
            ("01", [[5, 10, -5]]),
            ("02", [[8, -8]]),
        ],
        "boundary": [
            ("01-single", [[3]]),
            # 왼쪽으로 가는 것이 먼저면 아무도 안 만난다.
            ("02-left-first", [[-2, -1, 1, 2]]),
            ("03-all-right", [[1, 2, 3]]),
            ("04-all-left", [[-3, -2, -1]]),
            # 하나가 여럿을 연달아 부순다.
            ("05-one-breaks-many", [[1, 2, 3, -10]]),
            # 같은 크기. 둘 다 사라지고, 다음 것은 그 앞과 견준다.
            ("06-equal-then-continue", [[5, 3, -3, -6]]),
            ("07-survivor-then-more", [[10, -5, -15]]),
        ],
        "hidden": [
            ("01-mixed", [[-2, 1, 1, -1, -1, 3, -4, 2]]),
            ("02-random-small", [[v if v != 0 else 1 for v in randoms(30, -20, 20, salt=2901)]]),
            ("03-random-medium", [[v if v != 0 else 1 for v in randoms(3000, -1000, 1000, salt=2902)]]),
            ("04-chain-breaks", [list(range(1, 51)) + [-100]]),
            ("05-large", [[v if v != 0 else 1 for v in randoms(100000, -1000, 1000, salt=2903)]]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 오른쪽으로 가는 것을 쌓고, 왼쪽으로 가는 것이 오면 견준다.
fun asteroids(sizes: IntArray): IntArray {
    val stack = ArrayDeque<Int>()
    for (size in sizes) {
        var alive = true
        while (alive && size < 0 && stack.isNotEmpty() && stack.last() > 0) {
            val top = stack.last()
            Drill.compare(top, size)
            when {
                top < -size -> { stack.removeLast(); Drill.pop(top) }
                top == -size -> { stack.removeLast(); Drill.pop(top); alive = false }
                else -> alive = false
            }
        }
        if (alive) { stack.addLast(size); Drill.push(size) }
    }
    return stack.toIntArray()
}
""",
    mutants=[
        ("compares-signed", "WRONG_BRANCH",
         "크기를 절댓값이 아니라 부호 있는 값으로 견준다. 왼쪽으로 가는 것이 늘 진다.",
         """
fun asteroids(sizes: IntArray): IntArray {
    val stack = ArrayDeque<Int>()
    for (size in sizes) {
        var alive = true
        while (alive && size < 0 && stack.isNotEmpty() && stack.last() > 0) {
            val top = stack.last()
            when {
                top < size -> stack.removeLast()
                top == size -> { stack.removeLast(); alive = false }
                else -> alive = false
            }
        }
        if (alive) stack.addLast(size)
    }
    return stack.toIntArray()
}
"""),
        ("breaks-only-one", "MISSING_EDGE_CASE",
         "왼쪽으로 가는 것이 이겨도 하나만 부수고 멈춘다. 연달아 부수지 못한다.",
         """
fun asteroids(sizes: IntArray): IntArray {
    val stack = ArrayDeque<Int>()
    for (size in sizes) {
        var alive = true
        if (size < 0 && stack.isNotEmpty() && stack.last() > 0) {
            val top = stack.last()
            when {
                top < -size -> stack.removeLast()
                top == -size -> { stack.removeLast(); alive = false }
                else -> alive = false
            }
        }
        if (alive) stack.addLast(size)
    }
    return stack.toIntArray()
}
"""),
        ("equal-keeps-mover", "OFF_BY_ONE",
         "크기가 같을 때 쌓인 것만 부서지고 움직이는 것은 살아남는다. 둘 다 부서져야 한다.",
         """
fun asteroids(sizes: IntArray): IntArray {
    val stack = ArrayDeque<Int>()
    for (size in sizes) {
        var alive = true
        while (alive && size < 0 && stack.isNotEmpty() && stack.last() > 0) {
            val top = stack.last()
            if (top <= -size) stack.removeLast() else alive = false
        }
        if (alive) stack.addLast(size)
    }
    return stack.toIntArray()
}
"""),
        ("any-direction-collides", "WRONG_ALGORITHM",
         "방향을 보지 않고 이웃끼리 부딪힌다고 본다. 같은 방향은 만나지 않는다.",
         """
fun asteroids(sizes: IntArray): IntArray {
    val stack = ArrayDeque<Int>()
    for (size in sizes) {
        var alive = true
        while (alive && stack.isNotEmpty()) {
            val top = stack.last()
            if (Math.abs(top) < Math.abs(size)) stack.removeLast()
            else if (Math.abs(top) == Math.abs(size)) { stack.removeLast(); alive = false }
            else alive = false
        }
        if (alive) stack.addLast(size)
    }
    return stack.toIntArray()
}
"""),
    ],
))


# --- 72. 격자에서 가장 큰 직사각형 (EXPERT) ------------------------------------------------

def _maximal_rectangle(grid):
    rows = len(grid)
    cols = len(grid[0]) if rows else 0
    heights = [0] * cols
    best = 0
    for r in range(rows):
        for c in range(cols):
            heights[c] = heights[c] + 1 if grid[r][c] == 1 else 0
        best = max(best, _histogram(heights))
    return best


PROBLEMS.append(Problem(
    id="maximal-rectangle",
    title="격자에서 가장 큰 직사각형",
    summary="""
`0` 과 `1` 의 격자 `grid` 가 주어진다. **`1` 로만 채워진 직사각형** 중 가장 넓은 것의
넓이(칸 수)를 반환한다. 없으면 `0` 이다.
""",
    notes="""
두 문제를 엮는다. 행마다 "이 칸 위로 `1` 이 몇 개 이어져 있나"를 세면 그 행은 히스토그램이고,
히스토그램에서 가장 큰 직사각형은 스택으로 한 번 훑어 나온다. 행마다 그것을 하면 끝이다 —
행마다 O(cols), 전체 O(rows × cols).

가장 큰 직사각형의 아랫변은 어느 행에 닿아 있고, 그 행의 히스토그램 안에 그 직사각형이
있다. 그래서 행마다의 최선 중 최대가 답이다.
""",
    drill_doc="""
Drill.write(c, height)        // 이 행에서 c 열의 막대 높이
Drill.push(c)                 // 경계를 못 만난 막대
Drill.pop(c)                  // 경계를 만나 넓이를 쟀다
""",
    constraints="""
- `0 <= rows <= 200`, `0 <= cols <= 5_000`
- 모든 행의 길이는 같다
- 각 칸은 `0` 또는 `1`
""",
    signature=dict(name="maximalRectangle", parameters=[("grid", "INT_MATRIX")], returns="INT"),
    # 열의 쌍을 전부 시험하는 풀이는 열 수의 제곱이다. 입력을 더 못 키워(격자 하나가 2MB) 시간을 조인다.
    groups=perf_groups(time_multiplier=0.25),
    reference=_maximal_rectangle,
    cases={
        "sample": [
            ("01", [[[1, 0, 1, 0, 0], [1, 0, 1, 1, 1], [1, 1, 1, 1, 1], [1, 0, 0, 1, 0]]]),
            ("02", [[[0, 1], [1, 0]]]),
        ],
        "boundary": [
            ("01-empty", [[]]),
            ("02-no-columns", [[[], []]]),
            ("03-all-zero", [[[0, 0], [0, 0]]]),
            ("04-all-one", [[[1, 1, 1], [1, 1, 1]]]),
            ("05-single-one", [[[1]]]),
            # 세로로 긴 직사각형. 행 하나만 보면 놓친다.
            ("06-tall", [[[1, 0], [1, 0], [1, 0], [1, 1]]]),
            # 정사각형이 아닌 최선. 행과 열을 맞바꾸면 틀린다.
            ("07-wide", [[[1, 1, 1, 1], [0, 1, 1, 0]]]),
            # 높이가 끊긴다. 위로 이어진 개수를 0 으로 되돌려야 한다.
            ("08-broken-column", [[[1], [0], [1], [1]]]),
        ],
        "hidden": [
            ("01-staircase", [[[1, 0, 0, 0], [1, 1, 0, 0], [1, 1, 1, 0], [1, 1, 1, 1]]]),
            ("02-random-small", [[[1 if v < 7 else 0 for v in randoms(8, 0, 9, salt=3001 + r)] for r in range(6)]]),
            ("03-random-medium", [[[1 if v < 8 else 0 for v in randoms(60, 0, 9, salt=3101 + r)] for r in range(40)]]),
            ("04-diagonal-zeros", [[[0 if r == c else 1 for c in range(10)] for r in range(10)]]),
        ],
        "performance": [
            # 높이가 고르게 커야 좌우로 훑는 풀이가 행 끝까지 간다 — 전부 1 이거나 행마다 0 이 하나.
            ("01-small", [[[1] * 1000 for _ in range(50)]]),
            ("02-medium", [[[0 if c == (r * 37) % 2500 else 1 for c in range(2500)] for r in range(120)]]),
            ("03-large", [[[1] * 5000 for _ in range(200)]]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 행마다 히스토그램 + 단조 스택.
fun maximalRectangle(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val heights = IntArray(cols)
    val stack = IntArray(cols + 1)
    var best = 0
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            heights[c] = if (grid[r][c] == 1) heights[c] + 1 else 0
            Drill.write(c, heights[c])
        }
        var top = 0
        for (i in 0..cols) {
            val current = if (i == cols) 0 else heights[i]
            while (top > 0 && heights[stack[top - 1]] >= current) {
                val h = heights[stack[--top]]
                Drill.pop(stack[top])
                val left = if (top == 0) 0 else stack[top - 1] + 1
                best = maxOf(best, h * (i - left))
            }
            if (i < cols) { stack[top++] = i; Drill.push(i) }
        }
    }
    return best
}
""",
    mutants=[
        ("row-only--no-height", "WRONG_ALGORITHM",
         "행마다 연속한 1 의 길이만 잰다. 세로로 뻗는 직사각형을 보지 못한다.",
         """
fun maximalRectangle(grid: Array<IntArray>): Int {
    var best = 0
    for (row in grid) {
        var run = 0
        for (v in row) { run = if (v == 1) run + 1 else 0; best = maxOf(best, run) }
    }
    return best
}
"""),
        ("height-never-resets", "MISSING_EDGE_CASE",
         "0 을 만나도 위로 이어진 개수를 되돌리지 않는다. 끊긴 열을 이어진 것으로 본다.",
         """
fun maximalRectangle(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val heights = IntArray(cols)
    var best = 0
    for (r in 0 until rows) {
        for (c in 0 until cols) if (grid[r][c] == 1) heights[c] += 1
        val stack = ArrayDeque<Int>()
        for (i in 0..cols) {
            val current = if (i == cols) 0 else heights[i]
            while (stack.isNotEmpty() && heights[stack.last()] >= current) {
                val h = heights[stack.removeLast()]
                val left = if (stack.isEmpty()) 0 else stack.last() + 1
                best = maxOf(best, h * (i - left))
            }
            if (i < cols) stack.addLast(i)
        }
    }
    return best
}
"""),
        ("square-only", "WRONG_BRANCH",
         "정사각형만 찾는다. 가로나 세로로 긴 직사각형을 놓친다.",
         """
fun maximalRectangle(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val side = Array(rows + 1) { IntArray(cols + 1) }
    var best = 0
    for (r in 1..rows) for (c in 1..cols) {
        if (grid[r - 1][c - 1] == 1) {
            side[r][c] = minOf(side[r - 1][c], side[r][c - 1], side[r - 1][c - 1]) + 1
            best = maxOf(best, side[r][c] * side[r][c])
        }
    }
    return best
}
"""),
        ("all-column-pairs--per-row", "PERFORMANCE",
         "행마다 히스토그램을 만든 뒤 열의 모든 쌍에 대해 그 사이의 최소 높이로 넓이를 잰다. O(rows × cols²).",
         """
fun maximalRectangle(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val heights = IntArray(cols)
    var best = 0
    for (r in 0 until rows) {
        for (c in 0 until cols) heights[c] = if (grid[r][c] == 1) heights[c] + 1 else 0
        for (left in 0 until cols) {
            var lowest = heights[left]
            for (right in left until cols) {
                Drill.compare(left, right)
                lowest = minOf(lowest, heights[right])
                if (lowest == 0) break
                best = maxOf(best, lowest * (right - left + 1))
            }
        }
    }
    return best
}
"""),
    ],
))
