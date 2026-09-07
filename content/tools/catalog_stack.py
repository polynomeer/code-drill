"""스택·단조 자료구조 (역량: 나중에 쓸 정보를 미뤄 두기).

문자열 타입이 없으므로 괄호와 연산자는 정수로 인코딩한다. 인코딩 규칙은 본문에 적어
두며, 그것을 읽는 것도 문제의 일부다 — 실무에서 스펙을 읽는 일과 다르지 않다.
"""

from author import Problem, standard_groups, perf_groups, randoms

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
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 400000},
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
            ("03-worst-case", [list(range(150000, 0, -1))]),
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
            ("03-worst-case", [[min(i, 10000) for i in range(1, 100001)]]),
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
        while (left > 0 && heights[left - 1] >= height) left -= 1
        while (right < heights.size - 1 && heights[right + 1] >= height) right += 1
        val area = height * (right - left + 1)
        if (area > best) best = area
    }
    return best
}
"""),
    ],
))
