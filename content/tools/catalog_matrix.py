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
