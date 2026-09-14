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

from author import Problem, perf_groups, randoms, standard_groups

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
    return sum(([rs[i], cs[i]] for i in range(count)), [])


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
