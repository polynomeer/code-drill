"""트리·유니온파인드·단조 큐·누적합 (역량: 모델링 — 입력을 다룰 수 있는 구조로 옮기기).

트리는 부모 배열로 준다. `parent[i]` 가 `i` 의 부모이고 루트는 `-1` 이다. 간선 목록보다
한 단계 적은 변환이라, 재는 것이 "트리를 만들 수 있나"가 아니라 "트리 위에서 생각할 수
있나"가 된다.
"""

from author import Problem, standard_groups, perf_groups, randoms, shuffled

PROBLEMS = []


def _random_parents(n, salt):
    """i 의 부모를 0..i-1 중에서 뽑는다. 루트는 0."""
    picks = randoms(n, 0, 10 ** 9, salt=salt)
    return [-1] + [picks[i] % i for i in range(1, n)]


# --- 1. 트리의 높이 -------------------------------------------------------------

def _depths(parent):
    """정점별 깊이. 재귀 없이 — 20만 사슬에서 파이썬 재귀는 터진다."""
    n = len(parent)
    depth = [-1] * n
    for start in range(n):
        path = []
        v = start
        while v != -1 and depth[v] == -1:
            path.append(v)
            v = parent[v]
        d = -1 if v == -1 else depth[v]
        for u in reversed(path):
            d += 1
            depth[u] = d
    return depth


def _tree_height(parent):
    return max(_depths(parent))


PROBLEMS.append(Problem(
    id="tree-height",
    title="트리의 높이",
    summary="""
정점 `0..n-1` 의 트리가 부모 배열 `parent` 로 주어진다. `parent[i]` 는 `i` 의 부모이고
루트는 `-1` 이다. 루트에서 가장 먼 정점까지의 **간선 수**를 반환한다.

정점 하나뿐인 트리의 높이는 `0` 이다.
""",
    notes="""
정점마다 루트까지 올라가면 O(n²) 이다. 한 번 계산한 깊이를 기억해 두면 각 정점은 한 번만
올라간다. 부모가 자식보다 먼저 온다는 보장은 없다 — 순서를 믿으면 안 된다.
""",
    drill_doc="""
Drill.node(i)                  // 정점 i 의 깊이를 정했다
Drill.edge(i, parent)          // 부모로 올라갔다
""",
    constraints="""
- `1 <= parent.size <= 200_000`
- 정확히 하나의 `-1`, 나머지는 유효한 정점 번호, 순환 없음
""",
    signature=dict(name="treeHeight", parameters=[("parent", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(),
    reference=_tree_height,
    cases={
        "sample": [
            ("01", [[-1, 0, 0, 1, 1, 2]]),
            ("02", [[1, -1, 1]]),
        ],
        "boundary": [
            ("01-single", [[-1]]),
            # 루트가 마지막이다. 부모가 자식보다 뒤에 온다.
            ("02-root-last", [[1, 2, -1]]),
            # 사슬. 높이가 n-1 이다.
            ("03-chain", [[-1] + list(range(0, 9))]),
            # 별. 높이 1.
            ("04-star", [[-1] + [0] * 9]),
        ],
        "hidden": [
            ("01-random", [_random_parents(200, salt=931)]),
            ("02-reversed-chain", [[i + 1 for i in range(99)] + [-1]]),
        ],
        "performance": [
            ("01-small", [_random_parents(3000, salt=932)]),
            ("02-chain-medium", [[-1] + list(range(0, 19999))]),
            ("03-chain-large", [[-1] + list(range(0, 199999))]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 깊이를 메모하며 올라간다. 정점마다 한 번.
fun treeHeight(parent: IntArray): Int {
    val depth = IntArray(parent.size) { -1 }
    var best = 0
    for (start in parent.indices) {
        // 아직 모르는 조상까지 올라가며 경로를 쌓고, 내려오며 깊이를 채운다.
        val path = ArrayList<Int>()
        var v = start
        while (v != -1 && depth[v] == -1) {
            path.add(v)
            Drill.edge(v.toString(), parent[v].toString())
            v = parent[v]
        }
        var d = if (v == -1) -1 else depth[v]
        for (i in path.indices.reversed()) {
            d += 1
            depth[path[i]] = d
            Drill.node(path[i].toString())
        }
        if (depth[start] > best) best = depth[start]
    }
    return best
}
""",
    mutants=[
        ("counts-nodes--off-by-one", "OFF_BY_ONE",
         "간선 수가 아니라 정점 수를 센다. 높이가 늘 하나 크다.",
         """
fun treeHeight(parent: IntArray): Int {
    val depth = IntArray(parent.size) { -1 }
    var best = 0
    for (start in parent.indices) {
        val path = ArrayList<Int>()
        var v = start
        while (v != -1 && depth[v] == -1) { path.add(v); v = parent[v] }
        var d = if (v == -1) 0 else depth[v]
        for (i in path.indices.reversed()) { d += 1; depth[path[i]] = d }
        if (depth[start] > best) best = depth[start]
    }
    return best
}
"""),
        ("assumes-parent-first--single-pass", "MISSING_EDGE_CASE",
         "부모가 자식보다 먼저 온다고 믿고 한 번만 훑는다. 루트가 뒤에 있으면 틀린다.",
         """
fun treeHeight(parent: IntArray): Int {
    val depth = IntArray(parent.size)
    var best = 0
    for (i in parent.indices) {
        depth[i] = if (parent[i] == -1) 0 else depth[parent[i]] + 1
        if (depth[i] > best) best = depth[i]
    }
    return best
}
"""),
        ("no-memo--climbs-every-time", "PERFORMANCE",
         "정점마다 루트까지 다시 올라간다. 사슬에서 O(n²).",
         """
fun treeHeight(parent: IntArray): Int {
    var best = 0
    for (start in parent.indices) {
        var d = 0
        var v = start
        while (parent[v] != -1) { Drill.edge(v.toString(), parent[v].toString()); v = parent[v]; d += 1 }
        if (d > best) best = d
    }
    return best
}
"""),
    ],
))


# --- 2. 서브트리 합의 최대 ------------------------------------------------------

def _max_subtree_sum(parent, values):
    depth = _depths(parent)
    order = sorted(range(len(parent)), key=lambda i: -depth[i])
    total = list(values)
    for i in order:
        if parent[i] != -1:
            total[parent[i]] += total[i]
    return max(total)


PROBLEMS.append(Problem(
    id="max-subtree-sum",
    title="가장 큰 서브트리 합",
    summary="""
정점 `0..n-1` 의 트리가 부모 배열 `parent` 로, 정점의 값이 `values` 로 주어진다. 어떤
정점을 루트로 하는 서브트리(그 정점과 모든 자손)의 값 합 중 **최댓값**을 반환한다.

값은 음수일 수 있다. 잎 하나짜리 서브트리도 서브트리다.
""",
    notes="""
자식이 부모보다 먼저 처리돼야 한다. 깊은 정점부터 정리하거나, 자식 목록을 만들어 뒤에서
훑는다. 부모 배열의 순서는 보장이 없다.
""",
    drill_doc="""
Drill.node(i)                  // 정점 i 의 서브트리 합이 확정됐다
Drill.edge(i, parent)          // 부모에 합을 올려 보냈다
Drill.match(i, total)          // 최댓값이 갱신됐다
""",
    constraints="""
- `1 <= parent.size = values.size <= 200_000`
- `-10^4 <= values[i] <= 10^4`, 합은 `Int` 범위 안
""",
    signature=dict(name="maxSubtreeSum", parameters=[("parent", "INT_ARRAY"), ("values", "INT_ARRAY")],
                   returns="INT"),
    groups=perf_groups(),
    reference=_max_subtree_sum,
    cases={
        "sample": [
            ("01", [[-1, 0, 0, 1, 1], [1, -5, 3, 10, 2]]),
            ("02", [[-1, 0], [-3, -1]]),
        ],
        "boundary": [
            ("01-single", [[-1], [7]]),
            # 전부 음수. 가장 큰 잎 하나가 답이다.
            ("02-all-negative", [[-1, 0, 0], [-5, -2, -9]]),
            # 루트가 뒤에 있고, 그 서브트리 합이 답이다. 인덱스 역순으로 훑으면 루트를
            # 자식보다 먼저 정리해 합이 덜 올라간다.
            ("03-root-last", [[2, 2, -1], [4, 5, 1]]),
            # 깊은 사슬에서 중간 정점이 최대.
            ("04-chain", [[-1, 0, 1, 2, 3], [-10, 20, -1, -1, -1]]),
        ],
        "hidden": [
            ("01-random", [_random_parents(300, salt=941), randoms(300, -50, 50, salt=942)]),
            ("02-star-positive", [[-1] + [0] * 99, [1] * 100]),
        ],
        "performance": [
            ("01-small", [_random_parents(3000, salt=943), randoms(3000, -100, 100, salt=944)]),
            ("02-chain-medium", [[-1] + list(range(0, 19999)), randoms(20000, -100, 100, salt=945)]),
            ("03-chain-large", [[-1] + list(range(0, 199999)), randoms(200000, -100, 100, salt=946)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 자식 목록을 만들고 루트에서 DFS 순서를 뽑아 뒤에서 합친다.
fun maxSubtreeSum(parent: IntArray, values: IntArray): Int {
    val n = parent.size
    val head = IntArray(n) { -1 }
    val next = IntArray(n) { -1 }
    var root = 0
    for (i in 0 until n) {
        val p = parent[i]
        if (p == -1) { root = i } else { next[i] = head[p]; head[p] = i }
    }
    // 반복 DFS 로 방문 순서를 얻는다. 재귀는 20만 깊이의 사슬에서 스택이 넘친다.
    val order = IntArray(n)
    var size = 0
    val stack = ArrayDeque<Int>()
    stack.addLast(root)
    while (stack.isNotEmpty()) {
        val v = stack.removeLast()
        order[size++] = v
        var c = head[v]
        while (c != -1) { stack.addLast(c); c = next[c] }
    }
    val total = values.copyOf()
    var best = Int.MIN_VALUE
    for (k in n - 1 downTo 0) {
        val v = order[k]
        Drill.node(v.toString())
        if (total[v] > best) { best = total[v]; Drill.match(v, best) }
        if (parent[v] != -1) {
            total[parent[v]] += total[v]
            Drill.edge(v.toString(), parent[v].toString())
        }
    }
    return best
}
""",
    mutants=[
        ("positive-only--clamps-children", "WRONG_ALGORITHM",
         "음수인 자식 서브트리를 더하지 않는다. 그것은 '최대 경로'이지 서브트리 합이 아니다.",
         """
fun maxSubtreeSum(parent: IntArray, values: IntArray): Int {
    val n = parent.size
    val head = IntArray(n) { -1 }; val next = IntArray(n) { -1 }; var root = 0
    for (i in 0 until n) { val p = parent[i]; if (p == -1) root = i else { next[i] = head[p]; head[p] = i } }
    val order = IntArray(n); var size = 0
    val stack = ArrayDeque<Int>(); stack.addLast(root)
    while (stack.isNotEmpty()) { val v = stack.removeLast(); order[size++] = v; var c = head[v]; while (c != -1) { stack.addLast(c); c = next[c] } }
    val total = values.copyOf(); var best = Int.MIN_VALUE
    for (k in n - 1 downTo 0) {
        val v = order[k]
        if (total[v] > best) best = total[v]
        if (parent[v] != -1 && total[v] > 0) total[parent[v]] += total[v]
    }
    return best
}
"""),
        ("assumes-parent-first--reverse-index", "MISSING_EDGE_CASE",
         "인덱스 역순이 곧 자식→부모 순서라고 믿는다. 루트가 뒤에 있으면 합이 덜 올라간다.",
         """
fun maxSubtreeSum(parent: IntArray, values: IntArray): Int {
    val total = values.copyOf()
    var best = Int.MIN_VALUE
    for (v in parent.indices.reversed()) {
        if (total[v] > best) best = total[v]
        if (parent[v] != -1) total[parent[v]] += total[v]
    }
    return best
}
"""),
        ("zero-start--misses-all-negative", "MISSING_EDGE_CASE",
         "최댓값을 0 에서 시작한다. 전부 음수면 존재하지 않는 빈 서브트리를 답으로 낸다.",
         """
fun maxSubtreeSum(parent: IntArray, values: IntArray): Int {
    val n = parent.size
    val head = IntArray(n) { -1 }; val next = IntArray(n) { -1 }; var root = 0
    for (i in 0 until n) { val p = parent[i]; if (p == -1) root = i else { next[i] = head[p]; head[p] = i } }
    val order = IntArray(n); var size = 0
    val stack = ArrayDeque<Int>(); stack.addLast(root)
    while (stack.isNotEmpty()) { val v = stack.removeLast(); order[size++] = v; var c = head[v]; while (c != -1) { stack.addLast(c); c = next[c] } }
    val total = values.copyOf(); var best = 0
    for (k in n - 1 downTo 0) {
        val v = order[k]
        if (total[v] > best) best = total[v]
        if (parent[v] != -1) total[parent[v]] += total[v]
    }
    return best
}
"""),
        ("recomputes-per-node--quadratic", "PERFORMANCE",
         "정점마다 서브트리를 다시 훑어 합한다. 사슬에서 O(n²).",
         """
fun maxSubtreeSum(parent: IntArray, values: IntArray): Int {
    val n = parent.size
    var best = Int.MIN_VALUE
    for (root in 0 until n) {
        var sum = 0
        for (v in 0 until n) {
            var u = v
            while (u != -1 && u != root) { Drill.edge(u.toString(), parent[u].toString()); u = parent[u] }
            if (u == root) sum += values[v]
        }
        if (sum > best) best = sum
    }
    return best
}
"""),
    ],
))


# --- 3. 순환을 만드는 간선 ------------------------------------------------------

def _deep_chain_then_fan(n):
    half = n // 2
    chain = sum(([i + 1, i] for i in range(half - 1)), [])
    fan = sum(([0, half + j] for j in range(n - half)), [])
    return chain + fan + [1, 2]


def _redundant_edge(n, edges):
    parent = list(range(n))
    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x
    for i in range(0, len(edges), 2):
        a, b = find(edges[i]), find(edges[i + 1])
        if a == b:
            return [edges[i], edges[i + 1]]
        parent[a] = b
    return [-1, -1]


PROBLEMS.append(Problem(
    id="redundant-connection",
    title="순환을 만드는 간선",
    summary="""
정점 `0..n-1` 의 무방향 그래프에 간선이 `[a1, b1, a2, b2, ...]` 순서로 **하나씩 더해진다.**
처음으로 순환을 만드는 간선을 `[a, b]` 로 반환한다. 순환이 생기지 않으면 `[-1, -1]` 이다.

간선은 주어진 순서 그대로 더해진다 — 정렬하지 않는다.
""",
    notes="""
"두 정점이 이미 이어져 있는가"를 간선마다 묻는 문제다. 매번 탐색하면 간선당 O(n) 이고,
유니온파인드는 거의 O(1) 이다.
""",
    drill_doc="""
Drill.edge(a, b)               // 간선을 봤다
Drill.node(root)               // 대표를 찾았다
Drill.match(a, b)              // 순환을 만드는 간선을 찾았다
""",
    constraints="""
- `1 <= n <= 100_000`
- `0 <= edges.size <= 400_000`, 짝수
""",
    signature=dict(name="redundantEdge", parameters=[("n", "INT"), ("edges", "INT_ARRAY")],
                   returns="INT_ARRAY"),
    groups=perf_groups(),
    reference=_redundant_edge,
    cases={
        "sample": [
            ("01", [3, [0, 1, 1, 2, 2, 0]]),
            ("02", [4, [0, 1, 1, 2, 2, 3]]),
        ],
        "boundary": [
            ("01-no-edges", [3, []]),
            # 같은 간선이 두 번. 두 번째가 순환이다.
            ("02-duplicate-edge", [2, [0, 1, 0, 1]]),
            # 자기 자신을 잇는 간선은 곧 순환이다.
            ("03-self-loop", [2, [1, 1]]),
            # 순환이 둘이면 먼저 생긴 것.
            ("04-two-cycles", [6, [0, 1, 1, 2, 3, 4, 4, 5, 5, 3, 2, 0]]),
            # 뒤에 오는 간선이 먼저 온 것보다 번호가 작아도 순서를 지킨다.
            ("05-order-matters", [3, [2, 1, 1, 0, 0, 2]]),
        ],
        "hidden": [
            ("01-tree-then-cycle", [50, sum(([i, i + 1] for i in range(49)), []) + [49, 0]]),
            ("02-star", [40, sum(([0, i] for i in range(1, 40)), []) + [5, 7]]),
        ],
        "performance": [
            # 앞 절반으로 깊은 사슬을 만들고, 뒤 절반의 고립 정점을 사슬의 끝(0)에 하나씩
            # 잇는다. 경로 압축이 없으면 find(0) 이 매번 사슬을 통째로 걷는다.
            ("01-small", [3000, _deep_chain_then_fan(3000)]),
            ("02-medium", [30000, _deep_chain_then_fan(30000)]),
            ("03-large", [100000, _deep_chain_then_fan(100000)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 유니온파인드, 경로 압축.
fun redundantEdge(n: Int, edges: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int {
        var v = x
        while (parent[v] != v) { parent[v] = parent[parent[v]]; v = parent[v] }
        Drill.node(v.toString())
        return v
    }
    var i = 0
    while (i < edges.size) {
        val a = edges[i]; val b = edges[i + 1]
        Drill.edge(a.toString(), b.toString())
        val ra = find(a); val rb = find(b)
        if (ra == rb) { Drill.match(a, b); return intArrayOf(a, b) }
        parent[ra] = rb
        i += 2
    }
    return intArrayOf(-1, -1)
}
""",
    mutants=[
        ("last-cycle-edge--returns-final", "WRONG_BRANCH",
         "순환을 찾아도 멈추지 않고 마지막 것을 돌려준다. 순환이 둘이면 틀린다.",
         """
fun redundantEdge(n: Int, edges: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    var found = intArrayOf(-1, -1)
    var i = 0
    while (i < edges.size) {
        val a = edges[i]; val b = edges[i + 1]
        val ra = find(a); val rb = find(b)
        if (ra == rb) found = intArrayOf(a, b) else parent[ra] = rb
        i += 2
    }
    return found
}
"""),
        ("ignores-self-loop--skips-equal", "MISSING_EDGE_CASE",
         "양 끝이 같은 간선을 건너뛴다. 자기 자신을 잇는 간선도 순환이다.",
         """
fun redundantEdge(n: Int, edges: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    var i = 0
    while (i < edges.size) {
        val a = edges[i]; val b = edges[i + 1]
        if (a != b) {
            val ra = find(a); val rb = find(b)
            if (ra == rb) return intArrayOf(a, b)
            parent[ra] = rb
        }
        i += 2
    }
    return intArrayOf(-1, -1)
}
"""),
        ("sorted-edges--reorders-input", "WRONG_ALGORITHM",
         "간선을 정렬해 처리한다. 어느 간선이 '처음' 순환을 만드는지가 달라진다.",
         """
fun redundantEdge(n: Int, edges: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    val pairs = (0 until edges.size / 2).map { intArrayOf(edges[2 * it], edges[2 * it + 1]) }
        .sortedWith(compareBy({ it[0] }, { it[1] }))
    for (e in pairs) {
        val ra = find(e[0]); val rb = find(e[1])
        if (ra == rb) return intArrayOf(e[0], e[1])
        parent[ra] = rb
    }
    return intArrayOf(-1, -1)
}
"""),
        ("no-compression--long-chains", "PERFORMANCE",
         "경로 압축도 랭크도 없다. 사슬이 길어지면 찾기가 O(n).",
         """
fun redundantEdge(n: Int, edges: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) { Drill.edge(v.toString(), parent[v].toString()); v = parent[v] }; return v }
    var i = 0
    while (i < edges.size) {
        val a = edges[i]; val b = edges[i + 1]
        val ra = find(a); val rb = find(b)
        if (ra == rb) return intArrayOf(a, b)
        parent[rb] = ra
        i += 2
    }
    return intArrayOf(-1, -1)
}
"""),
    ],
))


# --- 4. 창의 최댓값 --------------------------------------------------------------

def _window_max(nums, k):
    from collections import deque
    out = []
    dq = deque()
    for i, v in enumerate(nums):
        while dq and nums[dq[-1]] <= v:
            dq.pop()
        dq.append(i)
        if dq[0] <= i - k:
            dq.popleft()
        if i >= k - 1:
            out.append(nums[dq[0]])
    return out


PROBLEMS.append(Problem(
    id="sliding-window-max",
    title="창마다 최댓값",
    summary="""
정수 배열 `nums` 와 창 크기 `k` 가 주어진다. 왼쪽부터 한 칸씩 미는 크기 `k` 의 창마다
그 안의 **최댓값**을 담아 반환한다. 결과의 길이는 `nums.size - k + 1` 이다.
""",
    notes="""
창을 밀 때 "앞으로 최댓값이 될 가능성이 없는 원소"는 버려도 된다 — 자기보다 크고 더
나중에 들어온 원소가 있으면 그렇다. 남는 후보는 내림차순이고, 그것을 양끝에서 다루는
구조가 덱이다.
""",
    drill_doc="""
Drill.enqueue(value)           // 후보로 넣었다
Drill.dequeue(value)           // 창을 벗어나 버렸다
Drill.pop(value)               // 더 큰 값에 밀려 버렸다
Drill.write(i, max)            // i 번째 창의 답을 적었다
""",
    constraints="""
- `1 <= k <= nums.size <= 200_000`
- `-10^9 <= nums[i] <= 10^9`
""",
    signature=dict(name="windowMax", parameters=[("nums", "INT_ARRAY"), ("k", "INT")], returns="INT_ARRAY"),
    # 결과가 입력만큼 길다. 기본 한도(64KB)로는 정답 풀이가 OUTPUT_LIMIT 으로 떨어진다.
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 2000000},
    # 입력을 더 키우면 출력 한도에 걸린다. 성능 그룹의 시간만 조여 여유를 확보한다.
    groups=perf_groups(time_multiplier=0.5),
    reference=_window_max,
    cases={
        "sample": [
            ("01", [[1, 3, -1, -3, 5, 3, 6, 7], 3]),
            ("02", [[9, 8, 7], 1]),
        ],
        "boundary": [
            # k 가 전체. 창 하나.
            ("01-whole", [[4, 2, 9, 1], 4]),
            # 내림차순. 앞 원소가 계속 최댓값이라 창을 벗어나는 순간이 중요하다.
            ("02-descending", [[5, 4, 3, 2, 1], 2]),
            # 같은 값이 이어진다. <= 와 < 의 차이가 여기서 난다.
            ("03-plateau", [[2, 2, 2, 2], 2]),
            # 음수만.
            ("04-negative", [[-5, -1, -3, -2], 3]),
        ],
        "hidden": [
            ("01-random", [randoms(500, -1000, 1000, salt=951), 7]),
            ("02-sawtooth", [[(i % 5) for i in range(200)], 4]),
        ],
        "performance": [
            ("01-small", [randoms(4000, -100000, 100000, salt=952), 500]),
            ("02-medium", [randoms(40000, -100000, 100000, salt=953), 5000]),
            ("03-large", [list(range(200000, 0, -1)), 100000]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 인덱스의 단조 감소 덱.
fun windowMax(nums: IntArray, k: Int): IntArray {
    val out = IntArray(nums.size - k + 1)
    val dq = ArrayDeque<Int>()
    for (i in nums.indices) {
        while (dq.isNotEmpty() && nums[dq.last()] <= nums[i]) {
            Drill.pop(nums[dq.removeLast()])
        }
        dq.addLast(i)
        Drill.enqueue(nums[i])
        if (dq.first() <= i - k) {
            Drill.dequeue(nums[dq.removeFirst()])
        }
        if (i >= k - 1) {
            out[i - k + 1] = nums[dq.first()]
            Drill.write(i - k + 1, out[i - k + 1])
        }
    }
    return out
}
""",
    mutants=[
        ("strict-pop--keeps-equal", "WRONG_BRANCH",
         "같은 값을 버리지 않는다. 답은 같지만 덱이 커져 성능 그룹에서 드러나거나, 창을 벗어난 같은 값이 남아 틀린다.",
         """
fun windowMax(nums: IntArray, k: Int): IntArray {
    val out = IntArray(nums.size - k + 1)
    val dq = ArrayDeque<Int>()
    for (i in nums.indices) {
        while (dq.isNotEmpty() && nums[dq.last()] < nums[i]) dq.removeLast()
        dq.addLast(i)
        if (dq.first() < i - k) dq.removeFirst()
        if (i >= k - 1) out[i - k + 1] = nums[dq.first()]
    }
    return out
}
"""),
        ("late-expire--off-by-one", "OFF_BY_ONE",
         "창을 벗어난 원소를 한 칸 늦게 버린다. 앞쪽 큰 값이 한 창 더 살아남는다.",
         """
fun windowMax(nums: IntArray, k: Int): IntArray {
    val out = IntArray(nums.size - k + 1)
    val dq = ArrayDeque<Int>()
    for (i in nums.indices) {
        while (dq.isNotEmpty() && nums[dq.last()] <= nums[i]) dq.removeLast()
        dq.addLast(i)
        if (dq.first() < i - k) dq.removeFirst()
        if (i >= k - 1) out[i - k + 1] = nums[dq.first()]
    }
    return out
}
"""),
        ("slices-window--copies-then-max", "PERFORMANCE",
         "창마다 부분 배열을 복사해 최댓값을 구한다. O(n·k) 에 할당까지 — 가장 흔한 첫 풀이다.",
         """
fun windowMax(nums: IntArray, k: Int): IntArray {
    val out = IntArray(nums.size - k + 1)
    for (start in out.indices) {
        val window = nums.copyOfRange(start, start + k)
        for (i in window.indices) Drill.visit(start + i, window[i])
        out[start] = window.max()
    }
    return out
}
"""),
    ],
))


# --- 5. 구간 합 질의 ------------------------------------------------------------

def _range_sums(nums, queries):
    prefix = [0]
    for v in nums:
        prefix.append(prefix[-1] + v)
    return [prefix[queries[i + 1] + 1] - prefix[queries[i]] for i in range(0, len(queries), 2)]


PROBLEMS.append(Problem(
    id="range-sum-queries",
    title="구간 합 질의",
    summary="""
정수 배열 `nums` 와 질의 `[l1, r1, l2, r2, ...]` 가 주어진다. 각 질의에 대해 `nums[l..r]`
(양 끝 포함)의 합을 담아 반환한다.
""",
    notes="""
질의마다 더하면 O(n) 씩이다. 누적합을 한 번 만들어 두면 질의는 뺄셈 한 번이다.
`prefix[r + 1] - prefix[l]` — 인덱스가 하나 어긋나기 쉬운 자리다.
""",
    drill_doc="""
Drill.write(i, prefix)         // 누적합을 채웠다
Drill.compare(l, r)            // 질의 구간을 봤다
""",
    constraints="""
- `1 <= nums.size <= 200_000`, `-10^4 <= nums[i] <= 10^4`
- `2 <= queries.size <= 400_000`, 짝수, `0 <= l <= r < nums.size`
""",
    signature=dict(name="rangeSums", parameters=[("nums", "INT_ARRAY"), ("queries", "INT_ARRAY")],
                   returns="INT_ARRAY"),
    # 질의 수만큼 출력한다.
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 2000000},
    groups=perf_groups(),
    reference=_range_sums,
    cases={
        "sample": [
            ("01", [[1, 2, 3, 4, 5], [0, 2, 1, 3, 4, 4]]),
            ("02", [[-1, 4, -2], [0, 2]]),
        ],
        "boundary": [
            ("01-single-element", [[7], [0, 0]]),
            # 전체 구간과 한 원소 구간.
            ("02-whole-and-point", [[3, -3, 3, -3], [0, 3, 3, 3]]),
            # 같은 질의가 반복된다.
            ("03-repeated", [[1, 1, 1], [1, 2, 1, 2, 1, 2]]),
            ("04-negative-sum", [[-5, -5, 10], [0, 1, 0, 2]]),
        ],
        "hidden": [
            ("01-random", [randoms(300, -100, 100, salt=961),
                           sum(([min(a, b), max(a, b)] for a, b in zip(randoms(200, 0, 299, salt=962), randoms(200, 0, 299, salt=963))), [])]),
        ],
        "performance": [
            ("01-small", [randoms(3000, -100, 100, salt=964), sum(([0, 2999] for _ in range(3000)), [])]),
            ("02-medium", [randoms(30000, -100, 100, salt=965), sum(([0, 29999] for _ in range(30000)), [])]),
            ("03-large", [randoms(200000, -100, 100, salt=966), sum(([0, 199999] for _ in range(100000)), [])]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 누적합.
fun rangeSums(nums: IntArray, queries: IntArray): IntArray {
    val prefix = IntArray(nums.size + 1)
    for (i in nums.indices) {
        prefix[i + 1] = prefix[i] + nums[i]
        Drill.write(i + 1, prefix[i + 1])
    }
    val out = IntArray(queries.size / 2)
    for (q in out.indices) {
        val l = queries[2 * q]; val r = queries[2 * q + 1]
        Drill.compare(l, r)
        out[q] = prefix[r + 1] - prefix[l]
    }
    return out
}
""",
    mutants=[
        ("exclusive-right--drops-last", "OFF_BY_ONE",
         "오른쪽 끝을 빼고 더한다. prefix[r] - prefix[l] 이다.",
         """
fun rangeSums(nums: IntArray, queries: IntArray): IntArray {
    val prefix = IntArray(nums.size + 1)
    for (i in nums.indices) prefix[i + 1] = prefix[i] + nums[i]
    val out = IntArray(queries.size / 2)
    for (q in out.indices) out[q] = prefix[queries[2 * q + 1]] - prefix[queries[2 * q]]
    return out
}
"""),
        ("inclusive-left--adds-extra", "OFF_BY_ONE",
         "왼쪽 끝 앞의 원소까지 포함한다. prefix[r+1] - prefix[l-1] 을 l=0 에서도 쓴다.",
         """
fun rangeSums(nums: IntArray, queries: IntArray): IntArray {
    val prefix = IntArray(nums.size + 2)
    for (i in nums.indices) prefix[i + 2] = prefix[i + 1] + nums[i]
    val out = IntArray(queries.size / 2)
    for (q in out.indices) out[q] = prefix[queries[2 * q + 1] + 2] - prefix[queries[2 * q]]
    return out
}
"""),
        ("sums-each-query--linear-per-query", "PERFORMANCE",
         "질의마다 구간을 다시 더한다. O(n·q).",
         """
fun rangeSums(nums: IntArray, queries: IntArray): IntArray {
    val out = IntArray(queries.size / 2)
    for (q in out.indices) {
        var s = 0
        for (i in queries[2 * q]..queries[2 * q + 1]) { Drill.visit(i, nums[i]); s += nums[i] }
        out[q] = s
    }
    return out
}
"""),
    ],
))
