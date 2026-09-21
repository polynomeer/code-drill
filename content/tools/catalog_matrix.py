"""격자 (역량: 2차원 좌표를 다루고 경계를 지키기).

네 문제가 격자가 드나드는 **네 가지 방향**을 모두 지난다. 문자열 때와 같은 이유다 —
하나만 있으면 인코딩의 한쪽 방향만 확인되고, 반대쪽은 처음 쓰는 사람이 발견한다.

| 문제 | 입력 → 출력 |
|---|---|
| island-perimeter | INT_MATRIX → INT |
| spiral-order | INT_MATRIX → INT_ARRAY |
| rotate-grid | INT_MATRIX → INT_MATRIX |
| spiral-fill | INT → INT_MATRIX |

**직사각형이 아닌 케이스가 핵심이다.** 정사각 격자만 두면 행과 열을 맞바꾼 구현이
그대로 통과한다. rotate-grid 의 2×3 케이스와 빈 격자·0열 격자가 그 자리를 지킨다.
"""

from author import Problem, perf_groups, randoms, standard_groups, flat, shuffled

PROBLEMS = []


# --- 35. 섬의 둘레 -----------------------------------------------------------

def _perimeter(grid):
    rows = len(grid)
    cols = len(grid[0]) if rows else 0
    total = 0
    for r in range(rows):
        for c in range(cols):
            if grid[r][c] != 1:
                continue
            total += 4
            if r > 0 and grid[r - 1][c] == 1:
                total -= 1
            if r + 1 < rows and grid[r + 1][c] == 1:
                total -= 1
            if c > 0 and grid[r][c - 1] == 1:
                total -= 1
            if c + 1 < cols and grid[r][c + 1] == 1:
                total -= 1
    return total


def _rect(rows, cols, values):
    return [values[r * cols:(r + 1) * cols] for r in range(rows)]


PROBLEMS.append(Problem(
    id="island-perimeter",
    title="섬의 둘레",
    summary="""
`0` 은 물, `1` 은 땅인 격자 `grid` 에서 **땅 전체의 둘레**를 반환한다.

한 칸의 네 변 중, 바깥이거나 물과 맞닿은 변만 둘레에 든다.
""",
    notes="""
땅이 여러 덩어리로 나뉘어 있어도 전부 더한다. 땅이 없으면 `0` 이다.
""",
    drill_doc="""
Drill.visit(r * cols + c, grid[r][c])  // 땅 한 칸을 봤다
""",
    constraints="""
- `0 <= rows, cols <= 300`
- 모든 행의 길이는 같다
- 각 칸은 `0` 또는 `1`
""",
    signature=dict(name="perimeter", parameters=[("grid", "INT_MATRIX")], returns="INT"),
    groups=perf_groups(),
    reference=_perimeter,
    cases={
        "sample": [
            ("01", [[[0, 1, 0, 0], [1, 1, 1, 0], [0, 1, 0, 0], [1, 1, 0, 0]]]),
            ("02", [[[1]]]),
        ],
        "boundary": [
            # 빈 격자. 인코딩이 `0,0` 으로 실려야 한다.
            ("01-empty", [[]]),
            # 행은 있는데 열이 없다. 원소 수만으로는 빈 격자와 구분되지 않는다.
            ("02-no-columns", [[[], [], []]]),
            ("03-all-water", [[[0, 0], [0, 0]]]),
            # 전부 땅. 안쪽 변이 전부 상쇄된다.
            ("04-all-land", [[[1, 1], [1, 1]]]),
            # 세로로만 붙어 있다. 가로 이웃만 보는 구현이 여기서 갈린다.
            ("05-vertical-pair", [[[1], [1]]]),
            ("06-horizontal-pair", [[[1, 1]]]),
            ("07-single-row", [[[1, 0, 1, 0, 1]]]),
            ("08-single-column", [[[1], [0], [1]]]),
            # 대각선은 맞닿은 것이 아니다.
            ("09-diagonal", [[[1, 0], [0, 1]]]),
        ],
        "hidden": [
            ("01-ring", [[[1, 1, 1], [1, 0, 1], [1, 1, 1]]]),
            ("02-comb", [[[1, 0, 1, 0, 1], [1, 0, 1, 0, 1], [1, 1, 1, 1, 1]]]),
            ("03-two-islands", [[[1, 1, 0, 0], [1, 0, 0, 1], [0, 0, 1, 1]]]),
            ("04-tall", [_rect(6, 2, [1, 0, 1, 1, 0, 1, 1, 0, 0, 1, 1, 1])]),
        ],
        "performance": [
            # 90,000 칸. 케이스 한 줄이 180KB 를 넘으므로 인코딩 자체가 시험된다.
            ("01-dense-300", [_rect(300, 300, randoms(300 * 300, 0, 1, salt=610))]),
            ("02-sparse-300", [_rect(300, 300, [1 if v == 0 else 0
                                                for v in randoms(300 * 300, 0, 7, salt=611)])]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 칸마다 네 변에서 시작해 이웃과 맞닿은 변을 뺀다.
//
// 덩어리를 찾아 다닐 필요가 없다. 둘레는 칸마다 독립적으로 정해지므로 한 번 훑으면
// 끝나고, 그래서 섬이 몇 개든 같은 코드가 답한다.
fun perimeter(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    var total = 0

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if (grid[r][c] != 1) continue
            Drill.visit(r * cols + c, grid[r][c])

            total += 4
            if (r > 0 && grid[r - 1][c] == 1) total -= 1
            if (r + 1 < rows && grid[r + 1][c] == 1) total -= 1
            if (c > 0 && grid[r][c - 1] == 1) total -= 1
            if (c + 1 < cols && grid[r][c + 1] == 1) total -= 1
        }
    }
    return total
}
""",
    mutants=[
        ("horizontal-only--ignores-rows", "MISSING_EDGE_CASE",
         "좌우 이웃만 뺀다. 세로로 붙은 땅의 공유 변이 둘레에 남는다.",
         """
fun perimeter(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    var total = 0
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if (grid[r][c] != 1) continue
            total += 4
            if (c > 0 && grid[r][c - 1] == 1) total -= 1
            if (c + 1 < cols && grid[r][c + 1] == 1) total -= 1
        }
    }
    return total
}
"""),
        ("counts-water--ignores-zero", "WRONG_BRANCH",
         "물 칸도 땅으로 세어 격자 전체의 둘레를 낸다.",
         """
fun perimeter(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    var total = 0
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            total += 4
            if (r > 0) total -= 1
            if (r + 1 < rows) total -= 1
            if (c > 0) total -= 1
            if (c + 1 < cols) total -= 1
        }
    }
    return total
}
"""),
        ("subtracts-two--double-counts-shared-edge", "OFF_BY_ONE",
         "맞닿은 변을 양쪽에서 한 번씩 빼고 또 뺀다.",
         """
fun perimeter(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    var total = 0
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if (grid[r][c] != 1) continue
            total += 4
            if (r > 0 && grid[r - 1][c] == 1) total -= 2
            if (r + 1 < rows && grid[r + 1][c] == 1) total -= 2
            if (c > 0 && grid[r][c - 1] == 1) total -= 2
            if (c + 1 < cols && grid[r][c + 1] == 1) total -= 2
        }
    }
    return total
}
"""),
    ],
))


# --- 36. 나선 순회 -----------------------------------------------------------

def _spiral_order(grid):
    """방향 벡터로 걸어 다닌다.

    코틀린 참조 구현은 네 경계를 좁혀 가는 방식이라 접근이 다르다. 같은 방식으로 두
    번 쓰면 같은 착각을 두 번 하게 된다.
    """
    rows = len(grid)
    cols = len(grid[0]) if rows else 0
    seen = [[False] * cols for _ in range(rows)]
    out = []
    r = c = 0
    dr, dc = 0, 1
    for _ in range(rows * cols):
        out.append(grid[r][c])
        seen[r][c] = True
        nr, nc = r + dr, c + dc
        if not (0 <= nr < rows and 0 <= nc < cols and not seen[nr][nc]):
            dr, dc = dc, -dr
            nr, nc = r + dr, c + dc
        r, c = nr, nc
    return out


PROBLEMS.append(Problem(
    id="spiral-order",
    title="나선으로 읽기",
    summary="""
격자 `grid` 를 바깥부터 시계 방향 나선으로 훑은 순서대로 반환한다.

왼쪽 위에서 시작해 오른쪽으로 간다.
""",
    notes="""
빈 격자는 빈 배열을 반환한다.
""",
    drill_doc="""
Drill.write(head, value)  // 결과 배열의 head 번째를 채웠다
""",
    constraints="""
- `0 <= rows, cols <= 60`
- 모든 행의 길이는 같다
- `-1_000 <= grid[r][c] <= 1_000`
""",
    signature=dict(name="spiralOrder", parameters=[("grid", "INT_MATRIX")], returns="INT_ARRAY"),
    groups=standard_groups(),
    reference=_spiral_order,
    cases={
        "sample": [
            ("01", [[[1, 2, 3], [4, 5, 6], [7, 8, 9]]]),
            ("02", [[[1, 2], [3, 4]]]),
        ],
        "boundary": [
            ("01-empty", [[]]),
            ("02-no-columns", [[[], []]]),
            ("03-single-cell", [[[7]]]),
            # 한 줄짜리. 경계를 좁히는 구현이 마지막 줄을 두 번 읽기 쉽다.
            ("04-single-row", [[[1, 2, 3, 4]]]),
            ("05-single-column", [[[1], [2], [3], [4]]]),
            ("06-two-by-three", [[[1, 2, 3], [4, 5, 6]]]),
            ("07-three-by-two", [[[1, 2], [3, 4], [5, 6]]]),
            ("08-negatives", [[[-1, -2], [-3, -4]]]),
        ],
        "hidden": [
            ("01-four-by-four", [_rect(4, 4, list(range(1, 17)))]),
            ("02-odd-square", [_rect(5, 5, list(range(1, 26)))]),
            ("03-wide", [_rect(2, 7, list(range(1, 15)))]),
            ("04-tall", [_rect(7, 2, list(range(1, 15)))]),
            ("05-random", [_rect(6, 5, randoms(30, -1000, 1000, salt=620))]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 네 경계를 안쪽으로 좁혀 간다.
//
// 한 줄만 남았을 때가 함정이다. 위쪽 줄을 읽고 top 을 내린 뒤에도 아래쪽 줄을 그대로
// 읽으면 같은 줄을 두 번 읽는다. `top <= bottom` 과 `left <= right` 가 그 자리를 막는다.
fun spiralOrder(grid: Array<IntArray>): IntArray {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val out = IntArray(rows * cols)
    var head = 0

    var top = 0
    var bottom = rows - 1
    var left = 0
    var right = cols - 1

    while (head < out.size) {
        for (c in left..right) {
            out[head] = grid[top][c]
            Drill.write(head, out[head])
            head += 1
        }
        top += 1

        for (r in top..bottom) {
            out[head] = grid[r][right]
            Drill.write(head, out[head])
            head += 1
        }
        right -= 1

        if (top <= bottom) {
            for (c in right downTo left) {
                out[head] = grid[bottom][c]
                Drill.write(head, out[head])
                head += 1
            }
            bottom -= 1
        }

        if (left <= right) {
            for (r in bottom downTo top) {
                out[head] = grid[r][left]
                Drill.write(head, out[head])
                head += 1
            }
            left += 1
        }
    }
    return out
}
""",
    mutants=[
        ("row-major--not-spiral", "WRONG_ALGORITHM",
         "나선이 아니라 행 순서대로 읽는다.",
         """
fun spiralOrder(grid: Array<IntArray>): IntArray {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val out = IntArray(rows * cols)
    var head = 0
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            out[head] = grid[r][c]
            head += 1
        }
    }
    return out
}
"""),
        ("column-major--transposed", "WRONG_ALGORITHM",
         "열 순서대로 읽는다.",
         """
fun spiralOrder(grid: Array<IntArray>): IntArray {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val out = IntArray(rows * cols)
    var head = 0
    for (c in 0 until cols) {
        for (r in 0 until rows) {
            out[head] = grid[r][c]
            head += 1
        }
    }
    return out
}
"""),
        ("outer-ring-only--stops-early", "MISSING_EDGE_CASE",
         "바깥 한 바퀴만 돌고 안쪽으로 들어가지 않는다.",
         """
fun spiralOrder(grid: Array<IntArray>): IntArray {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val out = IntArray(rows * cols)
    var head = 0
    var top = 0
    var bottom = rows - 1
    var left = 0
    var right = cols - 1
    if (head < out.size) {
        for (c in left..right) { out[head] = grid[top][c]; head += 1 }
        top += 1
        for (r in top..bottom) { out[head] = grid[r][right]; head += 1 }
        right -= 1
        if (top <= bottom) {
            for (c in right downTo left) { out[head] = grid[bottom][c]; head += 1 }
            bottom -= 1
        }
        if (left <= right) {
            for (r in bottom downTo top) { out[head] = grid[r][left]; head += 1 }
            left += 1
        }
    }
    return out
}
"""),
    ],
))


# --- 37. 격자 회전 -----------------------------------------------------------

def _rotate(grid):
    return [list(row) for row in zip(*grid[::-1])]


PROBLEMS.append(Problem(
    id="rotate-grid",
    title="격자 돌리기",
    summary="""
격자 `grid` 를 시계 방향으로 90도 돌린 격자를 반환한다.

`rows × cols` 격자를 돌리면 **`cols × rows` 격자**가 된다.
""",
    notes="""
입력이 정사각형이라는 보장은 없다. 결과의 행 수는 입력의 열 수와 같다.
""",
    drill_doc="""
Drill.write(index, value)  // 결과 격자의 index 번째 칸을 채웠다 (행 우선)
""",
    constraints="""
- `0 <= rows, cols <= 60`
- 모든 행의 길이는 같다
- `-1_000 <= grid[r][c] <= 1_000`
""",
    signature=dict(name="rotate", parameters=[("grid", "INT_MATRIX")], returns="INT_MATRIX"),
    groups=standard_groups(),
    reference=_rotate,
    cases={
        "sample": [
            ("01", [[[1, 2], [3, 4]]]),
            ("02", [[[1, 2, 3], [4, 5, 6], [7, 8, 9]]]),
        ],
        "boundary": [
            ("01-empty", [[]]),
            ("02-no-columns", [[[], [], []]]),
            ("03-single-cell", [[[5]]]),
            # 직사각형. 결과의 크기가 입력과 달라 크기를 함께 실어야만 맞는다.
            ("04-two-by-three", [[[1, 2, 3], [4, 5, 6]]]),
            ("05-three-by-two", [[[1, 2], [3, 4], [5, 6]]]),
            ("06-single-row", [[[1, 2, 3]]]),
            ("07-single-column", [[[1], [2], [3]]]),
            ("08-negatives", [[[-1, 0], [0, -2]]]),
        ],
        "hidden": [
            ("01-four-by-four", [_rect(4, 4, list(range(1, 17)))]),
            ("02-wide", [_rect(2, 8, list(range(1, 17)))]),
            ("03-tall", [_rect(8, 2, list(range(1, 17)))]),
            ("04-random", [_rect(5, 7, randoms(35, -1000, 1000, salt=630))]),
            # 회전 대칭이라 방향을 틀려도 값이 같아 보일 수 있는 격자.
            ("05-symmetric", [[[1, 2, 1], [2, 3, 2], [1, 2, 1]]]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 새 격자를 만들어 옮긴다.
//
// 제자리 회전은 정사각형에서만 되고, 여기서는 결과의 크기가 입력과 다르다. 크기를
// 먼저 정하고 옮기면 직사각형도 같은 코드로 끝난다.
fun rotate(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val out = Array(cols) { IntArray(rows) }

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            out[c][rows - 1 - r] = grid[r][c]
            Drill.write(c * rows + (rows - 1 - r), grid[r][c])
        }
    }
    return out
}
""",
    mutants=[
        ("counter-clockwise--wrong-direction", "WRONG_BRANCH",
         "반시계 방향으로 돌린다.",
         """
fun rotate(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val out = Array(cols) { IntArray(rows) }
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            out[cols - 1 - c][r] = grid[r][c]
        }
    }
    return out
}
"""),
        ("transpose-only--forgets-reverse", "MISSING_EDGE_CASE",
         "행과 열만 바꾸고 뒤집지 않는다.",
         """
fun rotate(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val out = Array(cols) { IntArray(rows) }
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            out[c][r] = grid[r][c]
        }
    }
    return out
}
"""),
        ("assumes-square--breaks-on-rectangle", "MISSING_EDGE_CASE",
         "결과를 입력과 같은 크기로 잡는다. 정사각형에서는 맞고 직사각형에서 터진다.",
         """
fun rotate(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val out = Array(rows) { IntArray(cols) }
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            out[c][rows - 1 - r] = grid[r][c]
        }
    }
    return out
}
"""),
    ],
))


# --- 38. 나선 채우기 ---------------------------------------------------------

def _spiral_fill(n):
    grid = [[0] * n for _ in range(n)]
    r = c = 0
    dr, dc = 0, 1
    for value in range(1, n * n + 1):
        grid[r][c] = value
        nr, nc = r + dr, c + dc
        if not (0 <= nr < n and 0 <= nc < n and grid[nr][nc] == 0):
            dr, dc = dc, -dr
            nr, nc = r + dr, c + dc
        r, c = nr, nc
    return grid


PROBLEMS.append(Problem(
    id="spiral-fill",
    title="나선으로 채우기",
    summary="""
`n × n` 격자를 만들고 `1` 부터 `n * n` 까지를 **시계 방향 나선**으로 채워 반환한다.

왼쪽 위 칸이 `1` 이고 오른쪽으로 나아간다.
""",
    notes="""
`n` 이 `0` 이면 빈 격자를 반환한다.
""",
    drill_doc="""
Drill.write(index, value)  // 격자의 index 번째 칸에 value 를 넣었다 (행 우선)
""",
    constraints="""
- `0 <= n <= 60`
""",
    signature=dict(name="spiralFill", parameters=[("n", "INT")], returns="INT_MATRIX"),
    groups=standard_groups(),
    reference=_spiral_fill,
    cases={
        "sample": [
            ("01", [3]),
            ("02", [1]),
        ],
        "boundary": [
            ("01-zero", [0]),
            ("02-one", [1]),
            # 안쪽에 한 칸만 남는다. 마지막 한 칸을 두 번 쓰거나 빠뜨리기 쉽다.
            ("03-two", [2]),
            ("04-three", [3]),
            ("05-four", [4]),
            ("06-five", [5]),
        ],
        "hidden": [
            ("01-six", [6]),
            ("02-seven", [7]),
            ("03-ten", [10]),
            ("04-eleven", [11]),
            ("05-sixty", [60]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 경계를 좁혀 가며 값을 채운다.
//
// 홀수 크기의 한가운데 한 칸이 함정이다. 네 방향을 무조건 다 돌면 그 칸을 두 번 쓴다.
// `top <= bottom` 과 `left <= right` 로 남은 줄이 있을 때만 돈다.
fun spiralFill(n: Int): Array<IntArray> {
    val out = Array(n) { IntArray(n) }
    var top = 0
    var bottom = n - 1
    var left = 0
    var right = n - 1
    var value = 1

    while (value <= n * n) {
        for (c in left..right) {
            out[top][c] = value
            Drill.write(top * n + c, value)
            value += 1
        }
        top += 1

        for (r in top..bottom) {
            out[r][right] = value
            Drill.write(r * n + right, value)
            value += 1
        }
        right -= 1

        if (top <= bottom) {
            for (c in right downTo left) {
                out[bottom][c] = value
                Drill.write(bottom * n + c, value)
                value += 1
            }
            bottom -= 1
        }

        if (left <= right) {
            for (r in bottom downTo top) {
                out[r][left] = value
                Drill.write(r * n + left, value)
                value += 1
            }
            left += 1
        }
    }
    return out
}
""",
    mutants=[
        ("row-major--fills-in-order", "WRONG_ALGORITHM",
         "나선이 아니라 행 순서대로 채운다.",
         """
fun spiralFill(n: Int): Array<IntArray> {
    val out = Array(n) { IntArray(n) }
    var value = 1
    for (r in 0 until n) {
        for (c in 0 until n) {
            out[r][c] = value
            value += 1
        }
    }
    return out
}
"""),
        ("counter-clockwise--starts-downward", "WRONG_BRANCH",
         "아래로 먼저 내려가는 반시계 나선을 만든다.",
         """
fun spiralFill(n: Int): Array<IntArray> {
    val out = Array(n) { IntArray(n) }
    var top = 0
    var bottom = n - 1
    var left = 0
    var right = n - 1
    var value = 1
    while (value <= n * n) {
        for (r in top..bottom) { out[r][left] = value; value += 1 }
        left += 1
        for (c in left..right) { out[bottom][c] = value; value += 1 }
        bottom -= 1
        if (left <= right) {
            for (r in bottom downTo top) { out[r][right] = value; value += 1 }
            right -= 1
        }
        if (top <= bottom) {
            for (c in right downTo left) { out[top][c] = value; value += 1 }
            top += 1
        }
    }
    return out
}
"""),
        ("starts-at-zero--off-by-one", "OFF_BY_ONE",
         "`0` 부터 채워 마지막 값이 하나씩 모자란다.",
         """
fun spiralFill(n: Int): Array<IntArray> {
    val out = Array(n) { IntArray(n) }
    var top = 0
    var bottom = n - 1
    var left = 0
    var right = n - 1
    var value = 0
    while (value < n * n) {
        for (c in left..right) { out[top][c] = value; value += 1 }
        top += 1
        for (r in top..bottom) { out[r][right] = value; value += 1 }
        right -= 1
        if (top <= bottom) {
            for (c in right downTo left) { out[bottom][c] = value; value += 1 }
            bottom -= 1
        }
        if (left <= right) {
            for (r in bottom downTo top) { out[r][left] = value; value += 1 }
            left += 1
        }
    }
    return out
}
"""),
    ],
))


# --- 55. 썩는 오렌지 (다중 출발 BFS) -------------------------------------------------

def _rotting_minutes(grid):
    from collections import deque
    rows = len(grid)
    cols = len(grid[0]) if rows else 0
    queue = deque()
    fresh = 0
    for r in range(rows):
        for c in range(cols):
            if grid[r][c] == 2:
                queue.append((r, c, 0))
            elif grid[r][c] == 1:
                fresh += 1
    state = [row[:] for row in grid]
    minutes = 0
    while queue:
        r, c, t = queue.popleft()
        minutes = t
        for dr, dc in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nr, nc = r + dr, c + dc
            if 0 <= nr < rows and 0 <= nc < cols and state[nr][nc] == 1:
                state[nr][nc] = 2
                fresh -= 1
                queue.append((nr, nc, t + 1))
    return minutes if fresh == 0 else -1


PROBLEMS.append(Problem(
    id="rotting-oranges",
    title="오렌지가 전부 썩는 데 걸리는 시간",
    summary="""
격자 `grid` 의 각 칸은 `0`(빈 칸), `1`(싱싱한 오렌지), `2`(썩은 오렌지) 중 하나다.
**1 분마다** 썩은 오렌지와 상하좌우로 맞닿은 싱싱한 오렌지가 썩는다.

싱싱한 오렌지가 하나도 남지 않을 때까지 걸리는 **최소 분**을 반환한다. 영영 썩지 않는
오렌지가 있으면 `-1` 이다. 처음부터 싱싱한 오렌지가 없으면 `0` 이다.
""",
    notes="""
썩은 오렌지가 여러 개면 **전부 동시에** 퍼진다. 하나에서 시작해 끝까지 퍼뜨린 뒤 다음
것을 시작하면 시간이 틀린다. 처음부터 전부 큐에 넣고 층 단위로 퍼뜨린다.
""",
    drill_doc="""
Drill.enqueue(r * cols + c)   // 썩은 칸을 큐에 넣었다
Drill.dequeue(r * cols + c)   // 큐에서 꺼내 이웃을 본다
Drill.write(r * cols + c, t)  // 칸이 t 분에 썩었다
""",
    constraints="""
- `0 <= rows, cols <= 300`
- 모든 행의 길이는 같다
- 각 칸은 `0`, `1`, `2` 중 하나
""",
    signature=dict(name="rottingMinutes", parameters=[("grid", "INT_MATRIX")], returns="INT"),
    groups=standard_groups(),
    reference=_rotting_minutes,
    cases={
        "sample": [
            ("01", [[[2, 1, 1], [1, 1, 0], [0, 1, 1]]]),
            ("02", [[[2, 1, 1], [0, 1, 1], [1, 0, 1]]]),
        ],
        "boundary": [
            ("01-empty", [[]]),
            ("02-no-columns", [[[], []]]),
            # 싱싱한 것이 없다. 0 이다 — 썩은 것도 없어도 0.
            ("03-no-fresh", [[[0, 2], [2, 0]]]),
            ("04-all-empty", [[[0, 0], [0, 0]]]),
            # 썩은 것이 없는데 싱싱한 것은 있다.
            ("05-no-rotten", [[[1, 1], [1, 1]]]),
            # 썩은 것 둘이 양끝에서 동시에 퍼진다. 하나씩 퍼뜨리면 시간이 배가 된다.
            ("06-two-sources", [[[2, 1, 1, 1, 1, 1, 2]]]),
            # 대각선은 닿은 것이 아니다.
            ("07-diagonal-isolated", [[[2, 0], [0, 1]]]),
            ("08-single-fresh-adjacent", [[[2, 1]]]),
            ("09-single-rotten", [[[2]]]),
        ],
        "hidden": [
            ("01-snake", [[[2, 1, 1, 1], [0, 0, 0, 1], [1, 1, 1, 1], [1, 0, 0, 0]]]),
            ("02-ring-with-hole", [[[1, 1, 1], [1, 0, 1], [1, 1, 2]]]),
            ("03-unreachable-corner", [[[2, 1, 0, 1], [1, 1, 0, 1], [0, 0, 0, 1]]]),
            ("04-tall", [_rect(8, 2, [2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2])]),
            ("05-big-random", [_rect(60, 60, [v if v < 2 else (2 if v == 2 else 1)
                                             for v in randoms(3600, 0, 9, salt=1301)])]),
            ("06-big-sparse-rotten", [_rect(100, 100, [2 if v == 0 else 1
                                                     for v in randoms(10000, 0, 400, salt=1302)])]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 썩은 칸 전부에서 동시에 시작하는 BFS.
fun rottingMinutes(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val state = Array(rows) { grid[it].copyOf() }
    val queue = ArrayDeque<Int>()
    var fresh = 0
    for (r in 0 until rows) for (c in 0 until cols) {
        if (state[r][c] == 2) { queue.addLast(r * cols + c); Drill.enqueue(r * cols + c) }
        else if (state[r][c] == 1) fresh += 1
    }
    var minutes = 0
    val dr = intArrayOf(1, -1, 0, 0)
    val dc = intArrayOf(0, 0, 1, -1)
    while (queue.isNotEmpty() && fresh > 0) {
        minutes += 1
        repeat(queue.size) {
            val cell = queue.removeFirst()
            Drill.dequeue(cell)
            val r = cell / cols
            val c = cell % cols
            for (k in 0 until 4) {
                val nr = r + dr[k]
                val nc = c + dc[k]
                if (nr !in 0 until rows || nc !in 0 until cols || state[nr][nc] != 1) continue
                state[nr][nc] = 2
                fresh -= 1
                Drill.write(nr * cols + nc, minutes)
                queue.addLast(nr * cols + nc)
            }
        }
    }
    return if (fresh == 0) minutes else -1
}
""",
    mutants=[
        ("one-source-at-a-time", "WRONG_ALGORITHM",
         "썩은 오렌지를 하나씩 끝까지 퍼뜨린다. 여러 곳에서 동시에 퍼지는 것을 잊었다.",
         """
fun rottingMinutes(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val time = Array(rows) { IntArray(cols) { -1 } }
    var worst = 0
    for (r0 in 0 until rows) for (c0 in 0 until cols) {
        if (grid[r0][c0] != 2) continue
        val queue = ArrayDeque<Int>()
        time[r0][c0] = 0
        queue.addLast(r0 * cols + c0)
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            val r = cell / cols; val c = cell % cols
            for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val nr = r + dr; val nc = c + dc
                if (nr !in 0 until rows || nc !in 0 until cols || grid[nr][nc] != 1 || time[nr][nc] != -1) continue
                time[nr][nc] = time[r][c] + 1
                worst = maxOf(worst, time[nr][nc])
                queue.addLast(nr * cols + nc)
            }
        }
    }
    for (r in 0 until rows) for (c in 0 until cols) if (grid[r][c] == 1 && time[r][c] == -1) return -1
    return worst
}
"""),
        ("ignores-leftover-fresh", "MISSING_EDGE_CASE",
         "끝까지 썩지 않은 오렌지가 남아도 걸린 시간을 돌려준다.",
         """
fun rottingMinutes(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val state = Array(rows) { grid[it].copyOf() }
    val queue = ArrayDeque<Int>()
    for (r in 0 until rows) for (c in 0 until cols) if (state[r][c] == 2) queue.addLast(r * cols + c)
    var minutes = 0
    while (queue.isNotEmpty()) {
        var spread = false
        repeat(queue.size) {
            val cell = queue.removeFirst()
            val r = cell / cols; val c = cell % cols
            for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val nr = r + dr; val nc = c + dc
                if (nr !in 0 until rows || nc !in 0 until cols || state[nr][nc] != 1) continue
                state[nr][nc] = 2; spread = true
                queue.addLast(nr * cols + nc)
            }
        }
        if (spread) minutes += 1
    }
    return minutes
}
"""),
        ("counts-rounds--off-by-one", "OFF_BY_ONE",
         "마지막으로 퍼뜨릴 것이 없던 회차까지 센다. 답이 1 크다.",
         """
fun rottingMinutes(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val state = Array(rows) { grid[it].copyOf() }
    val queue = ArrayDeque<Int>()
    var fresh = 0
    for (r in 0 until rows) for (c in 0 until cols) {
        if (state[r][c] == 2) queue.addLast(r * cols + c) else if (state[r][c] == 1) fresh += 1
    }
    if (fresh == 0) return 0
    var minutes = 0
    while (queue.isNotEmpty()) {
        minutes += 1
        repeat(queue.size) {
            val cell = queue.removeFirst()
            val r = cell / cols; val c = cell % cols
            for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val nr = r + dr; val nc = c + dc
                if (nr !in 0 until rows || nc !in 0 until cols || state[nr][nc] != 1) continue
                state[nr][nc] = 2; fresh -= 1
                queue.addLast(nr * cols + nc)
            }
        }
    }
    return if (fresh == 0) minutes else -1
}
"""),
        ("diagonals-spread", "WRONG_BRANCH",
         "대각선으로도 퍼진다고 본다. 상하좌우만이다.",
         """
fun rottingMinutes(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val state = Array(rows) { grid[it].copyOf() }
    val queue = ArrayDeque<Int>()
    var fresh = 0
    for (r in 0 until rows) for (c in 0 until cols) {
        if (state[r][c] == 2) queue.addLast(r * cols + c) else if (state[r][c] == 1) fresh += 1
    }
    var minutes = 0
    while (queue.isNotEmpty() && fresh > 0) {
        minutes += 1
        repeat(queue.size) {
            val cell = queue.removeFirst()
            val r = cell / cols; val c = cell % cols
            for (dr in -1..1) for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = r + dr; val nc = c + dc
                if (nr !in 0 until rows || nc !in 0 until cols || state[nr][nc] != 1) continue
                state[nr][nc] = 2; fresh -= 1
                queue.addLast(nr * cols + nc)
            }
        }
    }
    return if (fresh == 0) minutes else -1
}
"""),
    ],
))


# --- 56. 땅을 하나씩 더할 때마다의 섬 수 (유니온 파인드) ------------------------------------

def _islands_after_each(rows, cols, positions):
    parent = {}

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    count = 0
    out = []
    for i in range(0, len(positions), 2):
        r, c = positions[i], positions[i + 1]
        cell = r * cols + c
        if cell in parent:
            out.append(count)
            continue
        parent[cell] = cell
        count += 1
        for dr, dc in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nr, nc = r + dr, c + dc
            if 0 <= nr < rows and 0 <= nc < cols and (nr * cols + nc) in parent:
                a, b = find(cell), find(nr * cols + nc)
                if a != b:
                    parent[a] = b
                    count -= 1
        out.append(count)
    return out


def _fill_positions(rows, cols, count, salt):
    rs = randoms(count, 0, rows - 1, salt=salt)
    cs = randoms(count, 0, cols - 1, salt=salt + 1)
    return flat([rs[i], cs[i]] for i in range(count))


PROBLEMS.append(Problem(
    id="islands-after-each",
    title="땅을 하나씩 더할 때마다의 섬 수",
    summary="""
`rows × cols` 크기의 바다가 있다. `positions` 는 `[r1, c1, r2, c2, ...]` 로 평탄하게 이은
칸의 목록이며, 차례로 그 칸을 **땅으로 바꾼다.** 이미 땅인 칸이 또 나올 수 있다 — 그때는
아무것도 바뀌지 않는다.

땅을 하나 더할 때마다 그 시점의 **섬의 개수**를 담은 배열을 반환한다. 섬은 상하좌우로
이어진 땅의 덩어리다.
""",
    notes="""
매번 섬을 다시 세면 더하는 횟수만큼 격자 전체를 훑는다. 땅을 더하는 순간 바뀌는 것은
"새 섬 하나가 생기고, 맞닿은 서로 다른 섬들이 하나로 합쳐진다"뿐이다 — 어느 섬에
속하는지를 빠르게 합치고 찾는 구조가 필요하다.
""",
    drill_doc="""
Drill.write(r * cols + c, count)   // 칸을 땅으로 바꾼 뒤의 섬 수
Drill.match(a, b)                  // 두 섬을 합쳤다 (대표 칸 번호)
""",
    constraints="""
- `1 <= rows, cols <= 300`
- `positions.size` 는 짝수이며 `2 <= positions.size <= 40_000`
- `0 <= r < rows`, `0 <= c < cols`
""",
    signature=dict(
        name="islandsAfterEach",
        parameters=[("rows", "INT"), ("cols", "INT"), ("positions", "INT_ARRAY")],
        returns="INT_ARRAY",
    ),
    groups=perf_groups(),
    reference=_islands_after_each,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 2000000},
    cases={
        "sample": [
            ("01", [3, 3, [0, 0, 0, 1, 1, 2, 2, 1]]),
            ("02", [1, 1, [0, 0]]),
        ],
        "boundary": [
            # 같은 칸을 두 번. 섬 수가 그대로다.
            ("01-duplicate", [2, 2, [0, 0, 0, 0, 1, 1]]),
            # 세 섬이 마지막 한 칸으로 하나가 된다. 합칠 때 하나만 빼면 틀린다.
            ("02-merge-three", [3, 3, [0, 1, 1, 0, 1, 2, 1, 1]]),
            # 네 섬이 한 칸으로.
            ("03-merge-four", [3, 3, [0, 1, 1, 0, 1, 2, 2, 1, 1, 1]]),
            # 대각선은 이어진 것이 아니다.
            ("04-diagonal", [2, 2, [0, 0, 1, 1]]),
            # 한 줄 격자. 행 끝과 다음 행 처음은 이웃이 아니다.
            ("05-row-wrap", [2, 3, [0, 2, 1, 0]]),
            ("06-single-column", [3, 1, [0, 0, 2, 0, 1, 0]]),
        ],
        "hidden": [
            ("01-fill-row", [1, 6, [0, 0, 0, 2, 0, 4, 0, 1, 0, 3, 0, 5]]),
            ("02-spiral", [3, 3, [0, 0, 0, 1, 0, 2, 1, 2, 2, 2, 2, 1, 2, 0, 1, 0, 1, 1]]),
            ("03-random-small", [5, 5, _fill_positions(5, 5, 30, salt=1401)]),
            ("04-random-medium", [20, 20, _fill_positions(20, 20, 300, salt=1403)]),
        ],
        "performance": [
            ("01-small", [300, 300, _fill_positions(300, 300, 3000, salt=1405)]),
            ("02-medium", [300, 300, _fill_positions(300, 300, 10000, salt=1407)]),
            ("03-large", [300, 300, _fill_positions(300, 300, 20000, salt=1409)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 경로 압축 유니온 파인드.
fun islandsAfterEach(rows: Int, cols: Int, positions: IntArray): IntArray {
    val parent = IntArray(rows * cols) { -1 }
    fun find(x: Int): Int {
        var v = x
        while (parent[v] != v) { parent[v] = parent[parent[v]]; v = parent[v] }
        return v
    }
    val out = IntArray(positions.size / 2)
    var count = 0
    val dr = intArrayOf(1, -1, 0, 0)
    val dc = intArrayOf(0, 0, 1, -1)
    for (i in out.indices) {
        val r = positions[2 * i]
        val c = positions[2 * i + 1]
        val cell = r * cols + c
        if (parent[cell] == -1) {
            parent[cell] = cell
            count += 1
            for (k in 0 until 4) {
                val nr = r + dr[k]
                val nc = c + dc[k]
                if (nr !in 0 until rows || nc !in 0 until cols) continue
                val other = nr * cols + nc
                if (parent[other] == -1) continue
                val a = find(cell)
                val b = find(other)
                if (a != b) { parent[a] = b; count -= 1; Drill.match(a, b) }
            }
        }
        out[i] = count
        Drill.write(cell, count)
    }
    return out
}
""",
    mutants=[
        ("duplicate-counts-again", "MISSING_EDGE_CASE",
         "이미 땅인 칸을 새 섬으로 또 센다.",
         """
fun islandsAfterEach(rows: Int, cols: Int, positions: IntArray): IntArray {
    val parent = IntArray(rows * cols) { -1 }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    val out = IntArray(positions.size / 2)
    var count = 0
    for (i in out.indices) {
        val r = positions[2 * i]; val c = positions[2 * i + 1]
        val cell = r * cols + c
        parent[cell] = cell
        count += 1
        for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
            val nr = r + dr; val nc = c + dc
            if (nr !in 0 until rows || nc !in 0 until cols) continue
            val other = nr * cols + nc
            if (parent[other] == -1) continue
            val a = find(cell); val b = find(other)
            if (a != b) { parent[a] = b; count -= 1 }
        }
        out[i] = count
    }
    return out
}
"""),
        ("merges-once--stops-after-first", "WRONG_BRANCH",
         "이웃 섬을 하나만 합치고 멈춘다. 새 칸이 셋 이상을 잇는 자리에서 틀린다.",
         """
fun islandsAfterEach(rows: Int, cols: Int, positions: IntArray): IntArray {
    val parent = IntArray(rows * cols) { -1 }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    val out = IntArray(positions.size / 2)
    var count = 0
    for (i in out.indices) {
        val r = positions[2 * i]; val c = positions[2 * i + 1]
        val cell = r * cols + c
        if (parent[cell] == -1) {
            parent[cell] = cell
            count += 1
            for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val nr = r + dr; val nc = c + dc
                if (nr !in 0 until rows || nc !in 0 until cols) continue
                val other = nr * cols + nc
                if (parent[other] == -1) continue
                val a = find(cell); val b = find(other)
                if (a != b) { parent[a] = b; count -= 1; break }
            }
        }
        out[i] = count
    }
    return out
}
"""),
        ("row-wrap--neighbors-across-rows", "OFF_BY_ONE",
         "행의 끝과 다음 행의 처음을 이웃으로 본다. 칸 번호 ±1 만 보고 열 경계를 잊었다.",
         """
fun islandsAfterEach(rows: Int, cols: Int, positions: IntArray): IntArray {
    val total = rows * cols
    val parent = IntArray(total) { -1 }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    val out = IntArray(positions.size / 2)
    var count = 0
    for (i in out.indices) {
        val cell = positions[2 * i] * cols + positions[2 * i + 1]
        if (parent[cell] == -1) {
            parent[cell] = cell
            count += 1
            for (other in intArrayOf(cell - 1, cell + 1, cell - cols, cell + cols)) {
                if (other < 0 || other >= total || parent[other] == -1) continue
                val a = find(cell); val b = find(other)
                if (a != b) { parent[a] = b; count -= 1 }
            }
        }
        out[i] = count
    }
    return out
}
"""),
        ("recount-by-bfs--each-time", "PERFORMANCE",
         "땅을 더할 때마다 격자 전체를 BFS 로 다시 센다. O(추가 × 격자).",
         """
fun islandsAfterEach(rows: Int, cols: Int, positions: IntArray): IntArray {
    val land = BooleanArray(rows * cols)
    val out = IntArray(positions.size / 2)
    val seen = BooleanArray(rows * cols)
    for (i in out.indices) {
        land[positions[2 * i] * cols + positions[2 * i + 1]] = true
        java.util.Arrays.fill(seen, false)
        var count = 0
        for (start in 0 until rows * cols) {
            if (!land[start] || seen[start]) continue
            count += 1
            val stack = ArrayDeque<Int>()
            stack.addLast(start); seen[start] = true
            while (stack.isNotEmpty()) {
                val cell = stack.removeLast()
                Drill.visit(cell, count)
                val r = cell / cols; val c = cell % cols
                for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                    val nr = r + dr; val nc = c + dc
                    if (nr !in 0 until rows || nc !in 0 until cols) continue
                    val other = nr * cols + nc
                    if (land[other] && !seen[other]) { seen[other] = true; stack.addLast(other) }
                }
            }
        }
        out[i] = count
    }
    return out
}
"""),
    ],
))


# --- 67. 생명 게임 한 세대 -------------------------------------------------------------

def _life_step(grid):
    rows = len(grid)
    cols = len(grid[0]) if rows else 0
    out = [[0] * cols for _ in range(rows)]
    for r in range(rows):
        for c in range(cols):
            alive = 0
            for dr in (-1, 0, 1):
                for dc in (-1, 0, 1):
                    if dr == 0 and dc == 0:
                        continue
                    nr, nc = r + dr, c + dc
                    if 0 <= nr < rows and 0 <= nc < cols and grid[nr][nc] == 1:
                        alive += 1
            if grid[r][c] == 1:
                out[r][c] = 1 if alive in (2, 3) else 0
            else:
                out[r][c] = 1 if alive == 3 else 0
    return out


PROBLEMS.append(Problem(
    id="game-of-life-step",
    title="생명 게임 한 세대",
    summary="""
`0`(죽음) 과 `1`(삶) 의 격자 `grid` 가 주어진다. **다음 세대**의 격자를 반환한다. 각 칸의
이웃은 상하좌우와 대각선의 **여덟 칸**이고, 격자 밖은 죽은 것으로 본다. 규칙은 넷이다.

1. 살아 있는 칸의 이웃 중 산 것이 2 개 미만이면 죽는다.
2. 살아 있는 칸의 이웃 중 산 것이 2 개 또는 3 개면 산다.
3. 살아 있는 칸의 이웃 중 산 것이 4 개 이상이면 죽는다.
4. 죽은 칸의 이웃 중 산 것이 **정확히** 3 개면 산다.

모든 칸은 **동시에** 바뀐다 — 한 칸을 바꾼 결과가 옆 칸의 이웃 수에 들어가면 안 된다.
""",
    notes="""
규칙 넷을 그대로 옮기면 된다. 두 가지가 문제다. 이웃은 여덟이지 넷이 아니고, 갱신은 동시다 —
제자리에서 바꾸면 위와 왼쪽 이웃은 이미 다음 세대가 되어 있다.
""",
    drill_doc="""
Drill.visit(r * cols + c, alive)  // 칸의 산 이웃 수를 셌다
Drill.write(r * cols + c, next)   // 다음 세대의 값
""",
    constraints="""
- `0 <= rows, cols <= 200`
- 모든 행의 길이는 같다
- 각 칸은 `0` 또는 `1`
""",
    signature=dict(name="lifeStep", parameters=[("grid", "INT_MATRIX")], returns="INT_MATRIX"),
    groups=standard_groups(),
    reference=_life_step,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 2000000},
    cases={
        "sample": [
            ("01", [[[0, 1, 0], [0, 0, 1], [1, 1, 1], [0, 0, 0]]]),
            ("02", [[[1, 1], [1, 0]]]),
        ],
        "boundary": [
            ("01-empty", [[]]),
            ("02-no-columns", [[[], []]]),
            ("03-single-alive", [[[1]]]),
            ("04-all-dead", [[[0, 0], [0, 0]]]),
            # 2×2 블록. 안정하다 — 각 칸의 이웃이 셋.
            ("05-block", [[[1, 1], [1, 1]]]),
            # 세로 막대 셋 → 가로 막대. 대각선을 세지 않으면 다르게 나온다.
            ("06-blinker", [[[0, 0, 0], [1, 1, 1], [0, 0, 0]]]),
            # 제자리에서 바꾸면 틀리는 모양. 위 칸을 먼저 바꾸면 아래 칸의 이웃 수가 달라진다.
            ("07-in-place-trap", [[[1, 1, 0], [1, 0, 0], [0, 0, 0]]]),
            # 죽은 칸의 이웃이 넷. 3 이상으로 살리면 틀린다.
            ("08-four-neighbours-dead", [[[1, 0, 1], [0, 0, 0], [1, 0, 1]]]),
            ("09-single-row", [[[1, 1, 1, 0, 1]]]),
        ],
        "hidden": [
            ("01-glider", [[[0, 1, 0, 0, 0], [0, 0, 1, 0, 0], [1, 1, 1, 0, 0], [0, 0, 0, 0, 0], [0, 0, 0, 0, 0]]]),
            ("02-random-small", [_rect(6, 7, randoms(42, 0, 1, salt=2701))]),
            ("03-random-medium", [_rect(40, 50, randoms(2000, 0, 1, salt=2702))]),
            ("04-large", [_rect(200, 200, randoms(40000, 0, 1, salt=2703))]),
            ("05-all-alive", [_rect(5, 5, [1] * 25)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 새 격자에 쓴다 — 동시 갱신.
fun lifeStep(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val next = Array(rows) { IntArray(cols) }
    for (r in 0 until rows) for (c in 0 until cols) {
        var alive = 0
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols && grid[nr][nc] == 1) alive += 1
        }
        Drill.visit(r * cols + c, alive)
        next[r][c] = if (grid[r][c] == 1) (if (alive == 2 || alive == 3) 1 else 0) else (if (alive == 3) 1 else 0)
        Drill.write(r * cols + c, next[r][c])
    }
    return next
}
""",
    mutants=[
        ("in-place--sequential", "WRONG_ALGORITHM",
         "제자리에서 바꾼다. 먼저 바뀐 칸이 아직 안 바뀐 칸의 이웃 수에 들어간다.",
         """
fun lifeStep(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val out = Array(rows) { grid[it].copyOf() }
    for (r in 0 until rows) for (c in 0 until cols) {
        var alive = 0
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols && out[nr][nc] == 1) alive += 1
        }
        out[r][c] = if (out[r][c] == 1) (if (alive == 2 || alive == 3) 1 else 0) else (if (alive == 3) 1 else 0)
    }
    return out
}
"""),
        ("four-neighbours", "MISSING_EDGE_CASE",
         "상하좌우만 이웃으로 센다. 대각선을 잊었다.",
         """
fun lifeStep(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val next = Array(rows) { IntArray(cols) }
    for (r in 0 until rows) for (c in 0 until cols) {
        var alive = 0
        for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols && grid[nr][nc] == 1) alive += 1
        }
        next[r][c] = if (grid[r][c] == 1) (if (alive == 2 || alive == 3) 1 else 0) else (if (alive == 3) 1 else 0)
    }
    return next
}
"""),
        ("birth-at-least-three", "WRONG_BRANCH",
         "죽은 칸의 이웃이 3 이상이면 살린다. 정확히 3 이어야 한다.",
         """
fun lifeStep(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val next = Array(rows) { IntArray(cols) }
    for (r in 0 until rows) for (c in 0 until cols) {
        var alive = 0
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols && grid[nr][nc] == 1) alive += 1
        }
        next[r][c] = if (grid[r][c] == 1) (if (alive == 2 || alive == 3) 1 else 0) else (if (alive >= 3) 1 else 0)
    }
    return next
}
"""),
        ("counts-self", "OFF_BY_ONE",
         "자기 자신을 이웃에 넣어 센다. 산 칸의 이웃 수가 하나 많다.",
         """
fun lifeStep(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val next = Array(rows) { IntArray(cols) }
    for (r in 0 until rows) for (c in 0 until cols) {
        var alive = 0
        for (dr in -1..1) for (dc in -1..1) {
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols && grid[nr][nc] == 1) alive += 1
        }
        next[r][c] = if (grid[r][c] == 1) (if (alive == 2 || alive == 3) 1 else 0) else (if (alive == 3) 1 else 0)
    }
    return next
}
"""),
    ],
))


# --- 94. 블록 합 (2차원 누적합) ------------------------------------------------------

def _matrix_block_sum(grid, k):
    rows, cols = len(grid), len(grid[0])
    pre = [[0] * (cols + 1) for _ in range(rows + 1)]
    for r in range(rows):
        acc = 0
        for c in range(cols):
            acc += grid[r][c]
            pre[r + 1][c + 1] = pre[r][c + 1] + acc
    out = [[0] * cols for _ in range(rows)]
    for r in range(rows):
        r1, r2 = max(0, r - k), min(rows, r + k + 1)
        for c in range(cols):
            c1, c2 = max(0, c - k), min(cols, c + k + 1)
            out[r][c] = pre[r2][c2] - pre[r1][c2] - pre[r2][c1] + pre[r1][c1]
    return out


def _grid_of(rows, cols, lo, hi, salt):
    values = randoms(rows * cols, lo, hi, salt=salt)
    return [values[r * cols:(r + 1) * cols] for r in range(rows)]


PROBLEMS.append(Problem(
    id="matrix-block-sum",
    title="칸마다 주변 블록의 합",
    summary="""
정수 격자 `grid` 와 `k` 가 주어진다. 각 칸 `(r, c)` 에 대해 **행과 열이 각각 `k` 이내**인
칸들 — `|r'-r| <= k`, `|c'-c| <= k`, 격자 안 — 의 합을 구한 같은 크기의 격자를 반환한다.

예: `[[1,2,3],[4,5,6],[7,8,9]]`, `k = 1` 이면 `[[12,21,16],[27,45,33],[24,39,28]]` 이다.
""",
    notes="""
칸마다 `(2k+1)²` 개를 더하면 격자가 크고 `k` 가 크면 끝나지 않는다. 2차원 누적합 —
`pre[r][c]` 를 `(0,0)` 부터 `(r-1,c-1)` 까지의 합으로 두면 어떤 직사각형의 합도 네 값의
덧셈·뺄셈 하나다. 경계에서 잘리는 블록은 좌표를 격자 안으로 조이면 된다.
""",
    drill_doc="""
Drill.visit(r, c)             // 칸을 봤다
Drill.write(r * cols + c, v)  // 그 칸의 블록 합
""",
    constraints="""
- `1 <= rows, cols <= 500`, `0 <= k <= 500`
- `-100 <= grid[r][c] <= 100`
""",
    signature=dict(name="blockSum", parameters=[("grid", "INT_MATRIX"), ("k", "INT")], returns="INT_MATRIX"),
    groups=perf_groups(time_multiplier=0.5),
    reference=_matrix_block_sum,
    cases={
        "sample": [
            ("01", [[[1, 2, 3], [4, 5, 6], [7, 8, 9]], 1]),
            ("02", [[[1, 2, 3], [4, 5, 6], [7, 8, 9]], 2]),
        ],
        "boundary": [
            ("01-k-zero", [[[5, -3], [2, 7]], 0]),
            ("02-single-cell", [[[9]], 3]),
            # k 가 격자보다 크다. 모든 칸이 전체 합이다.
            ("03-k-covers-all", [[[1, 2], [3, 4]], 10]),
            ("04-single-row", [[[1, 2, 3, 4, 5]], 1]),
            ("05-single-column", [[[1], [2], [3], [4]], 2]),
            ("06-negatives", [[[-1, -2], [-3, -4]], 1]),
        ],
        "hidden": [
            ("01-random-small", [_grid_of(5, 7, -10, 10, salt=5001), 1]),
            ("02-random-medium", [_grid_of(30, 20, -100, 100, salt=5002), 3]),
            ("03-random-big-k", [_grid_of(40, 40, -100, 100, salt=5003), 25]),
            ("04-random-k-zero", [_grid_of(10, 10, -100, 100, salt=5004), 0]),
        ],
        "performance": [
            ("01-small", [_grid_of(200, 200, -100, 100, salt=5005), 50]),
            ("02-medium", [_grid_of(400, 400, -100, 100, salt=5006), 150]),
            ("03-large", [_grid_of(500, 500, -100, 100, salt=5007), 250]),
        ],
    },
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 4000000},
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 2차원 누적합. 직사각형 하나가 네 값이다.
fun blockSum(grid: Array<IntArray>, k: Int): Array<IntArray> {
    val rows = grid.size
    val cols = grid[0].size
    val pre = Array(rows + 1) { IntArray(cols + 1) }
    for (r in 0 until rows) {
        var acc = 0
        for (c in 0 until cols) {
            acc += grid[r][c]
            pre[r + 1][c + 1] = pre[r][c + 1] + acc
        }
    }
    return Array(rows) { r ->
        val r1 = maxOf(0, r - k); val r2 = minOf(rows, r + k + 1)
        IntArray(cols) { c ->
            val c1 = maxOf(0, c - k); val c2 = minOf(cols, c + k + 1)
            Drill.visit(r, c)
            val v = pre[r2][c2] - pre[r1][c2] - pre[r2][c1] + pre[r1][c1]
            Drill.write(r * cols + c, v)
            v
        }
    }
}
""",
    mutants=[
        ("adds-corner-twice", "WRONG_BRANCH",
         "포함·배제에서 왼쪽 위 모서리를 빼지 않고 더한다.",
         """
fun blockSum(grid: Array<IntArray>, k: Int): Array<IntArray> {
    val rows = grid.size; val cols = grid[0].size
    val pre = Array(rows + 1) { IntArray(cols + 1) }
    for (r in 0 until rows) { var acc = 0; for (c in 0 until cols) { acc += grid[r][c]; pre[r + 1][c + 1] = pre[r][c + 1] + acc } }
    return Array(rows) { r ->
        val r1 = maxOf(0, r - k); val r2 = minOf(rows, r + k + 1)
        IntArray(cols) { c ->
            val c1 = maxOf(0, c - k); val c2 = minOf(cols, c + k + 1)
            pre[r2][c2] - pre[r1][c2] - pre[r2][c1] - pre[r1][c1]
        }
    }
}
"""),
        ("exclusive-upper-bound", "OFF_BY_ONE",
         "블록의 아래·오른쪽 경계를 한 칸 덜 잡는다. r+k 행이 빠진다.",
         """
fun blockSum(grid: Array<IntArray>, k: Int): Array<IntArray> {
    val rows = grid.size; val cols = grid[0].size
    val pre = Array(rows + 1) { IntArray(cols + 1) }
    for (r in 0 until rows) { var acc = 0; for (c in 0 until cols) { acc += grid[r][c]; pre[r + 1][c + 1] = pre[r][c + 1] + acc } }
    return Array(rows) { r ->
        val r1 = maxOf(0, r - k); val r2 = minOf(rows, r + k)
        IntArray(cols) { c ->
            val c1 = maxOf(0, c - k); val c2 = minOf(cols, c + k)
            pre[r2][c2] - pre[r1][c2] - pre[r2][c1] + pre[r1][c1]
        }
    }
}
"""),
        ("brute-force--per-cell", "PERFORMANCE",
         "칸마다 블록을 전부 더한다. O(rows·cols·k²).",
         """
fun blockSum(grid: Array<IntArray>, k: Int): Array<IntArray> {
    val rows = grid.size; val cols = grid[0].size
    return Array(rows) { r ->
        IntArray(cols) { c ->
            var total = 0
            for (rr in maxOf(0, r - k) until minOf(rows, r + k + 1))
                for (cc in maxOf(0, c - k) until minOf(cols, c + k + 1)) { Drill.visit(rr, cc); total += grid[rr][cc] }
            total
        }
    }
}
"""),
    ],
))


# --- 97. 격자에서 단어 찾기 (백트래킹) ----------------------------------------------------

def _word_search(board, word):
    rows, cols = len(board), len(board[0])
    seen = [[False] * cols for _ in range(rows)]

    def walk(r, c, i):
        if board[r][c] != word[i]:
            return False
        if i == len(word) - 1:
            return True
        seen[r][c] = True
        for dr, dc in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nr, nc = r + dr, c + dc
            if 0 <= nr < rows and 0 <= nc < cols and not seen[nr][nc] and walk(nr, nc, i + 1):
                seen[r][c] = False
                return True
        seen[r][c] = False
        return False

    return 1 if any(walk(r, c, 0) for r in range(rows) for c in range(cols)) else 0


def _letter_grid(rows, cols, alphabet, salt):
    picks = randoms(rows * cols, 0, len(alphabet) - 1, salt=salt)
    return ["".join(alphabet[picks[r * cols + c]] for c in range(cols)) for r in range(rows)]


PROBLEMS.append(Problem(
    id="word-search",
    title="격자에서 단어 찾기",
    summary="""
글자 격자 `board` (행 하나가 문자열 하나, 길이는 전부 같다) 와 단어 `word` 가 주어진다.
격자에서 **상하좌우로 이어진 칸**을 따라 `word` 를 만들 수 있으면 `1`, 없으면 `0` 이다.
같은 칸을 두 번 쓸 수 없다.

예: `["ABCE", "SFCS", "ADEE"]` 에서 `ABCCED` 는 있고, `ABCB` 는 없다 (B 를 두 번 쓴다).
""",
    notes="""
첫 글자가 맞는 칸마다 시작해 다음 글자로 이웃을 뻗는다. 뻗기 전에 칸을 쓴 것으로 표시하고,
그 갈래가 실패하면 **표시를 되돌린다** — 되돌리지 않으면 다른 갈래가 그 칸을 못 쓴다.
그것이 백트래킹이다. 글자가 어긋나는 순간 그 갈래는 끝이다.
""",
    drill_doc="""
Drill.visit(r, c)             // 칸을 밟았다
Drill.match(r, c)             // 마지막 글자까지 맞았다
""",
    constraints="""
- `1 <= rows, cols <= 15`, `1 <= word.length <= 15`
- 격자와 단어는 대문자 영문자
""",
    signature=dict(name="wordSearch", parameters=[("board", "STRING_ARRAY"), ("word", "STRING")], returns="INT"),
    # 글자를 늦게 보는 오답이 개발 머신에서 한도의 2.9~3.0배로 겨우 넘겼다. 한도를 조여 자릿수로 지게 한다.
    groups=perf_groups(time_multiplier=0.25),
    # 공개 뒤 한도를 조였다 — 판정이 바뀌므로 새 버전이다 (§6.1 공개 후 불변).
    version=2,
    reference=_word_search,
    cases={
        "sample": [
            ("01", [["ABCE", "SFCS", "ADEE"], "ABCCED"]),
            ("02", [["ABCE", "SFCS", "ADEE"], "ABCB"]),
        ],
        "boundary": [
            ("01-single-cell-hit", [["A"], "A"]),
            ("02-single-cell-miss", [["A"], "B"]),
            # 단어가 격자의 칸 수보다 길다.
            ("03-word-too-long", [["AB"], "ABA"]),
            # 대각선은 이웃이 아니다.
            ("04-diagonal-not-adjacent", [["AB", "CD"], "AD"]),
            # 같은 칸을 두 번 써야만 만들어진다 — 없는 것이다.
            ("05-needs-reuse", [["AB"], "ABA"]),
            # 첫 갈래가 막히고 다른 시작점에서 된다. 표시를 되돌려야 한다.
            ("06-backtrack-needed", [["AAB", "AAA", "BBA"], "AAAAB"]),
            ("07-snake", [["ABCE", "SFES", "ADEE"], "ABCESEEEFS"]),
        ],
        "hidden": [
            ("01-random-present", [_letter_grid(6, 6, "AB", salt=6101), "ABAB"]),
            ("02-random-absent", [_letter_grid(6, 6, "ABC", salt=6102), "ABCABCABCABCABC"]),
            ("03-spiral-path", [["ABCD", "LMNE", "KPOF", "JIHG"], "ABCDEFGHIJKLMNOP"]),
            ("04-spiral-wrong-tail", [["ABCD", "LMNE", "KPOF", "JIHG"], "ABCDEFGHIJKLMNOQ"]),
        ],
        "performance": [
            # 글자가 여덟 가지인 무작위 격자에 없는 단어. 글자가 어긋나는 순간 끊으면 갈래가
            # 거의 없고, 끝에 가서 보면 칸마다 3^14 갈래다.
            ("01-small", [_letter_grid(10, 10, "ABCDEFGH", salt=6103), "ABCDEFGHABCD"]),
            ("02-medium", [_letter_grid(13, 13, "ABCDEFGH", salt=6104), "ABCDEFGHABCDEF"]),
            ("03-large", [_letter_grid(15, 15, "ABCDEFGH", salt=6105), "ABCDEFGHABCDEFG"]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 이웃으로 뻗고, 실패하면 표시를 되돌린다.
fun wordSearch(board: Array<String>, word: String): Int {
    val rows = board.size
    val cols = board[0].length
    val seen = Array(rows) { BooleanArray(cols) }
    fun walk(r: Int, c: Int, i: Int): Boolean {
        if (board[r][c] != word[i]) return false
        Drill.visit(r, c)
        if (i == word.length - 1) { Drill.match(r, c); return true }
        seen[r][c] = true
        val dr = intArrayOf(1, -1, 0, 0)
        val dc = intArrayOf(0, 0, 1, -1)
        for (d in 0 until 4) {
            val nr = r + dr[d]; val nc = c + dc[d]
            if (nr in 0 until rows && nc in 0 until cols && !seen[nr][nc] && walk(nr, nc, i + 1)) { seen[r][c] = false; return true }
        }
        seen[r][c] = false
        return false
    }
    if (word.length > rows * cols) return 0
    for (r in 0 until rows) for (c in 0 until cols) if (walk(r, c, 0)) return 1
    return 0
}
""",
    mutants=[
        ("never-unmarks", "WRONG_BRANCH",
         "실패한 갈래의 표시를 되돌리지 않는다. 다른 갈래가 그 칸을 못 쓴다.",
         """
fun wordSearch(board: Array<String>, word: String): Int {
    val rows = board.size; val cols = board[0].length
    val seen = Array(rows) { BooleanArray(cols) }
    fun walk(r: Int, c: Int, i: Int): Boolean {
        if (board[r][c] != word[i]) return false
        if (i == word.length - 1) return true
        seen[r][c] = true
        val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
        for (d in 0 until 4) {
            val nr = r + dr[d]; val nc = c + dc[d]
            if (nr in 0 until rows && nc in 0 until cols && !seen[nr][nc] && walk(nr, nc, i + 1)) return true
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
fun wordSearch(board: Array<String>, word: String): Int {
    val rows = board.size; val cols = board[0].length
    fun walk(r: Int, c: Int, i: Int): Boolean {
        if (board[r][c] != word[i]) return false
        if (i == word.length - 1) return true
        val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
        for (d in 0 until 4) {
            val nr = r + dr[d]; val nc = c + dc[d]
            if (nr in 0 until rows && nc in 0 until cols && walk(nr, nc, i + 1)) return true
        }
        return false
    }
    for (r in 0 until rows) for (c in 0 until cols) if (walk(r, c, 0)) return 1
    return 0
}
"""),
        ("diagonals-too", "WRONG_ALGORITHM",
         "대각선 이웃으로도 뻗는다.",
         """
fun wordSearch(board: Array<String>, word: String): Int {
    val rows = board.size; val cols = board[0].length
    val seen = Array(rows) { BooleanArray(cols) }
    fun walk(r: Int, c: Int, i: Int): Boolean {
        if (board[r][c] != word[i]) return false
        if (i == word.length - 1) return true
        seen[r][c] = true
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols && !seen[nr][nc] && walk(nr, nc, i + 1)) { seen[r][c] = false; return true }
        }
        seen[r][c] = false
        return false
    }
    for (r in 0 until rows) for (c in 0 until cols) if (walk(r, c, 0)) return 1
    return 0
}
"""),
        ("checks-letter-late", "PERFORMANCE",
         "글자가 맞는지 이웃을 다 뻗은 뒤에 본다. 어긋난 갈래를 끝까지 따라간다.",
         """
fun wordSearch(board: Array<String>, word: String): Int {
    val rows = board.size; val cols = board[0].length
    val seen = Array(rows) { BooleanArray(cols) }
    fun walk(r: Int, c: Int, i: Int): Boolean {
        Drill.visit(r, c)
        if (i == word.length - 1) return board[r][c] == word[i]
        seen[r][c] = true
        val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
        var found = false
        for (d in 0 until 4) {
            val nr = r + dr[d]; val nc = c + dc[d]
            if (nr in 0 until rows && nc in 0 until cols && !seen[nr][nc] && walk(nr, nc, i + 1)) { found = true; break }
        }
        seen[r][c] = false
        return found && board[r][c] == word[i]
    }
    for (r in 0 until rows) for (c in 0 until cols) if (walk(r, c, 0)) return 1
    return 0
}
"""),
    ],
))


# --- 120. 최소 벽 부수기 (0-1 BFS) ---------------------------------------------------------------

def _min_walls(grid):
    from collections import deque
    rows, cols = len(grid), len(grid[0])
    INF = 1 << 30
    cost = [[INF] * cols for _ in range(rows)]
    cost[0][0] = grid[0][0]
    queue = deque([(0, 0)])
    while queue:
        r, c = queue.popleft()
        here = cost[r][c]
        for nr, nc in ((r + 1, c), (r - 1, c), (r, c + 1), (r, c - 1)):
            if 0 <= nr < rows and 0 <= nc < cols:
                nd = here + grid[nr][nc]
                if nd < cost[nr][nc]:
                    cost[nr][nc] = nd
                    if grid[nr][nc] == 0:
                        queue.appendleft((nr, nc))
                    else:
                        queue.append((nr, nc))
    return cost[rows - 1][cols - 1]


def _wall_grid(rows, cols, wall_percent, salt):
    values = randoms(rows * cols, 0, 99, salt=salt)
    return [[1 if values[r * cols + c] < wall_percent else 0 for c in range(cols)] for r in range(rows)]


PROBLEMS.append(Problem(
    id="wall-breaking-path",
    title="최소 벽 부수기",
    summary="""
`0` 은 빈 칸, `1` 은 벽인 격자가 주어진다. 왼쪽 위 `(0, 0)` 에서 상하좌우로 움직여
오른쪽 아래 `(rows-1, cols-1)` 까지 간다. 벽인 칸에는 **벽을 부수고** 들어갈 수 있다.

가는 동안 부수는 벽의 **최소 개수**를 반환한다. 출발 칸과 도착 칸이 벽이면 그것도 센다.
격자는 비어 있지 않다.
""",
    notes="""
걸음 수가 아니라 부순 벽의 수를 줄여야 한다. 빈 칸으로 가는 것은 비용 0, 벽으로 가는 것은
비용 1 이다. 비용이 0 과 1 뿐인 그래프에서는 힙 없이도 된다 — 비용 0 으로 간 칸을 큐의
**앞**에, 비용 1 로 간 칸을 **뒤**에 넣으면 큐가 언제나 비용순으로 정렬돼 있다.
""",
    drill_doc="""
Drill.visit(r * cols + c, grid[r][c])   // 칸을 봤다
Drill.write(r * cols + c, walls)        // 칸까지의 최소 벽 수를 적었다
""",
    constraints="""
- `1 <= rows, cols <= 600`
- `grid[r][c]` 는 `0` 또는 `1`
""",
    signature=dict(
        name="minWallsToBreak",
        parameters=[("grid", "INT_MATRIX")],
        returns="INT",
    ),
    groups=perf_groups(),
    reference=_min_walls,
    cases={
        "sample": [
            ("01", [[[0, 1, 1], [0, 1, 1], [0, 0, 0]]]),
            ("02", [[[0, 1, 0], [1, 1, 0], [0, 0, 0]]]),
        ],
        "boundary": [
            ("01-single-open", [[[0]]]),
            ("02-single-wall", [[[1]]]),
            # 걸음이 가장 적은 길은 벽을 둘 부수고, 돌아가는 길은 하나도 안 부순다.
            ("03-shortest-steps-is-not-fewest-walls", [[[0, 1, 1, 0], [0, 0, 0, 0], [1, 1, 1, 0]]]),
            # 출발 칸이 벽이다.
            ("04-start-is-wall", [[[1, 0], [0, 0]]]),
            ("05-all-walls", [[[1, 1], [1, 1]]]),
            ("06-single-row", [[[0, 1, 0, 1, 0]]]),
            ("07-single-column", [[[0], [1], [1], [0]]]),
            # 벽을 뚫고 가는 것이 돌아가는 것보다 싸다.
            ("08-break-beats-detour", [[[0, 1, 0], [1, 1, 0], [1, 1, 0], [1, 1, 0], [0, 0, 0]]]),
        ],
        "hidden": [
            ("01-random-small", [_wall_grid(6, 7, 40, salt=8201)]),
            ("02-random-dense", [_wall_grid(12, 12, 70, salt=8202)]),
            ("03-random-sparse", [_wall_grid(15, 10, 15, salt=8203)]),
            ("04-corridor", [[[0, 0, 0, 0, 0], [1, 1, 1, 1, 0], [0, 0, 0, 0, 0], [0, 1, 1, 1, 1], [0, 0, 0, 0, 0]]]),
        ],
        "performance": [
            ("01-small", [_wall_grid(60, 60, 30, salt=8211)]),
            ("02-medium", [_wall_grid(250, 250, 30, salt=8212)]),
            ("03-large", [_wall_grid(600, 600, 30, salt=8213)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 0-1 BFS — 비용 0 은 큐 앞에, 비용 1 은 뒤에.
fun minWallsToBreak(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = grid[0].size
    val cost = IntArray(rows * cols) { Int.MAX_VALUE }
    val deque = java.util.ArrayDeque<Int>()
    cost[0] = grid[0][0]
    deque.addFirst(0)
    val dr = intArrayOf(1, -1, 0, 0)
    val dc = intArrayOf(0, 0, 1, -1)
    while (deque.isNotEmpty()) {
        val cell = deque.pollFirst()
        val r = cell / cols
        val c = cell % cols
        Drill.visit(cell, grid[r][c])
        for (k in 0 until 4) {
            val nr = r + dr[k]
            val nc = c + dc[k]
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue
            val nd = cost[cell] + grid[nr][nc]
            val idx = nr * cols + nc
            if (nd < cost[idx]) {
                cost[idx] = nd
                Drill.write(idx, nd)
                if (grid[nr][nc] == 0) deque.addFirst(idx) else deque.addLast(idx)
            }
        }
    }
    return cost[rows * cols - 1]
}
""",
    mutants=[
        ("bfs-by-steps", "WRONG_ALGORITHM",
         "걸음 수가 가장 적은 길을 찾고 그 길의 벽을 센다. 돌아가면 벽을 덜 부수는 경우를 놓친다.",
         """
fun minWallsToBreak(grid: Array<IntArray>): Int {
    val rows = grid.size; val cols = grid[0].size
    val walls = IntArray(rows * cols) { -1 }
    val queue = ArrayDeque<Int>()
    walls[0] = grid[0][0]; queue.addLast(0)
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    while (queue.isNotEmpty()) {
        val cell = queue.removeFirst(); val r = cell / cols; val c = cell % cols
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue
            val idx = nr * cols + nc
            if (walls[idx] == -1) { walls[idx] = walls[cell] + grid[nr][nc]; queue.addLast(idx) }
        }
    }
    return walls[rows * cols - 1]
}
"""),
        ("ignores-start-wall", "OFF_BY_ONE",
         "출발 칸이 벽이어도 세지 않는다.",
         """
fun minWallsToBreak(grid: Array<IntArray>): Int {
    val rows = grid.size; val cols = grid[0].size
    val cost = IntArray(rows * cols) { Int.MAX_VALUE }
    val deque = java.util.ArrayDeque<Int>()
    cost[0] = 0; deque.addFirst(0)
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    while (deque.isNotEmpty()) {
        val cell = deque.pollFirst(); val r = cell / cols; val c = cell % cols
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue
            val nd = cost[cell] + grid[nr][nc]; val idx = nr * cols + nc
            if (nd < cost[idx]) { cost[idx] = nd; if (grid[nr][nc] == 0) deque.addFirst(idx) else deque.addLast(idx) }
        }
    }
    return cost[rows * cols - 1]
}
"""),
        ("plain-queue--zero-cost-not-first", "WRONG_BRANCH",
         "비용 0 으로 간 칸도 큐의 뒤에 넣는다. 큐가 비용순이 아니게 되어 나중에 더 싼 길을 놓친다.",
         """
fun minWallsToBreak(grid: Array<IntArray>): Int {
    val rows = grid.size; val cols = grid[0].size
    val cost = IntArray(rows * cols) { Int.MAX_VALUE }
    val seen = BooleanArray(rows * cols)
    val queue = ArrayDeque<Int>()
    cost[0] = grid[0][0]; queue.addLast(0)
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    while (queue.isNotEmpty()) {
        val cell = queue.removeFirst()
        if (seen[cell]) continue
        seen[cell] = true
        val r = cell / cols; val c = cell % cols
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue
            val nd = cost[cell] + grid[nr][nc]; val idx = nr * cols + nc
            if (nd < cost[idx]) { cost[idx] = nd; queue.addLast(idx) }
        }
    }
    return cost[rows * cols - 1]
}
"""),
        ("dfs-all-paths", "PERFORMANCE",
         "모든 경로를 깊이 우선으로 다 가 보고 가장 적게 부순 것을 고른다. 지수적이다.",
         """
fun minWallsToBreak(grid: Array<IntArray>): Int {
    val rows = grid.size; val cols = grid[0].size
    val seen = BooleanArray(rows * cols)
    var best = Int.MAX_VALUE
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    fun go(r: Int, c: Int, walls: Int) {
        Drill.visit(r * cols + c, grid[r][c])
        val w = walls + grid[r][c]
        if (w >= best) return
        if (r == rows - 1 && c == cols - 1) { best = w; return }
        seen[r * cols + c] = true
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || seen[nr * cols + nc]) continue
            go(nr, nc, w)
        }
        seen[r * cols + c] = false
    }
    go(0, 0, 0)
    return best
}
"""),
    ],
))


# --- 121. 물이 차오르는 격자 (이분 탐색 + BFS) --------------------------------------------------

def _earliest_swim(grid):
    from collections import deque
    n = len(grid)

    def reachable(t):
        if grid[0][0] > t:
            return False
        seen = [[False] * n for _ in range(n)]
        seen[0][0] = True
        queue = deque([(0, 0)])
        while queue:
            r, c = queue.popleft()
            if r == n - 1 and c == n - 1:
                return True
            for nr, nc in ((r + 1, c), (r - 1, c), (r, c + 1), (r, c - 1)):
                if 0 <= nr < n and 0 <= nc < n and not seen[nr][nc] and grid[nr][nc] <= t:
                    seen[nr][nc] = True
                    queue.append((nr, nc))
        return False

    lo, hi = max(grid[0][0], grid[n - 1][n - 1]), n * n - 1
    while lo < hi:
        mid = (lo + hi) // 2
        if reachable(mid):
            hi = mid
        else:
            lo = mid + 1
    return lo


def _elevation_grid(n, salt, locked_corner=False):
    values = shuffled(range(n * n), salt=salt)
    grid = [values[r * n:(r + 1) * n] for r in range(n)]
    if locked_corner:
        # 도착 칸과 그 두 이웃에 가장 높은 셋을 둔다. 답이 n²-1 이 되어, 시각을 하나씩 올리는
        # 풀이는 격자가 거의 다 잠긴 뒤로도 수만 번의 BFS 를 더 돌아야 한다 — 무작위 격자에서는
        # 답이 격자가 이어지는 시각 근처라 그 풀이가 한도를 겨우 넘겼다.
        def place(r, c, value):
            for rr in range(n):
                for cc in range(n):
                    if grid[rr][cc] == value:
                        grid[rr][cc], grid[r][c] = grid[r][c], grid[rr][cc]
                        return
        place(n - 1, n - 1, n * n - 1)
        place(n - 2, n - 1, n * n - 2)
        place(n - 1, n - 2, n * n - 3)
    return grid


PROBLEMS.append(Problem(
    id="rising-water",
    title="물이 차오르는 격자",
    summary="""
`n × n` 격자의 각 칸에 서로 다른 높이 `0` 부터 `n²-1` 이 적혀 있다. 시각 `t` 에 물의 높이는
`t` 이고, 높이가 `t` **이하**인 칸은 물에 잠겨 있다. 잠긴 칸에서 상하좌우로 잠긴 칸으로는
시간 없이 헤엄쳐 갈 수 있다. 잠기지 않은 칸에는 들어갈 수 없다.

왼쪽 위 `(0, 0)` 에서 출발해 오른쪽 아래 `(n-1, n-1)` 에 닿을 수 있는 **가장 이른 시각**을
반환한다. 출발 칸에 들어가는 것부터 시각의 조건을 받는다.
""",
    notes="""
답은 "경로가 지나는 칸들의 최대 높이"를 가장 작게 만드는 경로의 그 최댓값이다 — 합이
아니라 최댓값. 시각 `t` 가 정해지면 닿을 수 있는지는 BFS 한 번으로 알 수 있고, `t` 가 크면
답이고 작으면 아니므로 이분 탐색이 된다. 힙으로 "지금까지의 최댓값이 가장 작은 칸"을 먼저
확정해도 된다.
""",
    drill_doc="""
Drill.visit(r * n + c, grid[r][c])   // 칸에 들어갔다
Drill.compare(t, grid[r][c])         // 물 높이와 칸 높이를 비교했다
""",
    constraints="""
- `1 <= n <= 300`
- `grid[r][c]` 는 `0` 부터 `n²-1` 까지의 서로 다른 정수
""",
    signature=dict(
        name="earliestSwimTime",
        parameters=[("grid", "INT_MATRIX")],
        returns="INT",
    ),
    groups=perf_groups(),
    reference=_earliest_swim,
    cases={
        "sample": [
            ("01", [[[0, 2], [1, 3]]]),
            ("02", [[[0, 1, 2, 3, 4], [24, 23, 22, 21, 5], [12, 13, 14, 15, 16], [11, 17, 18, 19, 20], [10, 9, 8, 7, 6]]]),
        ],
        "boundary": [
            ("01-single", [[[0]]]),
            # 출발 칸이 가장 높다.
            ("02-start-is-highest", [[[3, 0], [1, 2]]]),
            # 도착 칸이 가장 높다.
            ("03-end-is-highest", [[[0, 1], [2, 3]]]),
            # 합이 작은 길과 최댓값이 작은 길이 다르다.
            ("04-sum-vs-max", [[[0, 7, 8], [1, 2, 3], [6, 5, 4]]]),
            # 돌아가는 길이 최댓값이 작다.
            ("05-detour-lower-max", [[[0, 8, 2], [1, 7, 3], [6, 5, 4]]]),
        ],
        "hidden": [
            ("01-random-small", [_elevation_grid(5, salt=8221)]),
            ("02-random-medium", [_elevation_grid(12, salt=8222)]),
            ("03-random-large", [_elevation_grid(30, salt=8223)]),
            ("04-snake", [[[0, 1, 2], [5, 4, 3], [6, 7, 8]]]),
        ],
        "performance": [
            ("01-small", [_elevation_grid(60, salt=8231, locked_corner=True)]),
            ("02-medium", [_elevation_grid(150, salt=8232, locked_corner=True)]),
            ("03-large", [_elevation_grid(300, salt=8233, locked_corner=True)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 시각을 이분 탐색하고, 시각마다 BFS 로 닿는지 본다.
fun earliestSwimTime(grid: Array<IntArray>): Int {
    val n = grid.size
    val dr = intArrayOf(1, -1, 0, 0)
    val dc = intArrayOf(0, 0, 1, -1)
    val seen = BooleanArray(n * n)
    val queue = IntArray(n * n)
    fun reachable(t: Int): Boolean {
        Drill.compare(t, grid[0][0])
        if (grid[0][0] > t) return false
        java.util.Arrays.fill(seen, false)
        var head = 0; var tail = 0
        queue[tail++] = 0; seen[0] = true
        while (head < tail) {
            val cell = queue[head++]
            val r = cell / n; val c = cell % n
            Drill.visit(cell, grid[r][c])
            if (cell == n * n - 1) return true
            for (k in 0 until 4) {
                val nr = r + dr[k]; val nc = c + dc[k]
                if (nr < 0 || nr >= n || nc < 0 || nc >= n) continue
                val idx = nr * n + nc
                if (!seen[idx] && grid[nr][nc] <= t) { seen[idx] = true; queue[tail++] = idx }
            }
        }
        return false
    }
    var lo = maxOf(grid[0][0], grid[n - 1][n - 1])
    var hi = n * n - 1
    while (lo < hi) {
        val mid = (lo + hi) / 2
        if (reachable(mid)) hi = mid else lo = mid + 1
    }
    return lo
}
""",
    mutants=[
        ("min-sum-path--dijkstra-on-sum", "WRONG_ALGORITHM",
         "지나는 칸들의 높이 합이 가장 작은 길을 찾고 그 길의 최댓값을 답한다. 합과 최댓값은 다른 길을 고른다.",
         """
fun earliestSwimTime(grid: Array<IntArray>): Int {
    val n = grid.size
    val dist = LongArray(n * n) { Long.MAX_VALUE }
    val peak = IntArray(n * n)
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    dist[0] = grid[0][0].toLong(); peak[0] = grid[0][0]
    heap.add(longArrayOf(dist[0], 0L))
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    while (heap.isNotEmpty()) {
        val top = heap.poll(); val d = top[0]; val cell = top[1].toInt()
        if (d > dist[cell]) continue
        val r = cell / n; val c = cell % n
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr < 0 || nr >= n || nc < 0 || nc >= n) continue
            val idx = nr * n + nc; val nd = d + grid[nr][nc]
            if (nd < dist[idx]) { dist[idx] = nd; peak[idx] = maxOf(peak[cell], grid[nr][nc]); heap.add(longArrayOf(nd, idx.toLong())) }
        }
    }
    return peak[n * n - 1]
}
"""),
        ("ignores-start-height", "OFF_BY_ONE",
         "출발 칸의 높이를 최댓값에 넣지 않는다. 출발 칸이 가장 높으면 틀린다.",
         """
fun earliestSwimTime(grid: Array<IntArray>): Int {
    val n = grid.size
    val best = IntArray(n * n) { Int.MAX_VALUE }
    val heap = java.util.PriorityQueue<IntArray>(compareBy { it[0] })
    best[0] = 0
    heap.add(intArrayOf(0, 0))
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    while (heap.isNotEmpty()) {
        val top = heap.poll(); val t = top[0]; val cell = top[1]
        if (t > best[cell]) continue
        if (cell == n * n - 1) return t
        val r = cell / n; val c = cell % n
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr < 0 || nr >= n || nc < 0 || nc >= n) continue
            val idx = nr * n + nc; val nt = maxOf(t, grid[nr][nc])
            if (nt < best[idx]) { best[idx] = nt; heap.add(intArrayOf(nt, idx)) }
        }
    }
    return best[n * n - 1]
}
"""),
        ("greedy-lowest-neighbor", "WRONG_ALGORITHM",
         "매번 가장 낮은 이웃으로만 나아간다. 돌아가야 최댓값이 낮아지는 격자에서 틀린다.",
         """
fun earliestSwimTime(grid: Array<IntArray>): Int {
    val n = grid.size
    val seen = BooleanArray(n * n)
    var r = 0; var c = 0
    var peak = grid[0][0]
    seen[0] = true
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    while (!(r == n - 1 && c == n - 1)) {
        var bestR = -1; var bestC = -1
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr < 0 || nr >= n || nc < 0 || nc >= n || seen[nr * n + nc]) continue
            if (bestR == -1 || grid[nr][nc] < grid[bestR][bestC]) { bestR = nr; bestC = nc }
        }
        if (bestR == -1) return n * n - 1
        r = bestR; c = bestC; seen[r * n + c] = true
        peak = maxOf(peak, grid[r][c])
    }
    return peak
}
"""),
        ("linear-time-scan", "PERFORMANCE",
         "시각을 0 부터 하나씩 올리며 매번 BFS 를 돌린다. 이분 탐색이 없어 O(n⁴).",
         """
fun earliestSwimTime(grid: Array<IntArray>): Int {
    val n = grid.size
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    fun reachable(t: Int): Boolean {
        if (grid[0][0] > t) return false
        val seen = BooleanArray(n * n)
        val queue = ArrayDeque<Int>()
        queue.addLast(0); seen[0] = true
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            if (cell == n * n - 1) return true
            val r = cell / n; val c = cell % n
            for (k in 0 until 4) {
                val nr = r + dr[k]; val nc = c + dc[k]
                if (nr < 0 || nr >= n || nc < 0 || nc >= n) continue
                val idx = nr * n + nc
                Drill.compare(t, grid[nr][nc])
                if (!seen[idx] && grid[nr][nc] <= t) { seen[idx] = true; queue.addLast(idx) }
            }
        }
        return false
    }
    var t = 0
    while (!reachable(t)) t += 1
    return t
}
"""),
    ],
))


# --- 147. 가장 편한 길 (임계값 이분 탐색 + BFS) ---------------------------------------------------------

def _minimum_effort(heights):
    from collections import deque
    rows, cols = len(heights), len(heights[0])

    def reachable(limit):
        seen = [[False] * cols for _ in range(rows)]
        seen[0][0] = True
        queue = deque([(0, 0)])
        while queue:
            r, c = queue.popleft()
            if (r, c) == (rows - 1, cols - 1):
                return True
            for nr, nc in ((r + 1, c), (r - 1, c), (r, c + 1), (r, c - 1)):
                if 0 <= nr < rows and 0 <= nc < cols and not seen[nr][nc] and abs(heights[nr][nc] - heights[r][c]) <= limit:
                    seen[nr][nc] = True
                    queue.append((nr, nc))
        return False

    lo, hi = 0, 1_000_000
    while lo < hi:
        mid = (lo + hi) // 2
        if reachable(mid):
            hi = mid
        else:
            lo = mid + 1
    return lo


def _height_grid(rows, cols, lo, hi, salt):
    values = randoms(rows * cols, lo, hi, salt=salt)
    return [values[r * cols:(r + 1) * cols] for r in range(rows)]


PROBLEMS.append(Problem(
    id="minimum-effort-path",
    title="가장 편한 길",
    summary="""
높이 격자 `heights` 가 주어진다. 왼쪽 위 `(0, 0)` 에서 오른쪽 아래까지 상하좌우로 움직인다.
한 경로의 **수고**는 그 경로에서 인접한 두 칸의 높이 차의 절댓값 중 **최댓값**이다. 가능한 모든
경로의 수고 중 최솟값을 반환한다.
""",
    notes="""
합이 아니라 최댓값이라 보통의 최단 경로가 아니다. "수고가 `k` 이하인 경로가 있는가"는 높이
차가 `k` 이하인 간선만으로 BFS 하면 되고, 그 답은 `k` 에 대해 단조라 `k` 를 이분 탐색한다.
Dijkstra 의 완화를 `max(지금까지, 이 간선)` 으로 바꾸어도 된다. 칸이 하나면 답은 0 이다.
""",
    drill_doc="""
Drill.compare(lo, hi)         // 임계값의 범위를 좁혔다
Drill.write(0, limit)         // 답을 정했다
""",
    constraints="""
- `1 <= rows, cols <= 300`
- `0 <= heights[r][c] <= 1_000_000`
""",
    signature=dict(name="minimumEffortPath", parameters=[("heights", "INT_MATRIX")], returns="INT"),
    groups=perf_groups(time_multiplier=0.5),
    reference=_minimum_effort,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 65536},
    cases={
        "sample": [("01", [[[1, 2, 2], [3, 8, 2], [5, 3, 5]]]), ("02", [[[1, 2, 3], [3, 8, 4], [5, 3, 5]]])],
        "boundary": [
            ("01-single-cell", [[[7]]]),
            ("02-single-row", [[[1, 10, 6, 7, 9, 10, 4, 9]]]),
            ("03-single-column", [[[3], [9], [4]]]),
            # 뱀처럼 돌아가는 길이 더 편하다 — 왼쪽으로도 가야 한다.
            ("04-detour-goes-left", [[[1, 1, 1, 1], [9, 9, 9, 1], [1, 1, 1, 1], [1, 9, 9, 9], [1, 1, 1, 1]]]),
            ("05-flat", [[[5, 5, 5], [5, 5, 5]]]),
            ("06-max-difference", [[[0, 1000000], [1000000, 0]]]),
            ("07-answer-zero-with-wall", [[[1, 1, 9], [9, 1, 9], [9, 1, 1]]]),
        ],
        "hidden": [
            ("01-random-small", [_height_grid(4, 5, 0, 20, salt=8761)]),
            ("02-random-medium", [_height_grid(20, 30, 0, 1000, salt=8762)]),
            ("03-random-wide", [_height_grid(40, 40, 0, 1000000, salt=8763)]),
            ("04-narrow-values", [_height_grid(50, 50, 100, 105, salt=8764)]),
        ],
        "performance": [
            ("01-small", [_height_grid(100, 100, 0, 1000000, salt=8771)]),
            ("02-medium", [_height_grid(200, 200, 0, 1000000, salt=8772)]),
            ("03-large", [_height_grid(300, 300, 0, 1000000, salt=8773)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 임계값 이분 탐색 + BFS.
fun minimumEffortPath(heights: Array<IntArray>): Int {
    val rows = heights.size; val cols = heights[0].size
    val seen = BooleanArray(rows * cols)
    val queue = IntArray(rows * cols)
    fun reachable(limit: Int): Boolean {
        seen.fill(false)
        var head = 0; var tail = 0
        queue[tail++] = 0; seen[0] = true
        while (head < tail) {
            val cell = queue[head++]
            if (cell == rows * cols - 1) return true
            val r = cell / cols; val c = cell % cols
            val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
            for (k in 0 until 4) {
                val nr = r + dr[k]; val nc = c + dc[k]
                if (nr !in 0 until rows || nc !in 0 until cols) continue
                val next = nr * cols + nc
                if (!seen[next] && kotlin.math.abs(heights[nr][nc] - heights[r][c]) <= limit) { seen[next] = true; queue[tail++] = next }
            }
        }
        return false
    }
    var lo = 0; var hi = 1_000_000
    while (lo < hi) {
        val mid = (lo + hi) / 2
        Drill.compare(lo, hi)
        if (reachable(mid)) hi = mid else lo = mid + 1
    }
    Drill.write(0, lo)
    return lo
}
""",
    mutants=[
        ("only-right-and-down", "MISSING_EDGE_CASE",
         "오른쪽과 아래로만 간다. 돌아가는 길이 더 편할 때 틀린다.",
         """
fun minimumEffortPath(heights: Array<IntArray>): Int {
    val rows = heights.size; val cols = heights[0].size
    val seen = BooleanArray(rows * cols)
    val queue = IntArray(rows * cols)
    fun reachable(limit: Int): Boolean {
        seen.fill(false)
        var head = 0; var tail = 0
        queue[tail++] = 0; seen[0] = true
        while (head < tail) {
            val cell = queue[head++]
            if (cell == rows * cols - 1) return true
            val r = cell / cols; val c = cell % cols
            val dr = intArrayOf(1, 0); val dc = intArrayOf(0, 1)
            for (k in 0 until 2) {
                val nr = r + dr[k]; val nc = c + dc[k]
                if (nr !in 0 until rows || nc !in 0 until cols) continue
                val next = nr * cols + nc
                if (!seen[next] && kotlin.math.abs(heights[nr][nc] - heights[r][c]) <= limit) { seen[next] = true; queue[tail++] = next }
            }
        }
        return false
    }
    var lo = 0; var hi = 1_000_000
    while (lo < hi) { val mid = (lo + hi) / 2; if (reachable(mid)) hi = mid else lo = mid + 1 }
    return lo
}
"""),
        ("sum-of-differences--dijkstra", "WRONG_ALGORITHM",
         "수고를 최댓값이 아니라 합으로 잰다. 보통의 최단 경로가 된다.",
         """
fun minimumEffortPath(heights: Array<IntArray>): Int {
    val rows = heights.size; val cols = heights[0].size
    val dist = LongArray(rows * cols) { Long.MAX_VALUE }
    val heap = java.util.PriorityQueue<Pair<Long, Int>>(compareBy { it.first })
    dist[0] = 0; heap.add(0L to 0)
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    var bestMax = 0
    val maxAlong = IntArray(rows * cols)
    while (heap.isNotEmpty()) {
        val (d, cell) = heap.poll()
        if (d > dist[cell]) continue
        if (cell == rows * cols - 1) { bestMax = maxAlong[cell]; break }
        val r = cell / cols; val c = cell % cols
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr !in 0 until rows || nc !in 0 until cols) continue
            val diff = kotlin.math.abs(heights[nr][nc] - heights[r][c])
            val next = nr * cols + nc
            if (d + diff < dist[next]) { dist[next] = d + diff; maxAlong[next] = maxOf(maxAlong[cell], diff); heap.add(dist[next] to next) }
        }
    }
    return bestMax
}
"""),
        ("binary-search-excludes-answer", "OFF_BY_ONE",
         "탐색이 답을 한 칸 지나친다 — 도달하면 hi = mid - 1.",
         """
fun minimumEffortPath(heights: Array<IntArray>): Int {
    val rows = heights.size; val cols = heights[0].size
    val seen = BooleanArray(rows * cols)
    val queue = IntArray(rows * cols)
    fun reachable(limit: Int): Boolean {
        seen.fill(false)
        var head = 0; var tail = 0
        queue[tail++] = 0; seen[0] = true
        while (head < tail) {
            val cell = queue[head++]
            if (cell == rows * cols - 1) return true
            val r = cell / cols; val c = cell % cols
            val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
            for (k in 0 until 4) {
                val nr = r + dr[k]; val nc = c + dc[k]
                if (nr !in 0 until rows || nc !in 0 until cols) continue
                val next = nr * cols + nc
                if (!seen[next] && kotlin.math.abs(heights[nr][nc] - heights[r][c]) <= limit) { seen[next] = true; queue[tail++] = next }
            }
        }
        return false
    }
    var lo = 0; var hi = 1_000_000
    while (lo < hi) { val mid = (lo + hi) / 2; if (reachable(mid)) hi = maxOf(lo, mid - 1) else lo = mid + 1 }
    return lo
}
"""),
        ("linear-scan-of-threshold", "PERFORMANCE",
         "임계값을 0 부터 하나씩 올리며 BFS 한다. 답이 크면 수십만 번의 BFS 다.",
         """
fun minimumEffortPath(heights: Array<IntArray>): Int {
    val rows = heights.size; val cols = heights[0].size
    val seen = BooleanArray(rows * cols)
    val queue = IntArray(rows * cols)
    fun reachable(limit: Int): Boolean {
        seen.fill(false)
        var head = 0; var tail = 0
        queue[tail++] = 0; seen[0] = true
        while (head < tail) {
            val cell = queue[head++]
            if (cell == rows * cols - 1) return true
            val r = cell / cols; val c = cell % cols
            val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
            for (k in 0 until 4) {
                val nr = r + dr[k]; val nc = c + dc[k]
                if (nr !in 0 until rows || nc !in 0 until cols) continue
                val next = nr * cols + nc
                if (!seen[next] && kotlin.math.abs(heights[nr][nc] - heights[r][c]) <= limit) { seen[next] = true; queue[tail++] = next }
            }
        }
        return false
    }
    var limit = 0
    while (!reachable(limit)) { Drill.compare(limit, limit + 1); limit += 1 }
    return limit
}
"""),
    ],
))


# --- 156. 정렬된 격자의 음수 개수 (계단 훑기) ---------------------------------------------------------

def _count_negatives(grid):
    rows, cols = len(grid), len(grid[0])
    count = 0
    col = cols - 1
    for r in range(rows):
        while col >= 0 and grid[r][col] < 0:
            col -= 1
        count += cols - 1 - col
    return count


def _sorted_desc_grid(rows, cols, lo, hi, salt):
    values = sorted(randoms(rows * cols, lo, hi, salt=salt), reverse=True)
    # 행마다 내림차순, 열마다 내림차순이 되게 — 전체 내림차순을 행 우선으로 채우면 둘 다 성립한다.
    return [values[r * cols:(r + 1) * cols] for r in range(rows)]


PROBLEMS.append(Problem(
    id="count-negatives-sorted-grid",
    title="정렬된 격자의 음수 개수",
    summary="""
각 행과 각 열이 **내림차순**으로 정렬된 정수 격자 `grid` 가 주어진다. 음수의 개수를 반환한다.
""",
    notes="""
전부 세어도 답은 나온다. 정렬을 쓰면 더 적게 본다 — 행은 내림차순이라 음수는 행의 **오른쪽
끝에 몰려 있고**, 열도 내림차순이라 아래 행의 음수 경계는 윗 행보다 **왼쪽에 있거나 같다.** 그래서
오른쪽 위에서 시작해 음수면 왼쪽으로, 아니면 아래로 움직이는 계단 한 번(행 + 열)으로 끝난다.
`0` 은 음수가 아니다.
""",
    drill_doc="""
Drill.compare(r, c)           // 칸을 봤다
Drill.write(r, count)         // 행의 음수 수를 더했다
""",
    constraints="""
- `1 <= rows, cols <= 100`
- `-100 <= grid[r][c] <= 100`, 행과 열이 모두 내림차순
""",
    signature=dict(name="countNegatives", parameters=[("grid", "INT_MATRIX")], returns="INT"),
    groups=standard_groups(),
    reference=_count_negatives,
    cases={
        "sample": [("01", [[[4, 3, 2, -1], [3, 2, 1, -1], [1, 1, -1, -2], [-1, -1, -2, -3]]]), ("02", [[[3, 2], [1, 0]]])],
        "boundary": [
            ("01-single-negative", [[[-1]]]),
            ("02-single-positive", [[[5]]]),
            # 0 은 음수가 아니다.
            ("03-zeros", [[[1, 0], [0, 0]]]),
            ("04-all-negative", [[[-1, -2], [-3, -4]]]),
            ("05-single-row", [[[5, 1, 0, -1, -2]]]),
            ("06-single-column", [[[3], [0], [-1]]]),
            # 경계가 행마다 왼쪽으로 크게 움직인다.
            ("07-staircase", [[[5, 4, 3, 2, 1], [4, 3, -1, -2, -3], [-1, -2, -3, -4, -5]]]),
        ],
        "hidden": [
            ("01-random-small", [_sorted_desc_grid(3, 4, -5, 5, salt=8901)]),
            ("02-random-medium", [_sorted_desc_grid(20, 30, -100, 100, salt=8902)]),
            ("03-random-max", [_sorted_desc_grid(100, 100, -100, 100, salt=8903)]),
            ("04-mostly-positive", [_sorted_desc_grid(50, 50, -3, 100, salt=8904)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 오른쪽 위에서 시작하는 계단.
fun countNegatives(grid: Array<IntArray>): Int {
    val cols = grid[0].size
    var count = 0
    var col = cols - 1
    for (r in grid.indices) {
        while (col >= 0 && grid[r][col] < 0) { Drill.compare(r, col); col -= 1 }
        count += cols - 1 - col
        Drill.write(r, count)
    }
    return count
}
""",
    mutants=[
        ("counts-zero-as-negative", "OFF_BY_ONE",
         "0 도 센다. 음수는 0 보다 작은 것이다.",
         """
fun countNegatives(grid: Array<IntArray>): Int {
    val cols = grid[0].size
    var count = 0
    var col = cols - 1
    for (r in grid.indices) {
        while (col >= 0 && grid[r][col] <= 0) col -= 1
        count += cols - 1 - col
    }
    return count
}
"""),
        ("counts-one-per-row", "WRONG_BRANCH",
         "행마다 음수가 있는지만 보고 하나로 센다.",
         """
fun countNegatives(grid: Array<IntArray>): Int {
    var count = 0
    for (row in grid) {
        for (c in row.indices.reversed()) { if (row[c] < 0) { count += 1; break } }
    }
    return count
}
"""),
        ("starts-at-top-left", "WRONG_ALGORITHM",
         "왼쪽 위에서 시작해 음수인 동안 오른쪽으로 간다. 음수는 오른쪽 끝에 몰려 있어 거의 아무것도 세지 못한다.",
         """
fun countNegatives(grid: Array<IntArray>): Int {
    val cols = grid[0].size
    var count = 0
    var col = 0
    for (r in grid.indices) {
        while (col < cols && grid[r][col] < 0) col += 1
        count += col
    }
    return count
}
"""),
    ],
))


# --- 161. 지형에 고이는 물 (2차원, 힙으로 바깥부터) ------------------------------------------------------

def _trap_2d(heights):
    import heapq
    rows, cols = len(heights), len(heights[0])
    if rows < 3 or cols < 3:
        return 0
    seen = [[False] * cols for _ in range(rows)]
    heap = []
    for r in range(rows):
        for c in (0, cols - 1):
            if not seen[r][c]:
                seen[r][c] = True
                heapq.heappush(heap, (heights[r][c], r, c))
    for c in range(cols):
        for r in (0, rows - 1):
            if not seen[r][c]:
                seen[r][c] = True
                heapq.heappush(heap, (heights[r][c], r, c))
    water = 0
    while heap:
        level, r, c = heapq.heappop(heap)
        for nr, nc in ((r + 1, c), (r - 1, c), (r, c + 1), (r, c - 1)):
            if 0 <= nr < rows and 0 <= nc < cols and not seen[nr][nc]:
                seen[nr][nc] = True
                water += max(0, level - heights[nr][nc])
                heapq.heappush(heap, (max(level, heights[nr][nc]), nr, nc))
    return water


def _terrain(rows, cols, lo, hi, salt):
    values = randoms(rows * cols, lo, hi, salt=salt)
    return [values[r * cols:(r + 1) * cols] for r in range(rows)]


def _bowl(n, rim, floor):
    return [[rim if r in (0, n - 1) or c in (0, n - 1) else floor for c in range(n)] for r in range(n)]


PROBLEMS.append(Problem(
    id="trapping-rain-water-2d",
    title="지형에 고이는 물",
    summary="""
높이 격자 `heights` 에 비가 온다. 물은 상하좌우로 흐르고 격자의 **가장자리 밖으로** 빠져나간다.
다 흐른 뒤 격자 위에 고여 있는 물의 총량(칸마다 고인 높이의 합)을 반환한다.
""",
    notes="""
1차원의 "양쪽 최대 중 작은 것"이 2차원에서는 "밖으로 나가는 모든 길 중 가장 낮은 고개"다. 그것은
**바깥에서 안으로** 채우면 나온다: 가장자리 칸을 전부 최소 힙에 넣고, 가장 낮은 칸부터 꺼내 이웃을
본다 — 이웃이 지금 수위보다 낮으면 그 차이만큼 물이 고이고, 이웃은 `max(수위, 이웃 높이)` 로 힙에
들어간다. 한 칸이 한 번씩 들어가니 O(RC log RC) 다.
""",
    drill_doc="""
Drill.compare(r, c)           // 칸을 꺼냈다
Drill.write(0, water)         // 물을 더했다
""",
    constraints="""
- `1 <= rows, cols <= 200`
- `0 <= heights[r][c] <= 20_000`
""",
    signature=dict(name="trappingRainWater2d", parameters=[("heights", "INT_MATRIX")], returns="INT"),
    groups=perf_groups(time_multiplier=0.5),
    reference=_trap_2d,
    cases={
        "sample": [("01", [[[1, 4, 3, 1, 3, 2], [3, 2, 1, 3, 2, 4], [2, 3, 3, 2, 3, 1]]]), ("02", [[[3, 3, 3], [3, 1, 3], [3, 3, 3]]])],
        "boundary": [
            ("01-too-thin", [[[5, 1, 5]]]),
            ("02-two-rows", [[[5, 1, 5], [5, 1, 5]]]),
            ("03-flat", [[[2, 2, 2], [2, 2, 2], [2, 2, 2]]]),
            # 테두리에 낮은 곳이 하나 — 물은 그 고개 높이까지만 고인다.
            ("04-leaky-rim", [[[5, 5, 5, 5], [5, 1, 1, 5], [5, 1, 1, 5], [5, 5, 2, 5]]]),
            # 안쪽에 더 높은 봉우리 — 물이 고이는 칸과 아닌 칸이 섞인다.
            ("05-inner-peak", [[[9, 9, 9, 9, 9], [9, 1, 9, 1, 9], [9, 1, 9, 1, 9], [9, 9, 9, 9, 9]]]),
            ("06-deep-bowl", [_bowl(5, 20000, 0)]),
            # 안이 바깥보다 높으면 아무것도 안 고인다.
            ("07-hill", [[[1, 1, 1], [1, 9, 1], [1, 1, 1]]]),
        ],
        "hidden": [
            ("01-random-small", [_terrain(5, 6, 0, 9, salt=9001)]),
            ("02-random-medium", [_terrain(20, 25, 0, 100, salt=9002)]),
            ("03-random-wide", [_terrain(40, 40, 0, 20000, salt=9003)]),
            # 물이 계단처럼 흐른다 — 바깥에서 안으로 채워야 맞다.
            ("04-nested-bowls", [[[8, 8, 8, 8, 8, 8, 8], [8, 2, 2, 2, 2, 2, 8], [8, 2, 6, 6, 6, 2, 8], [8, 2, 6, 0, 6, 2, 8], [8, 2, 6, 6, 6, 2, 8], [8, 2, 2, 2, 2, 2, 8], [8, 8, 8, 8, 8, 8, 8]]]),
        ],
        "performance": [
            ("01-small", [_terrain(60, 60, 0, 20000, salt=9011)]),
            ("02-medium", [_terrain(120, 120, 0, 20000, salt=9012)]),
            ("03-large", [_terrain(200, 200, 0, 20000, salt=9013)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 가장자리에서 시작하는 최소 힙.
fun trappingRainWater2d(heights: Array<IntArray>): Int {
    val rows = heights.size; val cols = heights[0].size
    if (rows < 3 || cols < 3) return 0
    val seen = BooleanArray(rows * cols)
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    fun push(level: Int, r: Int, c: Int) { if (!seen[r * cols + c]) { seen[r * cols + c] = true; heap.add(longArrayOf(level.toLong(), r.toLong(), c.toLong())) } }
    for (r in 0 until rows) { push(heights[r][0], r, 0); push(heights[r][cols - 1], r, cols - 1) }
    for (c in 0 until cols) { push(heights[0][c], 0, c); push(heights[rows - 1][c], rows - 1, c) }
    var water = 0L
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    while (heap.isNotEmpty()) {
        val top = heap.poll()
        val level = top[0].toInt(); val r = top[1].toInt(); val c = top[2].toInt()
        Drill.compare(r, c)
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr !in 0 until rows || nc !in 0 until cols || seen[nr * cols + nc]) continue
            if (heights[nr][nc] < level) { water += level - heights[nr][nc]; Drill.write(0, water.toInt()) }
            push(maxOf(level, heights[nr][nc]), nr, nc)
        }
    }
    return water.toInt()
}
""",
    mutants=[
        ("pushes-own-height-not-level", "WRONG_BRANCH",
         "이웃을 수위가 아니라 자기 높이로 힙에 넣는다. 안쪽 낮은 칸의 수위가 새어 물이 적게 잡힌다.",
         """
fun trappingRainWater2d(heights: Array<IntArray>): Int {
    val rows = heights.size; val cols = heights[0].size
    if (rows < 3 || cols < 3) return 0
    val seen = BooleanArray(rows * cols)
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    fun push(level: Int, r: Int, c: Int) { if (!seen[r * cols + c]) { seen[r * cols + c] = true; heap.add(longArrayOf(level.toLong(), r.toLong(), c.toLong())) } }
    for (r in 0 until rows) { push(heights[r][0], r, 0); push(heights[r][cols - 1], r, cols - 1) }
    for (c in 0 until cols) { push(heights[0][c], 0, c); push(heights[rows - 1][c], rows - 1, c) }
    var water = 0L
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    while (heap.isNotEmpty()) {
        val top = heap.poll(); val level = top[0].toInt(); val r = top[1].toInt(); val c = top[2].toInt()
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr !in 0 until rows || nc !in 0 until cols || seen[nr * cols + nc]) continue
            if (heights[nr][nc] < level) water += level - heights[nr][nc]
            push(heights[nr][nc], nr, nc)
        }
    }
    return water.toInt()
}
"""),
        ("row-wise-1d", "WRONG_ALGORITHM",
         "행마다 1차원 빗물 문제를 풀어 더한다. 물은 위아래로도 새어 나간다.",
         """
fun trappingRainWater2d(heights: Array<IntArray>): Int {
    val rows = heights.size
    if (rows < 3) return 0
    var water = 0
    for (r in 1 until rows - 1) {
        val row = heights[r]
        var lo = 0; var hi = row.size - 1; var leftMax = 0; var rightMax = 0
        while (lo < hi) {
            if (row[lo] < row[hi]) { leftMax = maxOf(leftMax, row[lo]); water += leftMax - row[lo]; lo += 1 }
            else { rightMax = maxOf(rightMax, row[hi]); water += rightMax - row[hi]; hi -= 1 }
        }
    }
    return water
}
"""),
        ("fifo-queue-not-heap", "WRONG_ALGORITHM",
         "최소 힙 대신 보통 큐로 바깥부터 훑는다. 가장 낮은 고개부터 보지 않아 수위가 틀린다.",
         """
fun trappingRainWater2d(heights: Array<IntArray>): Int {
    val rows = heights.size; val cols = heights[0].size
    if (rows < 3 || cols < 3) return 0
    val seen = BooleanArray(rows * cols)
    val queue = ArrayDeque<IntArray>()
    fun push(level: Int, r: Int, c: Int) { if (!seen[r * cols + c]) { seen[r * cols + c] = true; queue.addLast(intArrayOf(level, r, c)) } }
    for (r in 0 until rows) { push(heights[r][0], r, 0); push(heights[r][cols - 1], r, cols - 1) }
    for (c in 0 until cols) { push(heights[0][c], 0, c); push(heights[rows - 1][c], rows - 1, c) }
    var water = 0L
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    while (queue.isNotEmpty()) {
        val top = queue.removeFirst(); val level = top[0]; val r = top[1]; val c = top[2]
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr !in 0 until rows || nc !in 0 until cols || seen[nr * cols + nc]) continue
            if (heights[nr][nc] < level) water += level - heights[nr][nc]
            push(maxOf(level, heights[nr][nc]), nr, nc)
        }
    }
    return water.toInt()
}
"""),
        ("bfs-per-cell--quadratic", "PERFORMANCE",
         "칸마다 밖으로 나가는 가장 낮은 고개를 따로 찾는다. O((RC)²).",
         """
fun trappingRainWater2d(heights: Array<IntArray>): Int {
    val rows = heights.size; val cols = heights[0].size
    if (rows < 3 || cols < 3) return 0
    var water = 0L
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    for (sr in 1 until rows - 1) for (sc in 1 until cols - 1) {
        // 이 칸에서 밖으로 나가는 길들의 최댓값의 최솟값 — 수위를 낮은 것부터 올려 가며 닿는지 본다.
        val heap = java.util.PriorityQueue<IntArray>(compareBy { it[0] })
        val seen = BooleanArray(rows * cols)
        heap.add(intArrayOf(heights[sr][sc], sr, sc)); seen[sr * cols + sc] = true
        var pass = 0
        while (heap.isNotEmpty()) {
            val top = heap.poll(); val level = maxOf(pass, top[0]); val r = top[1]; val c = top[2]
            Drill.compare(r, c)
            pass = level
            if (r == 0 || c == 0 || r == rows - 1 || c == cols - 1) break
            for (k in 0 until 4) {
                val nr = r + dr[k]; val nc = c + dc[k]
                if (nr !in 0 until rows || nc !in 0 until cols || seen[nr * cols + nc]) continue
                seen[nr * cols + nc] = true; heap.add(intArrayOf(heights[nr][nc], nr, nc))
            }
        }
        water += maxOf(0, pass - heights[sr][sc])
    }
    return water.toInt()
}
"""),
    ],
))


# --- 162. 체리 줍기 (두 사람이 동시에 — 3차원 DP) --------------------------------------------------------

def _cherry_pickup(grid):
    n = len(grid)
    NEG = float("-inf")
    # dp[r1][r2]: 걸음 수 t 에서 사람 1 이 (r1, t-r1), 사람 2 가 (r2, t-r2) 에 있을 때의 최대.
    dp = [[NEG] * n for _ in range(n)]
    dp[0][0] = grid[0][0]
    if grid[0][0] < 0:
        return 0
    for t in range(1, 2 * n - 1):
        nxt = [[NEG] * n for _ in range(n)]
        for r1 in range(max(0, t - n + 1), min(n, t + 1)):
            c1 = t - r1
            if grid[r1][c1] < 0:
                continue
            for r2 in range(max(0, t - n + 1), min(n, t + 1)):
                c2 = t - r2
                if grid[r2][c2] < 0:
                    continue
                best = NEG
                for pr1 in (r1 - 1, r1):
                    for pr2 in (r2 - 1, r2):
                        if 0 <= pr1 < n and 0 <= pr2 < n and dp[pr1][pr2] > best:
                            best = dp[pr1][pr2]
                if best == NEG:
                    continue
                gain = grid[r1][c1] + (grid[r2][c2] if r1 != r2 else 0)
                nxt[r1][r2] = best + gain
        dp = nxt
    return max(0, dp[n - 1][n - 1]) if dp[n - 1][n - 1] != NEG else 0


def _cherry_grid(n, salt, thorn_pct=15, cherry_pct=40):
    values = randoms(n * n, 0, 99, salt=salt)
    grid = [[(-1 if v < thorn_pct else 1 if v < thorn_pct + cherry_pct else 0) for v in values[r * n:(r + 1) * n]] for r in range(n)]
    grid[0][0] = 0
    grid[n - 1][n - 1] = 0
    return grid


PROBLEMS.append(Problem(
    id="cherry-pickup",
    title="체리 줍기",
    summary="""
`n × n` 격자의 칸은 `0`(빈 칸), `1`(체리 하나), `-1`(가시 — 못 지나간다)이다. `(0, 0)` 에서 오른쪽·
아래로만 움직여 `(n-1, n-1)` 에 간 뒤, 다시 왼쪽·위로만 움직여 `(0, 0)` 으로 돌아온다. 지나는 칸의
체리는 줍고 그 칸은 `0` 이 된다. 주울 수 있는 체리의 최대 수를 반환한다. 갈 길이 없으면 `0`.
""",
    notes="""
갔다가 돌아오는 것은 **두 사람이 동시에 출발해 같은 걸음 수로 내려가는 것**과 같다 — 돌아오는
길을 뒤집으면 또 하나의 내려가는 길이다. 걸음 수 `t` 에서 두 사람의 행 `r1, r2` 만 알면 열은
`t − r` 로 정해지니 상태는 `(t, r1, r2)` 이고 O(n³) 이다. 같은 칸에 있으면 체리는 한 번만 센다.
왕복을 따로 최적화하면(내려가며 최대로 줍고, 남은 격자에서 돌아오며 최대로) 틀린다.
""",
    drill_doc="""
Drill.compare(r1, r2)         // 두 사람의 자리를 봤다
Drill.write(t, best)          // 걸음 수의 최대를 정했다
""",
    constraints="""
- `1 <= n <= 60`
- `grid[r][c]` 는 `-1, 0, 1`, `grid[0][0]` 과 `grid[n-1][n-1]` 은 `-1` 이 아니다
""",
    signature=dict(name="cherryPickup", parameters=[("grid", "INT_MATRIX")], returns="INT"),
    groups=perf_groups(time_multiplier=0.5),
    reference=_cherry_pickup,
    cases={
        "sample": [("01", [[[0, 1, -1], [1, 0, -1], [1, 1, 1]]]), ("02", [[[1, 1, -1], [1, -1, 1], [-1, 1, 1]]])],
        "boundary": [
            ("01-single", [[[0]]]),
            ("02-single-cherry", [[[1]]]),
            ("03-two-by-two", [[[0, 1], [1, 0]]]),
            # 따로 최적화하면 틀린다 — 내려갈 때 최대로 주우면 돌아올 길이 빈다.
            ("04-greedy-fails", [[[1, 1, 1, 0, 0], [0, 0, 1, 0, 1], [1, 0, 1, 0, 0], [0, 0, 1, 0, 0], [0, 0, 1, 1, 1]]]),
            ("05-blocked", [[[0, -1], [-1, 0]]]),
            ("06-all-cherries", [[[1, 1, 1], [1, 1, 1], [1, 1, 1]]]),
            # 같은 칸을 둘이 지나면 한 번만.
            ("07-single-corridor", [[[1, -1, -1], [1, -1, -1], [1, 1, 1]]]),
        ],
        "hidden": [
            ("01-random-small", [_cherry_grid(5, salt=9021)]),
            ("02-random-medium", [_cherry_grid(12, salt=9022)]),
            ("03-random-sparse-thorns", [_cherry_grid(20, salt=9023, thorn_pct=5)]),
            ("04-random-many-thorns", [_cherry_grid(15, salt=9024, thorn_pct=35)]),
        ],
        "performance": [
            ("01-small", [_cherry_grid(30, salt=9031, thorn_pct=5)]),
            ("02-medium", [_cherry_grid(45, salt=9032, thorn_pct=5)]),
            ("03-large", [_cherry_grid(60, salt=9033, thorn_pct=3)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 걸음 수마다 (r1, r2) 표.
fun cherryPickup(grid: Array<IntArray>): Int {
    val n = grid.size
    if (grid[0][0] < 0) return 0
    val neg = Int.MIN_VALUE / 2
    var dp = Array(n) { IntArray(n) { neg } }
    dp[0][0] = grid[0][0]
    for (t in 1 until 2 * n - 1) {
        val next = Array(n) { IntArray(n) { neg } }
        val lo = maxOf(0, t - n + 1); val hi = minOf(n - 1, t)
        for (r1 in lo..hi) {
            val c1 = t - r1
            if (grid[r1][c1] < 0) continue
            for (r2 in lo..hi) {
                val c2 = t - r2
                if (grid[r2][c2] < 0) continue
                var best = neg
                for (p1 in r1 - 1..r1) for (p2 in r2 - 1..r2) if (p1 >= 0 && p2 >= 0 && dp[p1][p2] > best) best = dp[p1][p2]
                if (best == neg) continue
                Drill.compare(r1, r2)
                next[r1][r2] = best + grid[r1][c1] + (if (r1 != r2) grid[r2][c2] else 0)
            }
        }
        dp = next
        Drill.write(t, maxOf(0, dp[minOf(n - 1, t)][minOf(n - 1, t)]))
    }
    return maxOf(0, dp[n - 1][n - 1])
}
""",
    mutants=[
        ("counts-shared-cell-twice", "OFF_BY_ONE",
         "두 사람이 같은 칸에 있어도 체리를 두 번 센다.",
         """
fun cherryPickup(grid: Array<IntArray>): Int {
    val n = grid.size
    if (grid[0][0] < 0) return 0
    val neg = Int.MIN_VALUE / 2
    var dp = Array(n) { IntArray(n) { neg } }
    dp[0][0] = grid[0][0]
    for (t in 1 until 2 * n - 1) {
        val next = Array(n) { IntArray(n) { neg } }
        val lo = maxOf(0, t - n + 1); val hi = minOf(n - 1, t)
        for (r1 in lo..hi) { val c1 = t - r1; if (grid[r1][c1] < 0) continue
            for (r2 in lo..hi) { val c2 = t - r2; if (grid[r2][c2] < 0) continue
                var best = neg
                for (p1 in r1 - 1..r1) for (p2 in r2 - 1..r2) if (p1 >= 0 && p2 >= 0 && dp[p1][p2] > best) best = dp[p1][p2]
                if (best == neg) continue
                next[r1][r2] = best + grid[r1][c1] + grid[r2][c2]
            }
        }
        dp = next
    }
    return maxOf(0, dp[n - 1][n - 1])
}
"""),
        ("two-greedy-passes", "WRONG_ALGORITHM",
         "내려가며 최대로 줍고, 남은 격자에서 돌아오며 최대로 줍는다. 합이 최대가 아니다.",
         """
fun cherryPickup(grid: Array<IntArray>): Int {
    val n = grid.size
    val neg = Int.MIN_VALUE / 2
    val g = Array(n) { grid[it].copyOf() }
    fun bestPath(): Int {
        val dp = Array(n) { IntArray(n) { neg } }
        if (g[0][0] < 0) return 0
        dp[0][0] = g[0][0]
        for (r in 0 until n) for (c in 0 until n) {
            if (g[r][c] < 0 || (r == 0 && c == 0)) continue
            val up = if (r > 0) dp[r - 1][c] else neg
            val left = if (c > 0) dp[r][c - 1] else neg
            val b = maxOf(up, left)
            if (b > neg) dp[r][c] = b + g[r][c]
        }
        if (dp[n - 1][n - 1] <= neg) return 0
        // 경로를 되짚어 체리를 지운다.
        var r = n - 1; var c = n - 1
        while (r > 0 || c > 0) {
            g[r][c] = 0
            val up = if (r > 0) dp[r - 1][c] else neg
            val left = if (c > 0) dp[r][c - 1] else neg
            if (up >= left) r -= 1 else c -= 1
        }
        g[0][0] = 0
        return dp[n - 1][n - 1]
    }
    val first = bestPath()
    if (first == 0 && g[n - 1][n - 1] < 0) return 0
    return first + bestPath()
}
"""),
        ("thorn-treated-as-empty", "MISSING_EDGE_CASE",
         "가시를 빈 칸으로 본다. 못 지나가는 길로 간다.",
         """
fun cherryPickup(grid: Array<IntArray>): Int {
    val n = grid.size
    val neg = Int.MIN_VALUE / 2
    var dp = Array(n) { IntArray(n) { neg } }
    dp[0][0] = maxOf(0, grid[0][0])
    for (t in 1 until 2 * n - 1) {
        val next = Array(n) { IntArray(n) { neg } }
        val lo = maxOf(0, t - n + 1); val hi = minOf(n - 1, t)
        for (r1 in lo..hi) { val c1 = t - r1
            for (r2 in lo..hi) { val c2 = t - r2
                var best = neg
                for (p1 in r1 - 1..r1) for (p2 in r2 - 1..r2) if (p1 >= 0 && p2 >= 0 && dp[p1][p2] > best) best = dp[p1][p2]
                if (best == neg) continue
                next[r1][r2] = best + maxOf(0, grid[r1][c1]) + (if (r1 != r2) maxOf(0, grid[r2][c2]) else 0)
            }
        }
        dp = next
    }
    return maxOf(0, dp[n - 1][n - 1])
}
"""),
        # 좌표 넷으로 기억하는 판은 60⁴ = 1.3 × 10⁷ 상태라 1초 안에 들어 살아남았다 — 기억이 없는 판이다.
        ("no-memo--exponential", "PERFORMANCE",
         "두 사람의 자리를 기억하지 않고 재귀한다. 4^(2n) 이다.",
         """
fun cherryPickup(grid: Array<IntArray>): Int {
    val n = grid.size
    val neg = Int.MIN_VALUE / 2
    fun go(r1: Int, c1: Int, r2: Int, c2: Int): Int {
        if (r1 >= n || c1 >= n || r2 >= n || c2 >= n || grid[r1][c1] < 0 || grid[r2][c2] < 0) return neg
        if (r1 == n - 1 && c1 == n - 1) return grid[r1][c1]
        Drill.compare(r1, r2)
        var best = neg
        for (d1 in 0..1) for (d2 in 0..1) {
            val v = go(r1 + d1, c1 + 1 - d1, r2 + d2, c2 + 1 - d2)
            if (v > best) best = v
        }
        val gain = grid[r1][c1] + (if (r1 != r2 || c1 != c2) grid[r2][c2] else 0)
        return if (best == neg) neg else best + gain
    }
    return maxOf(0, go(0, 0, 0, 0))
}
"""),
    ],
))
